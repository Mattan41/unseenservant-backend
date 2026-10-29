# Architecture — unseenservant-backend

## Tech stack
- Java 25
- Spring Boot 4.1 (Spring Framework 7, Spring Security 7)
- Spring Security (JWT + OAuth2)
- Spring Data JPA · Hibernate 7
- JSON through Jackson 3 (`tools.jackson`), both in the Boot-managed
  `ObjectMapper` bean and in the JPA attribute converters
- MySQL · Flyway, reached through `spring-boot-starter-flyway` (Boot 4 moved the
  auto-configuration into a module of its own)
- Testcontainers 2 (integration tests)

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

## Audit trail

The application records **user- and administration-related events only** — an
account created, a login (succeeded, failed, throttled), an OAuth refusal, a role
change, a whitelist change — in `audit_event`, and reads them back under
Settings → Logs (`GET /api/admin/logs`, ROLE_ADMIN). Campaigns, messages,
characters, ships, journals and the planner are deliberately outside it: the trail
answers "who has an account here, and who changed what about it", not "what did
people do in their campaigns". The whole scope is the `AuditEventType` enum, and
every one of its values maps to exactly one call site.

### Two write policies, and why

`AuditEventStore` is the only writer, and it is a bean of its own because the
distinction below is a Spring transaction annotation — a call from one method of a
bean to another of the same bean does not go through the proxy, so an annotation
on the callee would be silently ignored:

| | `write` — joins the caller's transaction | `writeDetached` — `REQUIRES_NEW` |
|---|---|---|
| For | a change: a role edit, a whitelist edit, an account creation | an attempt or refusal: a login, a throttle, an OAuth refusal, a refused demotion |
| If the insert fails | the change rolls back with it; the caller gets a 500 | a `WARN` on the `audit` logger, and the request continues |
| Because | an unaudited privilege change is worse than a refused one | a full disk must not turn a valid login into a 500, and a refusal row has to outlive the rollback its own throw causes |

That last cell is the subtle one: `AdminService.changeUserRole` is
`@Transactional` and *throws* after recording a refused demotion, so a row written
inside that transaction would vanish with it — and the refusal is precisely what
the trail is for. `REQUIRES_NEW` is what makes it survive, and `AuditTrailIT`
proves it against a real database rather than asserting it. Suspending the
caller's transaction holds a second pooled connection for the length of one
insert: a deliberate trade at this scale (see "Concurrency"), and a reason the
Hikari pool is not the place to start tuning.

`AuditService` owns which policy applies, one named method per event, so a call
site reads as the sentence it records. Every event is *also* written to a logger
named `audit` as one `key=value` line. The redundancy is intentional: the table is
the readable trail, and the line is the half that survives the database being
unavailable and is greppable in `docker logs`. It can be routed to a file or a
collector later without touching the code.

### What the rows deliberately do not have

- **No foreign keys.** Every other table here has them; this one does not, because
  a trail that disappears when the account it names is deleted is not a trail.
  `actor_name` / `target_name` are copies taken at write time for the same reason,
  so an id in a row may name an account that no longer exists.
- **No update and no delete path.** No endpoint reaches a row except by reading
  it, and the repository exposes no mutator. The single delete is the prune below.
- **No secrets.** No password, no hash, no token. The failed-login row names the
  username that was attempted and never whether the account exists — the same
  property the 401 it accompanies has, so the trail cannot be used to enumerate
  accounts either.

### Retention

The rows are personal data — a username, an address, where somebody signed in from
— so they are pruned to `app.audit.retention-days` (default 365; `0` or less keeps
everything instead of deleting it, so a mis-set variable cannot wipe the trail).
`AuditRetentionRunner` does that once at startup, the same shape as
`WhitelistLoader` and needing no scheduler: at this scale the table gains a handful
of rows a day, so "daily, at boot" and "daily, at 03:00" differ by nothing an
operator can observe. **The trail is therefore not complete beyond the retention
period** — that is what a retention period is for.

## Database migrations

