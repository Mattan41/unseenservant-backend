# Architecture — unseenservant-backend

## Tech stack
- Java 25
- Spring Boot
- Spring Security (JWT + OAuth2)
- Spring Data JPA
- MySQL
- Testcontainers (integration tests)

## Concurrency

The application runs on **virtual threads** (`spring.threads.virtual.enabled=true`),
so Tomcat creates one virtual thread per request. Its `VirtualThreadExecutor` is
unbounded, which means `server.tomcat.threads.max` **no longer caps concurrency**.
The ceilings that matter are:

| Ceiling | Setting | Why |
|---|---|---|
| Open sockets | `server.tomcat.max-connections=500` | Blast-radius bound for a single-box deployment; Tomcat's default of 8192 would let a flood open far more sockets than the host should carry. Revisit if a load balancer is introduced. |
| Database connections | `spring.datasource.hikari.maximum-pool-size=15` | See below. |
| CPU | 4 cores on the deployment host | Virtual threads add concurrency, not CPU. |

`spring.datasource.hikari.connection-timeout=10000` makes a saturated pool fail
in 10 seconds instead of the 30-second default.

Pool sizing is a judgement call, not a tuned value. A request holds its
connection for its whole transaction (several sequential queries). Image uploads
deliberately do *not* hold one during the disk write: the file is written first
and the database work runs in a short transaction afterwards
(`StoredFileTransaction`), which deletes the file again if that transaction fails.
15 is deliberately generous for the ~7-user scale, but well below the point where
a 4-core InnoDB starts losing throughput to context switching. **The pool is not
the bottleneck** — MySQL's `innodb_buffer_pool_size` matters far more and should
be reviewed before any further pool tuning.

Because virtual threads remove the thread ceiling, a burst now competes for CPU
instead of for threads. CPU-bound work such as Jackson serialization of
`Spell.rawJsonData` is what to watch if the app ever feels slow.

`VirtualThreadsIT` asserts the property, the connector's executor type, and that
a request is actually served on a virtual thread.

## Login

`/api/auth/login` is the one unauthenticated endpoint that costs real work: a
database lookup plus a BCrypt comparison - tens of milliseconds of CPU - for every
attempt, on a box sized for a handful of users. Two things follow from that.

**The comparison runs whether or not the account exists.** An unknown username is
hashed against a random value computed at startup (`unknownUserHash`), so the two
outcomes cost the same and the response cannot be used to find out which usernames
are real. Both answer 401 - `BadCredentialsException` is handled by
`GlobalExceptionHandler`, because the controller throws it rather than the filter
chain, which has its own entry point.

**`ratelimit/LoginAttemptLimiter` brakes it on two axes**, the client address and
the username, because a username sits in a POST body and neither Cloudflare nor
nginx can count it. It is called at the top of `AuthController.login`, before the
lookup, so a throttled attempt never spends the hash it is protecting; over budget
it throws `RateLimitExceededException`, answered as 429 with `Retry-After`.

Counting is a bounded in-memory map of fixed windows, configured under
`app.rate-limit.*` in `application.properties`: per-instance, no shared store, no
extra dependency. Two backend instances would each allow the full budget - that is
the deliberate trade at this scale, and it is why the per-address `limit_req` in
the frontend's nginx stays underneath it. The size ceiling is what stops a flood
of distinct keys turning that map into a leak: while every tracked key is still
inside its window, the limiter fails closed instead of growing. Addresses come
from `ratelimit/ClientIpResolver`, which documents its own trust chain - behind
the tunnel that is `CF-Connecting-IP` - and the frontend's `docs/DEPLOYMENT.md`
("Rate limiting") carries the reasoning for the whole stack.

The throttle is off in the `test` profile (`app.rate-limit.enabled=false`), so a
test that signs in repeatedly is not answered with a 429 by a counter it never
asked about; `LoginRateLimitIT` turns it on for the behaviour it is about.

## Database migrations

Flyway owns the schema. Migrations are discovered from the single location
`classpath:db/migration`, split by file type between
`src/main/resources/db/migration` (SQL) and `src/main/java/db/migration` (Java
migrations). See [`MIGRATIONS.md`](./MIGRATIONS.md) for why some are Java, the
environment matrix, the full version sequence, and the rules for adding new ones.

