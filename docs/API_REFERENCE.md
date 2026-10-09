# API Reference — Unseen Servant Backend

**Base URL:** `http://localhost:8080`  
**Auth:** JWT Bearer token in the `Authorization` header on all protected endpoints.  
**Role hierarchy:** `ADMIN` > `USER` > `GUEST`

> Generated from source code (controllers + DTOs). Verify against `INTEGRATION_CONTRACT.md` at release.

---

## Error Format

All errors are returned in this format (via `GlobalExceptionHandler`):

```json
{
  "status": 404,
  "message": "Character not found",
  "timestamp": "2026-05-29T12:00:00Z",
  "path": "/api/characters/999"
}
```

| Status | Meaning |
|--------|---------|
| `200` | OK |
| `201` | Created |
| `204` | No Content (DELETE) |
| `400` | Bad Request / validation error |
| `401` | Unauthorized |
| `403` | Forbidden (not owner / wrong role) |
| `404` | Not Found |
| `409` | Conflict (unique constraint) |
| `500` | Server Error |

---

## 1. Authentication

`/api/auth/**` — open to all except `/api/auth/me` which requires JWT.

---

### POST /api/auth/login — Log in

**Auth:** No  
**Status:** 200, 400, 401

**Request:**
```json
{
  "username": "admin",
  "password": "password"
}
```

| Field | Type | Rules |
|-------|------|-------|
| `username` | string | Not blank, max 255 chars |
| `password` | string | Not blank, 8–128 chars |

**Response (200):**
```json
{
  "id": 1,
  "username": "admin",
  "email": "admin@admin.se",
  "role": "ROLE_ADMIN"
}
```

> JWT is returned in the `Authorization` response header: `Bearer <token>` — **not** in the response body.

---

### GET /api/auth/oauth-init — Initiate OAuth2 login

**Auth:** No  
**Status:** 302, 400

**Query params:**

| Param | Required | Values |
|-------|----------|--------|
| `provider` | Yes | `google` \| `github` |
| `origin` | Yes | Must match `ALLOWED_ORIGINS` |

Sets an HttpOnly `oauth_redirect_origin` cookie (5 min TTL) and redirects to `/oauth2/authorization/{provider}`. OAuth2 is disabled in the `demo` profile.

---

### GET /api/auth/me — Get current authenticated user

**Auth:** Yes  
**Status:** 200, 401

**Response (200):**
```json
{
  "id": 1,
  "username": "admin",
  "email": "admin@admin.se",
  "role": "ROLE_ADMIN"
}
```

---

## 2. User

`/api/users/**` — requires authentication.

---

### GET /api/users/me — Get own profile

**Auth:** Yes (ROLE_USER)  
**Status:** 200, 401, 404

**Response (200):**
```json
{
  "id": 7,
  "username": "user1@example.com",
  "email": "user1@example.com",
  "displayName": "User1",
  "role": "USER"
}
```

> `displayName` falls back to `fullName` if no explicit display name is set.

---

### GET /api/users/search — Search users

**Auth:** Yes (ROLE_USER)  
**Status:** 200, 401

**Query params:**

| Param | Required | Description |
|-------|----------|-------------|
| `query` | Yes | Search term matched against username / email / displayName |

Returns a list of `UserDTO`, excluding the currently logged-in user.

**Response (200):**
```json
[
  {
    "id": 2,
    "username": "user2@example.com",
    "email": "user2@example.com",
    "displayName": "User2",
    "role": "USER"
  }
]
```

---

### GET /api/users/{id} — Get user by ID (admin only)

**Auth:** Yes (ROLE_ADMIN)  
**Status:** 200, 401, 403, 404

**Response (200):** See `UserDTO` above.

---

### PATCH /api/users/{id} — Update user profile

**Auth:** Yes (ROLE_USER; must be self or ADMIN)  
**Status:** 200, 400, 401, 403, 404

**Request:** Partial update as a JSON object:
```json
{
  "displayName": "New display name"
}
```

**Response (200):** `UserDTO`