Flyway owns the schema, discovered from the single location
`classpath:db/migration`. That folder holds one SQL baseline
(`V1__baseline_schema.sql`) plus any migrations added after it: the former `V1`–`V17`
chain — including the Java migrations that used to live under
`src/main/java/db/migration` — is squashed into the baseline. See
[`MIGRATIONS.md`](./MIGRATIONS.md) for how the squash was made, how an existing
database is baselined rather than migrated, the environment matrix, and the rules
for adding new migrations.

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
│   ├── service/
│   │   ├── CampaignPermissionService.java 
│   │   ├── CampaignParticipantService.java # The roster: add/remove, rename, role, ownership
│   │   └── CampaignService.java
│   ├── CampaignMapper.java                 # Campaign → CampaignResponseDTO (the GM-notes rule)
│   └── CampaignParticipantFactory.java     # The role / nickname fallbacks a join row is built from
├── journal/                              # Private per-participant campaign journals
│   ├── controller/JournalController.java
│   ├── dto/
│   │   ├── JournalDTO.java
│   │   ├── JournalPageDTO.java
│   │   ├── JournalPageCreateDTO.java
│   │   ├── JournalPageUpdateDTO.java
│   │   └── JournalPageSaveDTO.java
│   ├── entity/
│   │   ├── Journal.java                 # One book per (campaign, user)
│   │   └── JournalPage.java             # A numbered page; @Version optimistic lock
│   ├── repository/
│   │   ├── JournalRepository.java
│   │   └── JournalPageRepository.java
│   ├── JournalMapper.java
│   └── service/JournalService.java
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
│   ├── controller/AdminController.java   # /api/admin/** — accounts, email whitelist, audit log
│   ├── dto/
│   │   ├── WhitelistEntryDTO.java
│   │   ├── WhitelistEntryRequest.java
│   │   ├── WhitelistUpdateRequest.java
│   │   └── RoleUpdateRequest.java
│   └── service/AdminService.java         # Role changes + whitelist CRUD, with the guards
├── audit/                                # The audit trail (user + administration events)
│   ├── AuditService.java                 # Named per-event methods; the two write policies
│   ├── AuditEventStore.java              # The one writer: joins, or REQUIRES_NEW
│   ├── AuditRetentionRunner.java         # The prune at startup (app.audit.retention-days)
│   ├── entity/
│   │   ├── AuditEvent.java               # Append-only; no FK to users
│   │   └── AuditEventType.java           # Enum — the wire contract the Logs view filters on
│   ├── repository/AuditEventRepository.java
│   └── dto/AuditEventDTO.java
├── whitelist/
│   ├── EmailWhitelist.java
│   ├── EmailWhitelistRepository.java
│   └── EmailWhitelistService.java
├── entity/                               # Shared JPA base classes; no feature of their own
│   ├── BaseEntity.java                   # @MappedSuperclass: id + createdAt / updatedAt
│   └── AuditedEntity.java                # extends BaseEntity: + createdBy / lastModifiedBy
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

Not everything lives in a feature package. The JPA base classes the entities share live in the
root `entity/` — `BaseEntity` (generated `id`, `createdAt` / `updatedAt`) and its `AuditedEntity`
subclass (the two `by` usernames), see "Existing entities" below — and code every feature uses
lives in `config/`, `exception/` and `filestorage/`.

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
- **One body for every failure:** an RFC 9457 `ProblemDetail` (`type`, `title`,
  `status`, `detail`, `instance`), served as `application/problem+json`. The client
  reads `detail`; `field` and `value` appear as extension properties when the caller
  can act on them. See docs/INTEGRATION_CONTRACT.md, "Error Response Format".
- `GlobalExceptionHandler` is the single place an MVC failure becomes a body. It
  extends `ResponseEntityExceptionHandler`, so the framework's own failures —
  malformed request body, wrong parameter type, unsupported media type, unknown
  path, upload past the multipart limit — answer in the same shape. A catch-all
  returns a generic 500 and logs the cause; nothing builds an error response
  anywhere else in the MVC layer.
