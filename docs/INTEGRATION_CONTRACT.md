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
- Request: `CharacterInputDTO { ownerId?, campaignId?, name, systemType, notes?, avatarUrl?, appearance?, backstory?, privateBackstory?, dnd5e?, offworlders? }`
  - `systemType`: `DND5E` | `OFFWORLDERS`
  - `appearance` / `backstory` / `privateBackstory`: free-text fields on the core character (any system). `appearance` and `backstory` are visible to every campaign member; `privateBackstory` is visible to the owner and the campaign GM only. Writes are owner-only (PATCH already requires ownership; a GM has read access, not write).
  - `dnd5e`: `{ level (1–20), characterClass, race, hitPoints?, armorClass?, stats }` — only used when `systemType === 'DND5E'`
  - `offworlders`: `{ characterClass?, species?, xp?, health?, currentHealth?, healthModifier?, armor?, supply?, supplyMax?, credits?, stats?, skills?, abilities?, weapons?, items? }` — only used when `systemType === 'OFFWORLDERS'`
    - `stats = { strength, agility, intelligence, willpower }`, each −1…+3; Health is derived as `max(1, 12 + strength + agility)`
    - `characterClass` is optional — it may be empty ("no class"), since experienced players may ignore classes
    - `armor` is a single value 0–3 (0 None / 1 Light / 2 Heavy / 3 Assault); `supplyMax` is fixed at `3` by the rules
    - `weapons = [ { type: 'Light' | 'Medium' | 'Heavy', description } ]` — damage (1D6 / 1D6+1 / 1D6+2) and `heavy` follow from `type`, not stored
    - `items = [ { name, description } ]` — free-text catch-all (custom weapons and everything else); `credits` is the tracked currency
    - `currentHealth` is the running HP (it may exceed `health` for temporary HP); `healthModifier` is a manual ± adjustment to the derived maximum
    - `skills` / `abilities` are arrays of entries `{ name, description }`: canonical catalog entries plus any free-text custom entries
- `ownerId` defaults to logged-in user if null
- Response: `CharacterOutputDTO`
- Status: 201, 400, 401

**GET /api/characters**
- Query params: `campaignId` (optional)
- Response: `[ CharacterOutputDTO ]`
- Status: 200, 401

**GET /api/characters/me**
- Requires: ROLE_USER
- Response: `[ CharacterOutputDTO ]` — all characters owned by the logged-in user
- Status: 200, 401

**GET /api/characters/without-campaign**
- Requires: ROLE_USER
- Response: `[ CharacterOutputDTO ]` — owned characters not linked to any campaign
- Status: 200, 401

**GET /api/characters/{id}**
- Requires: ROLE_USER
- Response: `CharacterOutputDTO { id, ownerId, campaignId, name, systemType, notes, avatarUrl, appearance, backstory, privateBackstory, dnd5e, offworlders, createdAt, updatedAt }`
  - `privateBackstory` is `null` unless the requester is the character's owner or the campaign GM
  - `dnd5e` is a nullable nested block `{ level, characterClass, race, hitPoints, armorClass, stats }`, present only when `systemType === 'DND5E'`
  - `offworlders` is a nullable nested block `{ characterClass, species, xp, health, currentHealth, healthModifier, armor, supply, supplyMax, credits, stats, skills, abilities, weapons, items }`, present only when `systemType === 'OFFWORLDERS'`
- Status: 200, 401, 404

**PATCH /api/characters/{id}**
- Requires: ROLE_USER, must be owner
- Request: `CharacterInputDTO` — partial, null-safe patch. Omitted/null fields are left unchanged. The nested `dnd5e` object is patch-applied field-by-field when `systemType === 'DND5E'`; likewise the nested `offworlders` object when `systemType === 'OFFWORLDERS'`.
- Response: `CharacterOutputDTO`
- Status: 200, 400, 401, 403, 404

**POST /api/characters/{id}/image**
- Requires: ROLE_USER, must be owner
- Request: `multipart/form-data`, field: `file`
- Response: `CharacterOutputDTO` (with updated avatarUrl)
- Status: 200, 400, 401, 403

**PATCH /api/characters/{id}/campaign**
- Requires: ROLE_USER, must be owner
- Request: `{ campaignId: Long }`
- Response: `CharacterOutputDTO`
- Status: 200, 400, 401, 403, 404

**DELETE /api/characters/{id}/campaign**
- Requires: ROLE_USER, must be owner
- Sets campaignId = null without deleting the character
- Response: `CharacterOutputDTO`
- Status: 200, 401, 403, 404

**DELETE /api/characters/{id}**
- Requires: ROLE_USER, must be owner
- Status: 204, 401, 403, 404

---

## Campaigns