---

## 3. Campaigns

`/api/campaigns/**` — requires authentication.  
[USES: User]

### ParticipantResponseDTO (embedded in campaign)

```json
{ "id": 1, "nickname": "DM", "role": "GM" }
```

Valid `role` values: `GM` | `PLAYER`

---

### POST /api/campaigns — Create campaign

**Auth:** Yes  
**Status:** 201, 400, 401

**Request:**
```json
{
  "name": "Neptune",
  "description": "An epic adventure campaign",
  "ownerId": null,
  "participants": []
}
```

> If `ownerId` is `null`, the logged-in user is automatically set as owner.

**Response (201):**
```json
{
  "id": 1,
  "name": "Neptune",
  "description": "An epic adventure campaign",
  "imageUrl": null,
  "ownerId": 7,
  "participants": []
}
```

---

### GET /api/campaigns — Get all campaigns

**Auth:** Yes  
**Status:** 200, 401

**Response (200):** List of `CampaignResponseDTO` (all campaigns, no filtering).

---

### GET /api/campaigns/me — Get my campaigns

**Auth:** Yes (ROLE_USER)  
**Status:** 200, 401

**Response (200):** List of campaigns where the logged-in user is a participant.

---

### GET /api/campaigns/{id} — Get campaign by ID

**Auth:** Yes (authorized participant)  
**Status:** 200, 401, 403, 404

**Response (200):**
```json
{
  "id": 1,
  "name": "Neptune",
  "description": "An epic adventure campaign",
  "imageUrl": "https://example.com/image.jpg",
  "ownerId": 7,
  "participants": [
    { "id": 1, "nickname": "The Hero", "role": "PLAYER" },
    { "id": 7, "nickname": "DM", "role": "GM" }
  ]
}
```

---

### PUT /api/campaigns/{id} — Update campaign

**Auth:** Yes  
**Status:** 200, 400, 401, 403, 404

**Request:**
```json
{
  "name": "Updated name",
  "description": "New description"
}
```

**Response (200):** `CampaignResponseDTO`

---

### POST /api/campaigns/{id}/image — Upload campaign image

**Auth:** Yes  
**Status:** 200, 400, 401, 403

**Request:** `multipart/form-data`, field name: `file`.

**Response (200):** `CampaignResponseDTO` with updated `imageUrl`.

---

### PATCH /api/campaigns/{id}/participants — Manage participants

**Auth:** Yes  
**Status:** 200, 400, 401, 403, 404

**Request:**
```json
{
  "participantsToAdd": [
    { "id": 3, "nickname": "The Wizard", "role": "PLAYER" }
  ],
  "participantIdsToRemove": [2]
}
```

**Response (200):** `CampaignResponseDTO`

---

### PATCH /api/campaigns/{id}/participants/{participantId}/nickname — Update participant nickname

**Auth:** Yes  
**Status:** 200, 401, 403, 404

**Request body:** Plain string (`text/plain` or JSON string):
```
"New nickname"
```

**Response (200):** `CampaignResponseDTO`

---

### PATCH /api/campaigns/{id}/participants/{participantId}/role — Update participant role

**Auth:** Yes  
**Status:** 200, 400, 401, 403, 404

**Request body:** Plain string — `"GM"` or `"PLAYER"`.

**Response (200):** `CampaignResponseDTO`

---

### PATCH /api/campaigns/{id}/owner — Transfer ownership

**Auth:** Yes (current owner)  
**Status:** 200, 401, 403, 404

**Request:**
```json
{ "newOwnerId": 3 }
```

**Response (200):** `CampaignResponseDTO`

---

### DELETE /api/campaigns/{id} — Delete campaign

**Auth:** Yes (owner)  
**Status:** 204, 401, 403, 404

---

## 4. Player Characters

`/api/characters/**` — requires authentication.  
[USES: Campaign, User]

---

### POST /api/characters — Create character

**Auth:** Yes (ROLE_USER)  
**Status:** 201, 400, 401