## Package structure

```
org.kruskopf.backend/
├── BackendApplication.java
├── StartupRunner.java
├── auth/
│   ├── JwtAuthenticationFilter.java
│   ├── JwtService.java
│   ├── controller/
│   │   └── AuthController.java
│   └── dto/
│       └── AuthDTO.java
├── campaign/
│   ├── controller/CampaignController.java
│   ├── dto/
│   │   ├── CampaignCreationDTO.java
│   │   ├── CampaignResponseDTO.java
│   │   ├── CampaignTransferOwnerDTO.java
│   │   ├── CampaignUpdateDTO.java
│   │   ├── ParticipantResponseDTO.java
│   │   └── UpdateParticipantsDTO.java
│   ├── entity/
│   │   ├── Campaign.java
│   │   ├── CampaignRole.java       # Enum
│   │   ├── CampaignUser.java       # Join entity (Campaign ↔ User)
│   │   └── CampaignUserId.java     # Composite key
│   ├── repository/
│   │   ├── CampaignRepository.java
│   │   └── CampaignUserRepository.java
│   └── service/
│       ├── CampaignPermissionService.java 
│       └── CampaignService.java
├── playercharacter/                     # System-agnostic character feature
│   ├── controller/GameCharacterController.java
│   ├── dto/
│   │   ├── CharacterCampaignUpdateDTO.java
│   │   ├── CharacterInputDTO.java
│   │   └── CharacterOutputDTO.java
│   ├── entity/
│   │   ├── GameCharacter.java           # Generic core character
│   │   └── GameSystem.java              # System enum (DND5E, OFFWORLDERS)
│   ├── repository/GameCharacterRepository.java
│   ├── service/GameCharacterService.java
│   └── GameCharacterMapper.java
├── dnd5e/                               # D&D 5e-specific character data
│   ├── entity/Dnd5eCharacterData.java           # 1:1 with GameCharacter (shared PK)
│   ├── repository/Dnd5eCharacterDataRepository.java
│   ├── dto/
│   │   ├── Dnd5eCharacterDataInputDTO.java
│   │   └── Dnd5eCharacterDataOutputDTO.java
│   ├── Dnd5eCharacterStats.java         # Stats value object
│   └── Dnd5eCharacterStatsConverter.java  # JPA AttributeConverter
├── offworlders/                          # Offworlders-specific character data
│   ├── entity/OffworldersCharacterData.java      # 1:1 with GameCharacter (shared PK)
│   ├── repository/OffworldersCharacterDataRepository.java
│   ├── dto/
│   │   ├── OffworldersCharacterDataInputDTO.java
│   │   └── OffworldersCharacterDataOutputDTO.java
│   ├── OffworldersStats.java             # Attribute value object (strength/agility/intelligence/willpower)
│   ├── OffworldersStatsConverter.java    # JPA AttributeConverter
│   ├── OffworldersWeapon.java            # Typed weapon value object (type + description)
│   ├── OffworldersWeaponsConverter.java  # JPA AttributeConverter: List<OffworldersWeapon> ↔ JSON array
│   ├── OffworldersItem.java              # Free-text inventory item value object (name + description)
│   ├── OffworldersItemsConverter.java    # JPA AttributeConverter: List<OffworldersItem> ↔ JSON array
│   ├── OffworldersEntry.java             # Skill/ability entry value object (name + description)
│   └── OffworldersEntriesConverter.java  # JPA AttributeConverter: List<OffworldersEntry> ↔ JSON array
├── spell/
│   ├── controller/
│   │   ├── CharacterSpellController.java
│   │   └── SpellSearchController.java
│   ├── dto/SpellSaveInputDTO.java
│   ├── dto/CharacterSpellResponseDTO.java
│   ├── entity/Spell.java
│   ├── repository/SpellRepository.java
│   └── service/SpellService.java
├── message/
│   ├── controller/MessageController.java
│   ├── dto/MessageDTO.java
│   ├── entity/Message.java
│   ├── repository/MessageRepository.java
│   └── service/MessageService.java
├── user/
│   ├── controller/UserController.java
│   ├── dto/UserDTO.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── UserRole.java       # Enum
│   │   └── ProviderType.java   # Enum (LOCAL, GOOGLE, GITHUB)
│   ├── repository/UserRepository.java
│   ├── service/UserService.java
│   ├── CustomUserDetails.java
│   └── CustomUserDetailsService.java
├── admin/                                # Administrator surface (ROLE_ADMIN only)
│   ├── controller/AdminController.java   # /api/admin/** — accounts + email whitelist
│   ├── dto/
│   │   ├── WhitelistEntryDTO.java
│   │   ├── WhitelistEntryRequest.java
│   │   ├── WhitelistUpdateRequest.java
│   │   └── RoleUpdateRequest.java
│   └── service/AdminService.java         # Role changes + whitelist CRUD, with the guards
├── whitelist/
│   ├── EmailWhitelist.java
│   ├── EmailWhitelistRepository.java
│   └── EmailWhitelistService.java
├── component/
│   └── CustomOAuth2SuccessHandler.java
├── config/
│   ├── AuditorAwareImpl.java
│   ├── Email.java
│   ├── JpaConfig.java
│   ├── SecurityConfig.java
│   └── WhitelistLoader.java
├── ratelimit/                            # Login throttle
│   ├── LoginAttemptLimiter.java          # Fixed-window counters, per address + per username
│   ├── RateLimitProperties.java          # app.rate-limit.*
│   └── ClientIpResolver.java             # CF-Connecting-IP, else the last X-Forwarded-For entry
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── UnauthorizedAccessException.java
│   ├── RateLimitExceededException.java   # 429 + Retry-After
│   └── UniqueConstraintViolationException.java
└── filestorage/
    ├── FileStorageService.java
    └── WebConfig.java
```