- Custom exceptions: `ResourceNotFoundException` (404),
  `UnauthorizedAccessException` (403), `InvalidFileFormatException` (400 — empty or
  wrong kind of file), `FileUploadTooLargeException` (413 — past
  `app.upload.max-file-size`; the same status the container gives past its own
  multipart limit), `InvalidRequestException` (400 — a well-formed request the
  domain refuses, e.g. self-demotion), `UniqueConstraintViolationException` (409),
  `ConcurrentUpdateException` (409), `RateLimitExceededException` (429 with
  `Retry-After` — see *Login throttle*).
- The security filter chain runs outside the dispatcher and cannot reach the
  advice: `SecurityConfig` writes its 401 and 403 in the same shape, so a client
  needs one parser for every status.

### External HTTP calls
When backend needs to call external APIs (e.g. Open5e for lazy-loading content), use Spring's `RestClient` or `WebClient` — not `RestTemplate`.

## Existing entities (summary)

The generated `id` and the audit columns are **not** repeated in each entity. `entity/BaseEntity`
declares `id`, `createdAt` and `updatedAt`; `entity/AuditedEntity` extends it with the usernames
`createdBy` / `lastModifiedBy`, which `AuditorAwareImpl` fills in for every entity Spring Data
writes. Both are `@MappedSuperclass`, so neither gets a table of its own and each entity maps the
inherited fields onto its own table exactly as it did when they were written out per entity.

`AuditingEntityListener` is the **only** writer of `createdAt` / `updatedAt`: it fills them on
persist (`@PrePersist` / `@PreUpdate`), so an entity must not stamp them itself — a stamp in a
constructor is overwritten before the row is written, and only a transient instance ever sees it.
The clock behind them is `JpaConfig`'s `DateTimeProvider` bean, which returns UTC. The columns are
MySQL `datetime`, which store no zone, so without that bean the wall clock would be whatever zone
the host JVM happened to run in — UTC in the production container, CET on a developer's machine —
and the same instant would be recorded two hours apart on the two.

The split in two is what makes the hierarchy schema-neutral: `AuditedEntity` exists because the
`by` columns are *not* universal, so an entity whose table has no `created_by` /
`last_modified_by` extends `BaseEntity` and inherits none — nobody gains a column, so no
migration.

Four entities stay outside the hierarchy, each for its own reason:

- `CampaignUser` — `@EmbeddedId`; its key is the pair of foreign keys, not a generated number.
- `Spell` — the primary key is the Open5e `slug` (a string), not a generated number.
- `Dnd5eCharacterData` / `OffworldersCharacterData` — the shared primary key *is* the character's,
  and the audit trail belongs to the character.
- `EmailWhitelist` — it does have a generated id, but its table has no `created_at` /
  `updated_at`, so extending the base would buy unused columns at the price of a migration.