**Request:**
```json
{
  "name": "Aragorn",
  "systemType": "DND5E",
  "notes": null,
  "campaignId": null,
  "avatarUrl": null,
  "dnd5e": {
    "level": 5,
    "characterClass": "Fighter",
    "race": "Human",
    "hitPoints": 44,
    "armorClass": 17,
    "stats": {
      "strength": 16, "dexterity": 14, "constitution": 15,
      "intelligence": 12, "wisdom": 14, "charisma": 13
    }
  }
}
```

| Field | Type | Rules |
|-------|------|-------|
| `name` | string | — |
| `systemType` | enum | `DND5E` \| `OFFWORLDERS`; defaults to `DND5E` |
| `notes` | string | Optional |
| `avatarUrl` | string | Optional |
| `campaignId` | Long | Optional |
| `ownerId` | Long | Optional; defaults to logged-in user if null |
| `dnd5e` | object | Optional; applied only when `systemType === 'DND5E'`. Shape: `{ level (1–20), characterClass, race, hitPoints?, armorClass?, stats }` |
| `offworlders` | object | Optional; applied only when `systemType === 'OFFWORLDERS'`. Shape: `{ characterClass?, species?, look?, xp?, health?, armor?, supply?, supplyMax?, credits?, stats?, skills?, abilities?, items? }`. `stats = { strength, agility, intelligence, willpower }` (each −1…+3); `skills` / `abilities` are string arrays mixing canonical options with free text; `characterClass` is optional (may be empty); `supplyMax` is fixed at `3`; `items` is a free-form array `[ { name, kind: 'weapon'|'armor'|'item', damage, armorRating, heavy, notes } ]` |

> If `ownerId` is `null`, the logged-in user is automatically set as owner.

**Response (201):**
```json
{
  "id": 14,
  "ownerId": 7,
  "campaignId": null,
  "name": "Aragorn",
  "systemType": "DND5E",
  "notes": null,
  "avatarUrl": null,
  "dnd5e": {
    "level": 5,
    "characterClass": "Fighter",
    "race": "Human",
    "hitPoints": 44,
    "armorClass": 17,
    "stats": {
      "strength": 16, "dexterity": 14, "constitution": 15,
      "intelligence": 12, "wisdom": 14, "charisma": 13
    }
  },
  "createdAt": "2026-05-29T09:00:00",
  "updatedAt": null
}
```

**Offworlders example (201):**

The endpoint is system-agnostic. When `systemType` is `OFFWORLDERS`, send (and receive) the `offworlders` block instead of `dnd5e`:

```json
{
  "name": "Vex",
  "systemType": "OFFWORLDERS",
  "offworlders": {
    "characterClass": "Outlaw",
    "species": "Human",
    "look": "Sharp-eyed, patched flight jacket",
    "xp": 3,
    "health": 15,
    "armor": 1,
    "supply": 2,
    "supplyMax": 3,
    "credits": 10,
    "stats": {
      "strength": 1, "agility": 3, "intelligence": 1, "willpower": 0
    },
    "skills": ["Pilot", "Sneak", "Tech"],
    "abilities": ["Lucky", "Smuggle", "Shoot First"],
    "items": [
      { "name": "Snubnosed revolver", "kind": "weapon", "damage": "1D6", "armorRating": 0, "heavy": false, "notes": "" },
      { "name": "Light armor", "kind": "armor", "damage": "", "armorRating": 1, "heavy": false, "notes": "" }
    ]
  }
}
```

> Offworlders attributes range from −1 to +3 and Armor from 0 to 3. `health` may be omitted and
> derived from the attributes as `max(1, 12 + strength + agility)`. `supplyMax` is always 3.
> `characterClass` is optional. `skills` and `abilities` are string arrays so players can combine
> the canonical catalogs with free-text custom entries. `credits` and `items` are optional additions.

---
---

### GET /api/characters — Get characters

**Auth:** Yes  
**Status:** 200, 401

**Query params:**

| Param | Required | Description |
|-------|----------|-------------|
| `campaignId` | No | Filter by campaign ID |