## Conventions

### Feature packaging
Each domain is a self-contained package with:
- `entity/` — JPA entities
- `repository/` — Spring Data JPA repositories (interface only)
- `service/` — business logic
- `controller/` — REST endpoints
- `dto/` — request/response objects

### Naming
- Controllers: `@RestController`, `@RequestMapping("/api/feature")`
- Services: `@Service`, injected via constructor
- Repositories: extend `JpaRepository<Entity, Id>`
- DTOs: suffix `InputDTO` (request), `OutputDTO`/`ResponseDTO` (response)

### Security
- JWT filter validates token on every request (`JwtAuthenticationFilter`)
- OAuth2 success handler issues JWT after Google/GitHub login (`CustomOAuth2SuccessHandler`)
- OAuth logins are matched by provider identity (`providerType` + `providerId`), then by email (case-insensitive) so an existing account is reused rather than duplicated
- Google requires a verified email; a rejected login (unverified or non-whitelisted email) redirects to `{FRONTEND_URL}/under-construction`
- Email whitelist controls who can register (`EmailWhitelistService`); it is the **login gate**, and its `role` only decides what a *first* login is granted — an existing account keeps its own role
- All `/api/**` endpoints require authentication unless explicitly permitted (`SecurityConfig` matchers)
- `/images/**` is served **without** a token on purpose: an `<img>` request cannot
  carry the JWT, so uploads are public at unguessable (UUID-bearing) paths
- `/error` is permitted as well. A permitted path with nothing behind it — a
  missing upload, an unknown URL — or a failing controller makes the container
  re-dispatch to `/error`, and the filter chain runs again on that dispatch.
  Without the matcher the second pass answers 401 and the real status is masked:
  a missing image looks like a rejected token, and the browser falls back to its
  placeholder instead of showing that the file is gone