**POST /api/campaigns**
- Request: `CampaignCreationDTO { name, description, privateDescription?, ownerId?, participants?, primarySystem? }`
- `ownerId` defaults to logged-in user if null
- `privateDescription`: GM-only description (returned to the owner/GM only)
- `primarySystem`: `DND5E` | `OFFWORLDERS` | null (optional; `null` = not chosen). If `OFFWORLDERS`, the backend also creates the campaign's default ship.
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
- Response: `CampaignResponseDTO { id, name, description, privateDescription, imageUrl, primarySystem, ownerId, participants: [ { id, nickname, role } ] }`
  - `privateDescription` is `null` unless the requester is the campaign owner or a campaign GM
- Status: 200, 401, 403, 404

**PUT /api/campaigns/{id}**
- Request: `CampaignUpdateDTO { name, description, privateDescription, primarySystem }` (`primarySystem`: `DND5E` | `OFFWORLDERS` | null)
- Setting `primarySystem` to `OFFWORLDERS` creates the campaign's default ship the first time (idempotent). Changing away from `OFFWORLDERS` keeps the ship.
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
- Note: A campaign's ship (if any) is removed with the campaign.

---

## Campaign Ship (Offworlders)

A ship is `1:1` with a campaign (unique `campaign_id` FK) and only meaningful when the
campaign's `primarySystem` is `OFFWORLDERS`. Defaults mirror the Offworlders rulebook
(p.13): 15 Hull, 0 Armor, 1D6 Damage, 4 Max Drive Fuel, no upgrades. Every campaign
participant may view and edit; there is **no delete**.

**GET /api/campaigns/{id}/ship**
- Requires: campaign participant
- Response: `ShipDTO` (see Data Structure Examples)
- Status: 200, 401, 403, 404 (`404` when the campaign has no ship yet)

**PUT /api/campaigns/{id}/ship**
- Requires: campaign participant
- Request: `ShipDTO` fields including `version`
- Upsert: creates the ship with the defaults if the campaign has none, otherwise updates it in place
- Optimistic concurrency: a stale `version` is rejected with `409` (no pessimistic locking)
- Response: `ShipDTO` (with the incremented `version`)
- Status: 200, 400, 401, 403, 409

**POST /api/campaigns/{id}/ship/image**
- Requires: campaign participant
- Request: multipart/form-data with field `file`
- Replaces the ship's profile image (`imageUrl`); the UI falls back to `/defaultShip.svg`
- Response: `ShipDTO`
- Status: 200, 400, 401, 403, 404

**POST /api/campaigns/{id}/ship/images**
- Requires: campaign participant
- Request: multipart/form-data with field `file`
- Appends an image to the ship's gallery (`imageUrls`)
- Response: `ShipDTO`
- Status: 200, 400, 401, 403, 404

**DELETE /api/campaigns/{id}/ship/images?url={url}**
- Requires: campaign participant
- Removes the matching image from the ship's gallery (`imageUrls`)
- Response: `ShipDTO`
- Status: 200, 401, 403, 404

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

**GET /api/spells/{slug}**
- Requires: ROLE_USER
- Path param: `slug`
- Response: full Open5e-shaped spell object, or `404` if not found in local DB
  - If JSON parsing fails for a spell, returns `{ slug, name, error }` fallback
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

### CharacterOutputDTO
```json
{
  "id": 14,
  "ownerId": 7,
  "campaignId": null,
  "name": "Aragorn",
  "systemType": "DND5E",
  "notes": null,
  "avatarUrl": null,
  "appearance": null,
  "backstory": null,
  "privateBackstory": null,
  "dnd5e": {
    "level": 5,
    "characterClass": "Fighter",
    "race": "Human",
    "hitPoints": 44,
    "armorClass": 17,
    "stats": {
      "strength": 16,
      "dexterity": 14,
      "constitution": 15,
      "intelligence": 12,
      "wisdom": 14,
      "charisma": 13
    }
  },
  "offworlders": null,
  "createdAt": "2026-05-29T09:00:00",
  "updatedAt": null
}
```

> The nested `dnd5e` / `offworlders` block is populated only for the matching `systemType`; the other is `null`.
> `privateBackstory` is `null` unless the requester is the character's owner or the campaign GM.

### CampaignResponseDTO
```json
{
  "id": 2,
  "name": "Neptune",
  "description": "...",
  "privateDescription": "GM-only notes...",
  "imageUrl": "https://...",
  "primarySystem": "OFFWORLDERS",
  "ownerId": 2,
  "participants": [
    { "id": 1, "nickname": "User1", "role": "PLAYER" },
    { "id": 2, "nickname": "User2", "role": "GM" }
  ]
}
```

### ShipDTO
```json
{
  "id": 1,
  "campaignId": 2,
  "name": "The Null Gravitas",
  "hull": 15,
  "hullMax": 15,
  "armor": 1,
  "damage": "1D6",
  "driveFuel": 4,
  "maxDriveFuel": 6,
  "upgrades": ["Additional Armor", "Fuel Tanks"],
  "notes": "Cargo: a sealed crate.",
  "imageUrl": null,
  "imageUrls": [],
  "version": 3
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