**Response (200):** List of `CharacterOutputDTO`.

---

### GET /api/characters/me — Get my characters

**Auth:** Yes (ROLE_USER)  
**Status:** 200, 401

**Response (200):** List of all characters owned by the logged-in user.

---

### GET /api/characters/without-campaign — Characters without a campaign

**Auth:** Yes (ROLE_USER)  
**Status:** 200, 401

**Response (200):** List of characters owned by the logged-in user that are not linked to any campaign.

---

### GET /api/characters/{id} — Get character by ID

**Auth:** Yes (ROLE_USER)  
**Status:** 200, 401, 404

**Response (200):** `CharacterOutputDTO`

---

### PATCH /api/characters/{characterId} — Update character

**Auth:** Yes (ROLE_USER, must be owner)  
**Status:** 200, 400, 401, 403, 404

**Request:** Full `CharacterInputDTO` (validated, `level` must be 0–20).

**Response (200):** `CharacterOutputDTO`

---

### POST /api/characters/{id}/image — Upload character image

**Auth:** Yes (ROLE_USER, must be owner)  
**Status:** 200, 400, 401, 403

**Request:** `multipart/form-data`, field name: `file`.

**Response (200):** `CharacterOutputDTO` with updated `imageUrl`.

---

### PATCH /api/characters/{characterId}/campaign — Link character to campaign

**Auth:** Yes (ROLE_USER, must be owner)  
**Status:** 200, 400, 401, 403, 404

**Request:**
```json
{ "campaignId": 2 }
```

**Response (200):** `CharacterOutputDTO`

[USES: Campaign]

---

### DELETE /api/characters/{characterId}/campaign — Unlink character from campaign

**Auth:** Yes (ROLE_USER, must be owner)  
**Status:** 200, 401, 403, 404

Sets `campaignId = null` without deleting the character.

**Response (200):** `CharacterOutputDTO`

---

### DELETE /api/characters/{characterId} — Delete character

**Auth:** Yes (ROLE_USER, must be owner)  
**Status:** 204, 401, 403, 404

---

## 5. Messages

`/api/messages/**` — requires authentication (via SecurityConfig).  
[USES: Campaign, User]

Intended for per-campaign message boards. Messages are visible only to campaign participants.

---

### GET /api/messages/campaign/{campaignId} — Get messages by campaign

**Auth:** Yes (must be campaign participant)  
**Status:** 200, 401, 403, 404

**Response (200):** List of `MessageDTO` for the given campaign.

---

### GET /api/messages/{id} — Get message by ID

**Auth:** Yes (must be campaign participant)  
**Status:** 200, 401, 403, 404

**Response (200):** `MessageDTO`

---

### POST /api/messages — Create message

**Auth:** Yes (must be campaign participant)  
**Status:** 200, 400, 401, 403, 404

**Request:**
```json
{
  "campaignId": 1,
  "messageBody": "Hello from the game table!"
}
```

| Field | Type | Rules |
|-------|------|-------|
| `campaignId` | Long | Required |
| `messageBody` | string | Not blank, max 10000 chars |

> The sender's user ID is automatically set from the authenticated principal (JWT). Do not include `userId` in the request.

**Response (200):** `MessageDTO`

---

### DELETE /api/messages/{id} — Delete message

**Auth:** Yes (must be message sender)  
**Status:** 204, 401, 403, 404

> Only the user who created the message can delete it. Campaign GMs cannot delete other users' messages.

---

## 6. Spells

`/api/spells/**` and `/api/characters/{characterId}/spells` — requires authentication.  
[USES: GameCharacter, Dnd5eCharacterData]

Spells are stored in the local `spell` table from bulk Open5e import. The join table `character_spell` links a character's D&D 5e data (`dnd5e_character_data`) to spells (many-to-many).

---

### GET /api/spells — Search spells

**Auth:** Yes (ROLE_USER)  
**Status:** 200, 401

**Query params:**