| Entity | Key fields | Relations |
|--------|-----------|-----------|
| `User` | id, email, role, providerType | has many GameCharacters, CampaignUsers |
| `GameCharacter` | id, name, systemType (nullable), notes, avatarUrl, imageUrls (JSON), appearance, backstory, privateBackstory | generic core character; belongs to User, optionally linked to Campaign; 1:1 with Dnd5eCharacterData when `systemType = DND5E`, 1:1 with OffworldersCharacterData when `systemType = OFFWORLDERS`, and neither when `systemType is null` (a character may be created without a game system — `system_type` is nullable and CHECK-constrained to `NULL` or a registered system in the baseline schema; extend `chk_character_system_type` when `GameSystem` gains a constant). `appearance`/`backstory` are public to the campaign; `privateBackstory` is only returned to the owner or the campaign GM |
| `Dnd5eCharacterData` | characterId (shared PK), level, characterClass, race, hitPoints, armorClass, stats (JSON) | D&D 5e data; 1:1 with GameCharacter; many-to-many with Spell via `character_spell` |
| `OffworldersCharacterData` | characterId (shared PK), characterClass, species, xp, health, currentHealth, healthModifier, armor, supply, supplyMax, credits, stats (JSON), skills (JSON), abilities (JSON), weapons (JSON), items (JSON) | Offworlders data; 1:1 with GameCharacter. `skills`/`abilities` are JSON arrays of `{ name, description }` entries; `weapons` is a typed array `{ type, description }`; `items` is a free-text array `{ name, description }` (appearance lives on the generic GameCharacter) |
| `Campaign` | id, name, imageUrl | has many CampaignUsers |
| `CampaignUser` | campaignId + userId (composite PK), role | join table Campaign ↔ User |
| `Ship` | id, campaignId (unique), version, name, hull, hullMax, armor, damage, driveFuel, maxDriveFuel, credits, upgrades (JSON), notes, imageUrl, imageUrls (JSON) | Offworlders ship; 1:1 with Campaign; optimistic concurrency via JPA `@Version` on `version`, which Hibernate enforces with `UPDATE ... WHERE version = ?` (a stale flush becomes a 409 via `ObjectOptimisticLockingFailureException`). `ShipService` additionally compares the version token the client sent against the freshly loaded row and throws `ConcurrentUpdateException` (also 409) before doing any work — that check alone cannot see a writer that commits between the load and the flush |
| `Message` | id, content, timestamp | belongs to Campaign |
| `Journal` | id, campaignId + userId (unique) | a participant's private journal in one campaign; belongs to Campaign and User, has many JournalPages. FKs are `ON DELETE CASCADE`, so deleting a campaign or a user takes the journals with it — removing a participant does **not**, so the rows survive a rejoin. Only its owner can read or write it: not another participant, not the campaign owner and not a GM |
| `JournalPage` | id, journalId, content (LONGTEXT markdown), position (1-based), version | one numbered page. `position` is the page's place in the book and what the client shows: a page inserted after page P shifts every later `position` by one (one bulk UPDATE) and a deleted page shifts them back, while `id` stays stable. The trailing blank page is virtual and never stored. `version` gives the same two-layer optimistic concurrency as the ship (`ConcurrentUpdateException` 409, plus `UPDATE ... WHERE version = ?` on flush) |
| `Spell` | slug (PK), name, rawJsonData (TEXT) | many-to-many with Dnd5eCharacterData; stored in local DB from bulk Open5e import |
| `CampaignAddon` | id, campaign_id + addon_key (unique), created_by | **the optional-capability switch**: one row per add-on a campaign owner has turned on, keyed by a stable string (`PLANNER`, exposed as the API key `planner`). The row's presence *is* the state, so the next add-on costs a registry entry and no migration. Every campaign answer carries the enabled keys as `addons`, which is what the client's navigation filters on. No planner data lives here — switching the planner off and on again returns the plans |
| `PlannerPlan` | id, campaign_id, title, description (LONGTEXT markdown), status (`OPEN`/`DECIDED`), decided_date_id, created_by_user_id, version | one plan in the planner: the question a group is answering ("when do we play next?"). Any participant may start one. `decided_date_id` must be one of the plan's own dates and is cleared on reopen, so status and date cannot disagree. Optimistic concurrency as on the ship and journal pages |
| `PlannerDate` | id, plan_id + plan_date (unique), start_time/end_time (optional), note, created_by_user_id, version | one candidate date. One row per day, which is what keeps a day's answers that day's answers. `ON DELETE SET NULL` on the plan's decision: a plan that had decided on a deleted date stays decided but loses the date |
| `PlannerAttendance` | id, plan_date_id + user_id (unique), state (`YES`/`NO`/`MAYBE`) | one member's answer to one date. **No row means "not answered yet"**, which counts against a date — that is what makes the green marker mean "everybody still in said yes". Deliberately without a version token: it is the caller's own answer |
| `PlannerMemberExclusion` | id, plan_id + user_id (unique), removed_by, removed_by_owner | a member who is out of a plan. The roster is the campaign's participants minus these rows, so everybody is in a plan without any bookkeeping. `removed_by_owner` is the member's own move versus the owner's: a member undoes their own by rejoining, while a removal by the owner is undone by the owner putting them back. Answers survive, so either way the return restores them |
| `EmailWhitelist` | id, email, role, active | standalone; **login gate** for OAuth — a revoked row is soft-deleted (`active = false`) so the decision can be revisited, and the address column is unique, so re-admitting reactivates that row. `role` applies to a *first* login only |
| `AuditEvent` | id, eventType, actorId/actorName, targetId/targetName, detail, clientIp, createdAt | one event of the audit trail (see "Audit trail"). **Append-only and deliberately without foreign keys**: the actor and target names are copies, so a row keeps reading correctly after an account is renamed or deleted. The only delete is the retention prune |

