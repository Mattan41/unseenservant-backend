# Integration Contract — Frontend ↔ Backend

This document is a **verification checklist** used at release to confirm the frontend and backend are aligned.
The canonical API reference is `API_REFERENCE.md` — generated from source code.

## Core Principles

- **Backward compatibility:** Never break existing endpoints without frontend approval
- **Explicit responses:** All endpoints return DTOs (never raw entities)
- **Error consistency:** Use standard HTTP status codes + custom exception types
- **Authentication:** All `/api/**` endpoints require JWT (except `/api/auth/login` and `/api/auth/oauth-init`)

---

## Authentication

**POST /api/auth/login**
- Request: `{ username, password }`
- Response body: `AuthDTO { id, username, email, role }`
- JWT returned in `Authorization` response header: `Bearer <token>` — **not** in the body
- Status: 200, 400, 401

**GET /api/auth/oauth-init**
- Query params: `provider` (google|github), `origin`
- Redirects to OAuth2 provider; sets HttpOnly `oauth_redirect_origin` cookie
- Disabled in `demo` profile
- Status: 302, 400

**GET /api/auth/me**
- Response: `AuthDTO { id, username, email, role }`
- Status: 200, 401

---

## User

**GET /api/users/me**
- Response: `UserDTO { id, username, email, displayName, role }`
- Status: 200, 401, 404

**GET /api/users/search?query=**
- Requires: ROLE_USER
- Response: `[ UserDTO ]` (excludes self)
- Status: 200, 401

**GET /api/users/{id}**
- Requires: ROLE_ADMIN
- Response: `UserDTO`
- Status: 200, 401, 403, 404

**PATCH /api/users/{id}**
- Requires: ROLE_USER, must be self or ADMIN
- Request: partial JSON object, e.g. `{ "displayName": "..." }`
- Response: `UserDTO`
- Status: 200, 400, 401, 403, 404

---

## Characters

**POST /api/characters**
- Requires: ROLE_USER
- Request: `PlayerCharacterInputDTO { ownerId?, campaignId?, name, level (0–20), characterClass, imageUrl?, race, playerCharacterData }`
- `ownerId` defaults to logged-in user if null
- Response: `PlayerCharacterOutputDTO`
- Status: 201, 400, 401

**GET /api/characters**
- Query params: `campaignId` (optional)
- Response: `[ PlayerCharacterOutputDTO ]`
- Status: 200, 401

**GET /api/characters/me**
- Requires: ROLE_USER
- Response: `[ PlayerCharacterOutputDTO ]` — all characters owned by the logged-in user
- Status: 200, 401

**GET /api/characters/without-campaign**
- Requires: ROLE_USER
- Response: `[ PlayerCharacterOutputDTO ]` — owned characters not linked to any campaign
- Status: 200, 401

**GET /api/characters/{id}**
- Requires: ROLE_USER
- Response: `PlayerCharacterOutputDTO { id, ownerId, campaignId, name, level, characterClass, race, imageUrl, playerCharacterData, createdAt, updatedAt }`
- Status: 200, 401, 404

**PATCH /api/characters/{id}**
- Requires: ROLE_USER, must be owner
- Request: `PlayerCharacterInputDTO` — partial, null-safe patch. Omitted/null fields are left unchanged.
- Response: `PlayerCharacterOutputDTO`
- Status: 200, 400, 401, 403, 404

**POST /api/characters/{id}/image**
- Requires: ROLE_USER, must be owner
- Request: `multipart/form-data`, field: `file`
- Response: `PlayerCharacterOutputDTO` (with updated imageUrl)
- Status: 200, 400, 401, 403

**PATCH /api/characters/{id}/campaign**
- Requires: ROLE_USER, must be owner
- Request: `{ campaignId: Long }`
- Response: `PlayerCharacterOutputDTO`
- Status: 200, 400, 401, 403, 404

**DELETE /api/characters/{id}/campaign**
- Requires: ROLE_USER, must be owner
- Sets campaignId = null without deleting the character
- Response: `PlayerCharacterOutputDTO`
- Status: 200, 401, 403, 404

**DELETE /api/characters/{id}**
- Requires: ROLE_USER, must be owner
- Status: 204, 401, 403, 404