| Param | Required | Description |
|-------|----------|-------------|
| `query` | No | Optional search term to filter spells by name (case-insensitive) |
| `page` | No | Page number (0-indexed), default `0` |
| `size` | No | Page size, default `20`, capped server-side at `100` |

Searches the local spell database. Blank/empty query returns `{ count: 0, results: [] }` (does not return all spells).

**Response (200):**
```json
{
  "count": 42,
  "results": [
    {
      "slug": "fireball",
      "name": "Fireball",
      "level": 3,
      "school": "evocation",
      "desc": "A bright streak flashes from your pointing finger...",
      "...": "..."
    }
  ]
}
```

Each result item is the full Open5e v2 spell object. If a spell's JSON data cannot be parsed, a fallback object with `slug`, `name`, and `error` fields is returned instead.

---

### GET /api/spells/{slug} — Get spell by slug

**Auth:** Yes (ROLE_USER)  
**Status:** 200, 401, 404

**Path params:**

| Param | Required | Description |
|-------|----------|-------------|
| `slug` | Yes | Spell identifier |

Looks up a spell by slug in the local database. Returns the full Open5e-shaped spell object, or 404 if not found.

**Response (200):**
```json
{
  "slug": "fireball",
  "name": "Fireball",
  "level": 3,
  "school": "evocation",
  "desc": "A bright streak flashes from your pointing finger...",
  "...": "..."
}
```

If the spell's JSON data cannot be parsed, a fallback object with `slug`, `name`, and `error` fields is returned instead.

---

### POST /api/characters/{characterId}/spells — Add spell to character

**Auth:** Yes (ROLE_USER, must be owner)  
**Status:** 201, 400, 401, 403, 404

**Path params:** `characterId` — the character to add the spell to.

**Request:**
```json
{
  "slug": "fireball",
  "name": "Fireball"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `slug` | string | Spell identifier (e.g. `"fireball"`) |
| `name` | string | Display name |

> The spell must exist in the local DB. Returns 404 if the spell slug is not found.

**Response (201):**
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
    "desc": "A bright streak flashes from your pointing finger...",
    "...": "..."
  }
}
```

`spellData` is the raw JSON object returned by Open5e — field set mirrors the Open5e v2 spell schema.

---

### GET /api/characters/{characterId}/spells — Get spells for character

**Auth:** Yes (ROLE_USER, must be owner)  
**Status:** 200, 401, 403, 404

**Response (200):** List of `CharacterSpellResponseDTO`:
```json
[
  {
    "characterId": 14,
    "slug": "fireball",
    "name": "Fireball",
    "spellData": { "...": "..." }
  }
]
```

---

### DELETE /api/characters/{characterId}/spells/{slug} — Remove spell from character

**Auth:** Yes (ROLE_USER, must be owner)  
**Status:** 204, 401, 403, 404

Removes the spell from the character's spell list. The `Spell` record itself is not deleted from the DB (it may be shared with other characters).

---

## Appendix — Common Data Structures

### AuthDTO
```json
{ "id": 1, "username": "admin", "email": "admin@admin.se", "role": "ROLE_ADMIN" }
```

### UserDTO
```json
{ "id": 7, "username": "user1@example.com", "email": "user1@example.com", "displayName": "User1", "role": "USER" }
```

### CampaignResponseDTO
```json
{
  "id": 1,
  "name": "Neptune",
  "description": "...",
  "imageUrl": "https://...",
  "ownerId": 7,
  "participants": [
    { "id": 1, "nickname": "The Hero", "role": "PLAYER" }
  ]
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
  "dnd5e": {
    "level": 5,
    "characterClass": "Fighter",
    "race": "Human",
    "hitPoints": 44,
    "armorClass": 17,
    "stats": {
      "strength": 16, "dexterity": 14, "constitution": 15,
      "intelligence": 12, "wisdom": 14, "charisma": 13
    }
  },
  "createdAt": "2026-05-29T09:00:00",
  "updatedAt": null
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

> `spellData` mirrors the full Open5e v2 spell object. Shape may vary by spell.