## Tests
- Integration tests use Testcontainers (`AbstractIntegrationTest`)
- `TestContainersConfiguration` spins up the MySQL container
  (`org.testcontainers.mysql.MySQLContainer`, module `testcontainers-mysql`)
- MockMvc and TestRestTemplate come from `spring-boot-starter-webmvc-test`; Boot 4
  needs `@AutoConfigureTestRestTemplate` on a test class before it can inject a
  `TestRestTemplate` (it used to depend on the classpath alone)
- `MessageControllerIT` pins the `/api` wire format the frontend parses
  (`createdAt` as an ISO-8601 local date-time in UTC, null fields still written), and is
  also where JPA auditing is proven end to end: it asserts the stored value really is the
  UTC clock, which fails on a non-UTC machine if the provider stops being wired in

Coverage, by area — unit tests (`*Test`, no Docker) and integration tests (`*IT`,
full application against a Testcontainer):

| Area | Unit | Integration |
|------|------|-------------|
| Auth, sessions, OAuth linking | `AuthControllerLoginTest`, `CustomUserDetailsTest` | `AuthControllerIT`, `LoginCredentialsIT`, `SecurityConfigIT`, `EndpointAuthenticationIT`, `UserServiceOAuthLoginIT` |
| Login throttle | `LoginAttemptLimiterTest`, `ClientIpResolverTest` | `LoginRateLimitIT` |
| Campaigns & permissions | `CampaignServiceTest`, `CampaignParticipantServiceTest`, `CampaignPermissionServiceTest`, `CampaignMapperTest`, `CampaignParticipantFactoryTest` | `CampaignPermissionServiceIT` |
| Characters | `GameCharacterMapperTest`, `GameCharacterServiceTest` | — (reached through the campaign and message ITs) |
| Ships | `ShipServiceTest` | `ShipOptimisticLockIT` |
| Messages | `MessageMapperTest` | `MessageControllerIT` — also the JSON wire format the frontend parses |
| Journals | `JournalServiceTest` | `JournalControllerIT` (ownership, page ordering, the virtual last page, the limits, concurrent first write), `JournalOptimisticLockIT` |
| Campaign add-ons & planner | `PlannerServiceTest` | `PlannerControllerIT` (owner-only add-on switch, the add-on gating the endpoints, participant-only access, dates and their 409s, one answer per member, deciding and reopening, both kinds of roster removal and the owner putting a member back) |
| Spells | `SpellServiceTest` | `SpellServiceIT` |
| Admin & whitelist | `AdminServiceTest` | `AdminControllerIT` |
| Audit trail | `AuditServiceTest` | `AuditTrailIT` (both write policies, the rollback a refusal survives, the prune), `AdminControllerIT` (the endpoint) |
| Files & images | `FileStorageServiceTest`, `StoredFileTransactionTest` | `ImageCorsIT` |
| Schema & runtime | `DemoProfileBootTest` (the `demo` profile's entity-generated schema + seed) | `SchemaValidationIT` (Flyway baseline + Hibernate `validate`), `BackendApplicationIT`, `VirtualThreadsIT` |

`./mvnw test` runs the unit column alone; `./mvnw verify` runs both, which is what
CI does on every pull request.