- **Authorisation is two-layered:** role-shaped rules use `@PreAuthorize`
  (`hasRole('ROLE_USER')` / `hasRole('ROLE_ADMIN')`, e.g. `AdminController`,
  `UserController`, `GameCharacterController`, the spell controllers), while
  **resource-shaped** rules ("is this user the campaign's owner / GM / a
  participant?") live in the services behind `CampaignPermissionService` and
  raise `UnauthorizedAccessException` (403). SpEL cannot express those without a
  second database read, so they deliberately stay in the service layer.

### Error handling
- `GlobalExceptionHandler` handles all exceptions via `@RestControllerAdvice`
- Custom exceptions: `ResourceNotFoundException` (404), `UnauthorizedAccessException` (403), `InvalidRequestException` (400 — a well-formed request the domain refuses, e.g. self-demotion), `UniqueConstraintViolationException` (409), `ConcurrentUpdateException` (409), `RateLimitExceededException` (429 with `Retry-After` — see *Login throttle*)

### External HTTP calls
When backend needs to call external APIs (e.g. Open5e for lazy-loading content), use Spring's `RestClient` or `WebClient` — not `RestTemplate`.

## Existing entities (summary)

| Entity | Key fields | Relations |
|--------|-----------|-----------|
| `User` | id, email, role, providerType | has many GameCharacters, CampaignUsers |
| `GameCharacter` | id, name, systemType (nullable), notes, avatarUrl, appearance, backstory, privateBackstory | generic core character; belongs to User, optionally linked to Campaign; 1:1 with Dnd5eCharacterData when `systemType = DND5E`, 1:1 with OffworldersCharacterData when `systemType = OFFWORLDERS`, and neither when `systemType is null` (a character may be created without a game system — `system_type` is nullable and CHECK-constrained to `NULL` or a registered system since V17; extend `chk_character_system_type` when `GameSystem` gains a constant). `appearance`/`backstory` are public to the campaign; `privateBackstory` is only returned to the owner or the campaign GM |
| `Dnd5eCharacterData` | characterId (shared PK), level, characterClass, race, hitPoints, armorClass, stats (JSON) | D&D 5e data; 1:1 with GameCharacter; many-to-many with Spell via `character_spell` |
| `OffworldersCharacterData` | characterId (shared PK), characterClass, species, xp, health, currentHealth, healthModifier, armor, supply, supplyMax, credits, stats (JSON), skills (JSON), abilities (JSON), weapons (JSON), items (JSON) | Offworlders data; 1:1 with GameCharacter. `skills`/`abilities` are JSON arrays of `{ name, description }` entries; `weapons` is a typed array `{ type, description }`; `items` is a free-text array `{ name, description }` (appearance moved to the generic GameCharacter in V10) |
| `Campaign` | id, name, imageUrl | has many CampaignUsers |
| `CampaignUser` | campaignId + userId (composite PK), role | join table Campaign ↔ User |
| `Ship` | id, campaignId (unique), version, name, hull, hullMax, armor, damage, driveFuel, maxDriveFuel, credits, upgrades (JSON), notes, imageUrl, imageUrls (JSON) | Offworlders ship; 1:1 with Campaign; optimistic concurrency via JPA `@Version` on `version`, which Hibernate enforces with `UPDATE ... WHERE version = ?` (a stale flush becomes a 409 via `ObjectOptimisticLockingFailureException`). `ShipService` additionally compares the version token the client sent against the freshly loaded row and throws `ConcurrentUpdateException` (also 409) before doing any work — that check alone cannot see a writer that commits between the load and the flush |
| `ShipCrewMember` | id, shipId + userId (unique), notes, imageUrls (JSON), characters (JSON) | one crew entry per participant; keyed on the **User** (not CampaignUser) so it survives a participant leaving and re-activates on rejoin; `characters` is `[ { name, roleOnShip } ]` (free text) |
| `Message` | id, content, timestamp | belongs to Campaign |
| `Spell` | slug (PK), name, rawJsonData (TEXT) | many-to-many with Dnd5eCharacterData; stored in local DB from bulk Open5e import |
| `EmailWhitelist` | id, email, role, active | standalone; **login gate** for OAuth — a revoked row is soft-deleted (`active = false`) so the decision can be revisited, and the address column is unique, so re-admitting reactivates that row. `role` applies to a *first* login only |

## Tests
- Integration tests use Testcontainers (`AbstractIntegrationTest`)
- `TestContainersConfiguration` spins up MySQL container
- Current coverage: auth, security config, basic smoke test