---

## Campaigns

**POST /api/campaigns**
- Request: `CampaignCreationDTO { name, description, ownerId?, participants? }`
- `ownerId` defaults to logged-in user if null
- Response: `CampaignResponseDTO`
- Status: 201, 400, 401

**GET /api/campaigns**
- Returns all campaigns (no filtering)
- Response: `[ CampaignResponseDTO ]`
- Status: 200, 401

**GET /api/campaigns/me**
- Requires: ROLE_USER
- Response: `[ CampaignResponseDTO ]` — campaigns where logged-in user is a participant
- Status: 200, 401

**GET /api/campaigns/{id}**
- Requires: authorized participant
- Response: `CampaignResponseDTO { id, name, description, imageUrl, ownerId, participants: [ { id, nickname, role } ] }`
- Status: 200, 401, 403, 404

**PUT /api/campaigns/{id}**
- Request: `CampaignUpdateDTO { name, description }`
- Response: `CampaignResponseDTO`
- Status: 200, 400, 401, 403, 404

**POST /api/campaigns/{id}/image**
- Request: `multipart/form-data`, field: `file`
- Response: `CampaignResponseDTO` (with updated imageUrl)
- Status: 200, 400, 401, 403

**PATCH /api/campaigns/{id}/participants**
- Request: `UpdateParticipantsDTO { participantsToAdd: [ ParticipantResponseDTO ], participantIdsToRemove: [ Long ] }` (`nickname` optional, defaults server-side)
- Response: `CampaignResponseDTO`
- Status: 200, 400, 401, 403, 404

**PATCH /api/campaigns/{id}/participants/{participantId}/nickname**
- Request body: plain string
- Response: `CampaignResponseDTO`
- Status: 200, 401, 403, 404

**PATCH /api/campaigns/{id}/participants/{participantId}/role**
- Request body: plain string — `"GM"` or `"PLAYER"`
- Response: `CampaignResponseDTO`
- Status: 200, 400, 401, 403, 404

**PATCH /api/campaigns/{id}/owner**
- Requires: current owner
- Request: `{ newOwnerId: Long }`
- Response: `CampaignResponseDTO`
- Status: 200, 401, 403, 404

**DELETE /api/campaigns/{id}**
- Requires: owner
- Status: 204, 401, 403, 404

---

## Messages

Intended as per-campaign message boards. Messages are visible only to campaign participants.

**GET /api/messages/campaign/{campaignId}**
- Requires: campaign participant
- Response: `[ MessageDTO ]`
- Status: 200, 401, 403, 404

**GET /api/messages/{id}**
- Requires: campaign participant
- Response: `MessageDTO { id, campaignId, userId, messageBody, createdAt, updatedAt }`
- Status: 200, 401, 403, 404

**POST /api/messages**
- Requires: campaign participant
- Request: `{ campaignId, messageBody }`
  - `campaignId`: Long (required)
  - `messageBody`: string (not blank, max 10000 chars)
- Response: `MessageDTO`
- Status: 200, 400, 401, 403, 404
- Note: The sender's user ID is automatically set from the authenticated principal (JWT). Do not include `userId` in the request.

**DELETE /api/messages/{id}**
- Requires: message sender (only the user who created the message can delete it)
- Status: 204, 401, 403, 404
- Note: Campaign GMs cannot delete other users' messages.

---

## Spells

> Spells are stored in the local DB from bulk Open5e import. Shared spell cache — removing a spell from a character does not delete the spell record.
> See **Client Routing Policy** above: only authenticated sessions use these endpoints. Guest mode and anonymous visitors never call them.

**GET /api/spells**
- Requires: ROLE_USER
- Query params:
  - `query` (string, default `""`) — case-insensitive name search. Blank/empty query returns `count: 0, results: []` (does **not** return all spells).
  - `page` (integer, default `0`, 0-indexed)
  - `size` (integer, default `20`, capped server-side at `100`)
- Response: `{ count: number, results: [ spell objects ] }`
  - This is a **flat object**, not a Spring Data `Page` (no `content`, `totalPages`, etc.)
  - Each result is the full Open5e v2 spell object
  - If JSON parsing fails for a spell, returns `{ slug, name, error }` fallback
