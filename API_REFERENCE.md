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
  "level": 5,
  "characterClass": "Fighter",
  "race": "Human",
  "imageUrl": null,
  "campaignId": null,
  "playerCharacterData": {
    "strength": 16,
    "dexterity": 14,
    "constitution": 15,
    "intelligence": 12,
    "wisdom": 14,
    "charisma": 13
  }
}
```

| Field | Type | Rules |
|-------|------|-------|
| `name` | string | — |
| `level` | int | 0–20 |
| `characterClass` | string | — |
| `race` | string | — |
| `imageUrl` | string | Optional |
| `campaignId` | Long | Optional |
| `ownerId` | Long | Optional; defaults to logged-in user if null |
| `playerCharacterData` | object | See stats structure below |

> If `ownerId` is `null`, the logged-in user is automatically set as owner.

**Response (201):**
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

---

### GET /api/characters — Get characters

**Auth:** Yes  
**Status:** 200, 401

**Query params:**

| Param | Required | Description |
|-------|----------|-------------|
| `campaignId` | No | Filter by campaign ID |

**Response (200):** List of `PlayerCharacterOutputDTO`.

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

**Response (200):** `PlayerCharacterOutputDTO`

---

### PATCH /api/characters/{characterId} — Update character

**Auth:** Yes (ROLE_USER, must be owner)  
**Status:** 200, 400, 401, 403, 404

**Request:** Full `PlayerCharacterInputDTO` (validated, `level` must be 0–20).

**Response (200):** `PlayerCharacterOutputDTO`

---

### POST /api/characters/{id}/image — Upload character image

**Auth:** Yes (ROLE_USER, must be owner)  
**Status:** 200, 400, 401, 403

**Request:** `multipart/form-data`, field name: `file`.

**Response (200):** `PlayerCharacterOutputDTO` with updated `imageUrl`.

---

### PATCH /api/characters/{characterId}/campaign — Link character to campaign

**Auth:** Yes (ROLE_USER, must be owner)  
**Status:** 200, 400, 401, 403, 404

**Request:**
```json
{ "campaignId": 2 }
```

**Response (200):** `PlayerCharacterOutputDTO`

[USES: Campaign]

---

### DELETE /api/characters/{characterId}/campaign — Unlink character from campaign

**Auth:** Yes (ROLE_USER, must be owner)  
**Status:** 200, 401, 403, 404

Sets `campaignId = null` without deleting the character.

**Response (200):** `PlayerCharacterOutputDTO`

---

### DELETE /api/characters/{characterId} — Delete character

**Auth:** Yes (ROLE_USER, must be owner)  
**Status:** 204, 401, 403, 404

---

## 5. Messages

`/api/messages/**` — requires authentication (via SecurityConfig).  
[USES: Campaign, User]

> **Planning status:** Backend implementation exists. No frontend integration yet. Intended for per-campaign message boards. Authorization logic (ownership, campaign membership) is not yet implemented at the endpoint level.

---

### GET /api/messages — Get all messages

**Auth:** Yes  
**Status:** 200, 401

**Response (200):**
```json
[
  {
    "campaignId": 1,
    "userId": 3,
    "messageBody": "Hello everyone!",
    "createdAt": "2026-05-29T10:00:00",
    "updatedAt": "2026-05-29T10:00:00"
  }
]
```

---

### GET /api/messages/campaign/{campaignId} — Get messages by campaign

**Auth:** Yes  
**Status:** 200, 401, 404

**Response (200):** List of `MessageDTO` for the given campaign.

---

### GET /api/messages/{id} — Get message by ID

**Auth:** Yes  
**Status:** 200, 401, 404

**Response (200):** `MessageDTO`

---

### POST /api/messages — Create message

**Auth:** Yes  
**Status:** 200, 400, 401

**Request:**
```json
{
  "campaignId": 1,
  "userId": 3,
  "messageBody": "Hello from the game table!"
}
```

> `createdAt` and `updatedAt` are automatically set to the current time if omitted.

**Response:** `MessageDTO`

---

### DELETE /api/messages/{id} — Delete message

**Auth:** Yes  
**Status:** 204, 401, 404

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
    "strength": 16, "dexterity": 14, "constitution": 15,
    "intelligence": 12, "wisdom": 14, "charisma": 13
  },
  "createdAt": "2026-05-29T09:00:00",
  "updatedAt": null
}
```

### MessageDTO
```json
{
  "campaignId": 1,
  "userId": 3,
  "messageBody": "Hello!",
  "createdAt": "2026-05-29T10:00:00",
  "updatedAt": "2026-05-29T10:00:00"
}
```
