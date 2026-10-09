# Architecture — unseenservant-backend

## Tech stack
- Java 21
- Spring Boot
- Spring Security (JWT + OAuth2)
- Spring Data JPA
- MySQL
- Testcontainers (integration tests)

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
│   ├── OffworldersItem.java              # Free-form inventory item value object
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
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── UnauthorizedAccessException.java
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
- Email whitelist controls who can register (`EmailWhitelistService`)
- All `/api/**` endpoints require authentication unless explicitly permitted

### Error handling
- `GlobalExceptionHandler` handles all exceptions via `@RestControllerAdvice`
- Custom exceptions: `ResourceNotFoundException` (404), `UnauthorizedAccessException` (403), `UniqueConstraintViolationException` (409)

### External HTTP calls
When backend needs to call external APIs (e.g. Open5e for lazy-loading content), use Spring's `RestClient` or `WebClient` — not `RestTemplate`.

## Existing entities (summary)

| Entity | Key fields | Relations |
|--------|-----------|-----------|
| `User` | id, email, role, providerType | has many GameCharacters, CampaignUsers |
| `GameCharacter` | id, name, systemType, notes, avatarUrl | generic core character; belongs to User, optionally linked to Campaign; 1:1 with Dnd5eCharacterData when `systemType = DND5E`, 1:1 with OffworldersCharacterData when `systemType = OFFWORLDERS` |
| `Dnd5eCharacterData` | characterId (shared PK), level, characterClass, race, hitPoints, armorClass, stats (JSON) | D&D 5e data; 1:1 with GameCharacter; many-to-many with Spell via `character_spell` |
| `OffworldersCharacterData` | characterId (shared PK), characterClass, species, look, xp, health, currentHealth, healthModifier, armor, supply, supplyMax, credits, stats (JSON), skills (JSON), abilities (JSON), items (JSON) | Offworlders data; 1:1 with GameCharacter. `skills`/`abilities` are JSON arrays of `{ name, description }` entries so players can mix canonical options with free text; `items` is a free-form inventory array |
| `Campaign` | id, name, imageUrl | has many CampaignUsers |
| `CampaignUser` | campaignId + userId (composite PK), role | join table Campaign ↔ User |
| `Message` | id, content, timestamp | belongs to Campaign |
| `Spell` | slug (PK), name, rawJsonData (TEXT) | many-to-many with Dnd5eCharacterData; stored in local DB from bulk Open5e import |
| `EmailWhitelist` | email | standalone |

## Tests
- Integration tests use Testcontainers (`AbstractIntegrationTest`)
- `TestContainersConfiguration` spins up MySQL container
- Current coverage: auth, security config, basic smoke test