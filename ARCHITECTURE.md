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
├── playercharacter/
│   ├── controller/PlayerCharacterController.java
│   ├── dto/
│   │   ├── PlayerCharacterCampaignUpdateDTO.java
│   │   ├── PlayerCharacterInputDTO.java
│   │   └── PlayerCharacterOutputDTO.java
│   ├── entity/PlayerCharacter.java
│   ├── repository/PlayerCharacterRepository.java
│   ├── service/PlayerCharacterService.java
│   ├── PlayerCharacterMapper.java
│   ├── PlayerCharacterStats.java        # Stats value object
│   └── PlayerCharacterStatsConverter.java  # JPA AttributeConverter
├── spell/
│   ├── controller/SpellController.java
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
| `User` | id, email, role, providerType | has many PlayerCharacters, CampaignUsers |
| `PlayerCharacter` | id, name, stats (JSON) | belongs to User, optionally linked to Campaign; many-to-many with Spell via `character_spell` |
| `Campaign` | id, name, imageUrl | has many CampaignUsers |
| `CampaignUser` | campaignId + userId (composite PK), role | join table Campaign ↔ User |
| `Message` | id, content, timestamp | belongs to Campaign |
| `Spell` | slug (PK), name, rawJsonData (TEXT) | many-to-many with PlayerCharacter; lazy-loaded from Open5e and cached in DB |
| `EmailWhitelist` | email | standalone |

## Tests
- Integration tests use Testcontainers (`AbstractIntegrationTest`)
- `TestContainersConfiguration` spins up MySQL container
- Current coverage: auth, security config, basic smoke test