- Status: 200, 401

**GET /api/spells/{slug}** — ⚠️ *planned, not yet implemented*
- Requires: ROLE_USER
- Path param: `slug`
- Response: full Open5e-shaped spell object, or `404` if not found in local DB
- Status: 200, 401, 404

**POST /api/characters/{characterId}/spells**
- Requires: ROLE_USER, must own character
- Request: `{ slug, name }`
  - `slug` is the spell identifier (required)
  - `name` is the spell name (required)
- Response: `CharacterSpellResponseDTO { characterId, slug, name, spellData }`
  - `spellData` is the full spell object (raw JSON)
- Backend behavior:
  - 403 if user does not own character
  - 404 if spell slug not found in local DB
  - Links spell to character via `character_spell` join table
- Status: 201, 400, 401, 403, 404

**GET /api/characters/{characterId}/spells**
- Requires: ROLE_USER, must own character
- Response: `[ CharacterSpellResponseDTO ]`
- Status: 200, 401, 403, 404

**DELETE /api/characters/{characterId}/spells/{slug}**
- Requires: ROLE_USER, must own character
- Removes spell from character's list; does **not** delete the shared spell record
- Status: 204, 401, 403, 404

---

## Error Response Format

```json
{
  "status": 404,
  "message": "Character not found",
  "timestamp": "2026-05-29T12:00:00Z",
  "path": "/api/characters/999"
}
```

**Common status codes:**
- `200` — Success
- `201` — Created
- `204` — No Content (successful DELETE)
- `400` — Bad Request (validation error)
- `401` — Unauthorized (missing/invalid JWT)
- `403` — Forbidden (not owner/insufficient permissions)
- `404` — Not Found
- `409` — Conflict (unique constraint violation)
- `500` — Server Error

---

## Data Structure Examples

### AuthDTO
```json
{
  "id": 1,
  "username": "admin",
  "email": "admin@admin.se",
  "role": "ROLE_ADMIN"
}
```

### UserDTO
```json
{
  "id": 7,
  "username": "user1@example.com",
  "email": "user1@example.com",
  "displayName": "User1",
  "role": "USER"
}
```

### PlayerCharacterOutputDTO
```json
{
  "id": 14,
  "ownerId": 7,
  "campaignId": null,
  "name": "Aragorn",
  "level": 5,
  "characterClass": "Fighter",
  "race": "Human",
  "imageUrl": null,
  "playerCharacterData": {
    "strength": 16,
    "dexterity": 14,
    "constitution": 15,
    "intelligence": 12,
    "wisdom": 14,
    "charisma": 13
  },
  "createdAt": "2026-05-29T09:00:00",
  "updatedAt": null
}
```

### CampaignResponseDTO
```json
{
  "id": 2,
  "name": "Neptune",
  "description": "...",
  "imageUrl": "https://...",
  "ownerId": 2,
  "participants": [
    { "id": 1, "nickname": "User1", "role": "PLAYER" },
    { "id": 2, "nickname": "User2", "role": "GM" }
  ]
}
```

### MessageDTO
```json
{
  "id": 1,
  "campaignId": 1,
  "userId": 3,
  "messageBody": "Hello!",
  "createdAt": "2026-05-29T10:00:00",
  "updatedAt": "2026-05-29T10:00:00"
}
```

### CharacterSpellResponseDTO
```json
{
  "characterId": 14,
  "slug": "fireball",
  "name": "Fireball",
  "spellData": {
    "slug": "fireball",
    "name": "Fireball",
    "level": 3,
    "school": "evocation",
    "desc": "A bright streak flashes from your pointing finger..."
  }
}
```

> `spellData` is the raw Open5e v2 response. Its shape is Open5e's — not controlled by this backend.

---

## Notes for Teams

**Frontend:** This contract defines what you can expect. If backend deviates, flag it immediately.

**Backend:** This contract is binding. Changes require frontend approval and must be backward compatible or coordinated with a versioned API change.

**Both:** Use this document during code review to ensure compliance. Consider mirroring this contract as a runnable Bruno collection so drift between this document and the real endpoints is caught automatically instead of found during manual review.