# API Reference — Unseen Servant Backend

**Base URL:** `http://localhost:8080`  
**Auth:** JWT Bearer token in the `Authorization` header on all protected endpoints.  
**Role hierarchy:** `ADMIN` > `USER` > `GUEST`

> Generated from source code (controllers + DTOs). Verify against `INTEGRATION_CONTRACT.md` at release.

---

## Error Format

Every error body is an RFC 9457 `ProblemDetail` (`application/problem+json`),
whether it comes from `GlobalExceptionHandler`, from the framework's own handlers
(malformed body, unknown path, upload past the multipart limit) or from the
security filter chain's 401/403:

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Character not found with id: 999",
  "instance": "/api/characters/999"
}
```

Read `detail` for the sentence to show. Validation and unique-constraint failures
add `field` (and `value` where it helps) as extension properties:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "A display name may be at most 50 characters",
  "instance": "/api/users/7",
  "field": "displayName"
}
```

| Status | Meaning |
|--------|---------|
| `200` | OK |
| `201` | Created |
| `204` | No Content (DELETE) |
| `400` | Bad Request / validation error / malformed body or parameter |
| `401` | Unauthorized |
| `403` | Forbidden (not owner / wrong role) |
| `404` | Not Found |
| `409` | Conflict (unique constraint, concurrent update — e.g. a stale journal page `version`) |
| `413` | Content Too Large (upload past the 5MB or 8MB limit) |
| `429` | Too Many Requests (login throttle; `Retry-After` header) |
| `500` | Server Error (generic `detail`; the cause is logged) |

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

> **Account linking:** A returning OAuth login is matched first by provider identity (`providerType` + `providerId`) and then by email (case-insensitive). A person who already has an account under the same email — e.g. registered with a different provider — reuses that existing account; no duplicate user is created and the existing account's role / username / display name are left untouched.
>
> **Verified email required:** Google sign-in is accepted only when the provider reports a verified email (`email_verified = true`). A rejected login (unverified email, or an address that is not whitelisted) redirects to `{FRONTEND_URL}/under-construction` instead of `/oauth-redirect`, and no JWT is issued.

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
**Status:** 200, 400, 401, 403, 404, 409

**Request:** `UserProfileUpdateDTO` — the **display name only**:
```json
{
  "displayName": "New display name"
}
```

**Rules:**

- `displayName` is required, stored trimmed, and may be at most 50 characters
  (`400` otherwise, with the constraint message in the body).
- `userName` and `email` are declared in the DTO but **refused** with `400`: the
  email is the OAuth account-linking key (`findOrCreateOAuthUser` matches by it)
  and the email-whitelist key, so it is not self-service — it needs a verified
  flow or an administrator — and the username is the form-login identity. A
  request that carries either changes nothing.
- `409` when another account already uses that display name (`display_name` is
  unique).
- Properties outside the DTO are ignored, as elsewhere in this API.

**Response (200):** `UserDTO`

---

## 3. Admin

`/api/admin/**` — requires **ROLE_ADMIN**, twice over: the prefix is matched in
`SecurityConfig` and every endpoint repeats the rule with
`@PreAuthorize("hasRole('ROLE_ADMIN')")`.

---

### GET /api/admin/users — List every account

**Auth:** Yes (ROLE_ADMIN)  
**Status:** 200, 401, 403

**Response (200):** `[ UserDTO ]`

---

### PATCH /api/admin/users/{id}/role — Change an account's role

**Auth:** Yes (ROLE_ADMIN)  
**Status:** 200, 400, 401, 403, 404

**Request:**
```json
{
  "role": "USER"
}
```

**Response (200):** `UserDTO`

**Rules:**

- `400` — `You cannot change your own role.`
- `400` — `The last administrator cannot be demoted.` This branch is defence in
  depth: the caller is itself an administrator, so a demotion always leaves one
  behind, and the self-demotion rule above is the reachable protection. It stays
  for the day an authority is cached or taken from the JWT claim.
- `400` — the role is missing or unknown (the message names the allowed values).
- `404` — no such account.
- Authorities are rebuilt from the database on every request
  (`JwtAuthenticationFilter` → `CustomUserDetails`), so the new role applies on
  the account's next request — the account does not have to log in again.
- An **active** `email_whitelist` row for the same address is set to the same
  role, so a rebuilt database does not silently demote the account at its next
  login.

---

### GET /api/admin/whitelist — List the email whitelist

**Auth:** Yes (ROLE_ADMIN)  
**Status:** 200, 401, 403

**Response (200):**
```json
[
  {
    "id": 1,
    "email": "someone@example.com",
    "role": "USER",
    "active": true
  }
]
```

Revoked rows are included (`active: false`) so a decision can be revisited.

---

### POST /api/admin/whitelist — Admit an address

**Auth:** Yes (ROLE_ADMIN)  
**Status:** 201, 400, 401, 403

**Request:**
```json
{
  "email": "Someone@Example.com",
  "role": "USER"
}
```

Stored lower-cased. The `email` column is unique and removal is a soft delete, so
re-adding a revoked address **re-activates that row** instead of inserting a
second one.

**Response (201):** `WhitelistEntryDTO`

---

### PATCH /api/admin/whitelist/{id} — Re-role, revoke or re-admit

**Auth:** Yes (ROLE_ADMIN)  
**Status:** 200, 400, 401, 403, 404

**Request:** `{ "role": "ADMIN" }`, `{ "active": false }`, or both. A body with
neither is refused with `400` (*Nothing to update*).

**Response (200):** `WhitelistEntryDTO`

---

### DELETE /api/admin/whitelist/{id} — Revoke an address

**Auth:** Yes (ROLE_ADMIN)  
**Status:** 204, 401, 403, 404

Soft delete: the row remains with `active = false` but stops admitting logins.

---

### WhitelistEntryDTO

```json
{
  "id": 1,
  "email": "someone@example.com",
  "role": "USER",
  "active": true
}
```

---

### Whitelist role vs. account role

Two different switches, and the Admin UI says so:

- The whitelist is the **login gate**. `CustomOAuth2SuccessHandler` refuses an
  OAuth login whose address has no active row, and reads the row's `role` to
  decide what a *newly created* account is granted.
- An account that already exists keeps its own role —
  `UserService.findOrCreateOAuthUser` returns an email match untouched — so
  editing a whitelist role changes nothing for someone who has already logged in.
  That is what `PATCH /api/admin/users/{id}/role` is for.
- Addresses are matched case-insensitively on the login path and stored
  lower-cased on write (including `WhitelistLoader` seeding), so hand-typed casing
  cannot lock anyone out.

---

### GET /api/admin/logs — Read the audit trail

**Auth:** Yes (ROLE_ADMIN)  
**Status:** 200, 400, 401, 403

**Query parameters:**

| Name | Meaning |
|---|---|
| `type` | Only this event type. Case-insensitive; an unknown name is refused with `400` naming the valid ones. Omit for everything. |
| `beforeId` | Only rows older than this id. The cursor: pass the `id` of the last row of the page before. |
| `limit` | Rows per page. Clamped to `app.audit.max-page-size`; defaults to `app.audit.default-page-size`. |

**Response (200):** newest first

```json
[
  {
    "id": 41,
    "type": "ROLE_CHANGED",
    "occurredAt": "2026-10-26T19:04:11",
    "actorId": 6,
    "actorName": "admin",
    "targetId": 2,
    "targetName": "User2",
    "detail": "USER -> ADMIN",
    "clientIp": "203.0.113.7"
  }
]
```

**Rules:**

- Rows are ordered by `id` descending, which is also time descending: the table is
  append-only, so ids increase with the clock while two rows written in the same
  millisecond would leave a timestamp-ordered page ambiguous.
- There is **no endpoint that writes, edits or deletes a row.** The trail is
  append-only; `app.audit.retention-days` is the only thing that ever removes one
  (`AuditRetentionRunner`, once at startup).
- `actorId` / `targetId` are ids that may no longer exist — deliberately no
  foreign key, and `actorName` / `targetName` are copies taken at the time, so a
  rename or a deletion does not rewrite history.
- `clientIp` is the address `ClientIpResolver` documents (`CF-Connecting-IP`
  behind the tunnel), and `null` for a row written outside a request.

### AuditEventType

Every value the `type` field can hold:

| Type | Recorded when |
|---|---|
| `ACCOUNT_CREATED` | a first OAuth login created the account |
| `LOGIN_SUCCEEDED` | a correct password, or a completed OAuth handshake |
| `LOGIN_FAILED` | a wrong password, or a username that does not exist |
| `LOGIN_THROTTLED` | the login throttle answered `429` |
| `OAUTH_REFUSED` | a provider login was refused: unverified email, or an address not on the whitelist |
| `ROLE_CHANGED` | an administrator changed an account's role |
| `ROLE_CHANGE_REFUSED` | the last-administrator guard refused a demotion |
| `WHITELIST_ADDED` | an address was admitted, or a revoked row re-admitted |
| `WHITELIST_UPDATED` | a whitelist row was re-roled and/or re-activated |
| `WHITELIST_REVOKED` | an address was revoked (soft delete) |

Details worth knowing when reading the table:

- **`LOGIN_FAILED` says nothing about whether the account exists**, exactly like
  the `401` it accompanies: the endpoint must not answer that question, and the
  row keeps the same property (it names the username that was attempted).
- **A refused self-demotion leaves no row.** It is answered before the lookup,
  changes nothing, and the administrator who tried it is told. The
  last-administrator refusal *is* recorded, because that one says somebody is
  about to empty the administrator seat.
- **Only user- and administration-related events are recorded.** Campaigns,
  messages, characters, ships, journals and the planner are not: a scope decision,
  not an oversight (docs/ARCHITECTURE.md, "Audit trail").
- Every row is mirrored to the `audit` logger as one `key=value` line, so the same
  event is greppable in `docker logs` without reaching the database.

---

## 4. Campaigns

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
  "description": "A short overview for the Overview page",
  "worldLore": "World lore & background material",
  "privateDescription": "GM-only notes",
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
  "description": "A short overview for the Overview page",
  "worldLore": "World lore & background material",
  "privateDescription": "GM-only notes",
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
  "description": "A short overview for the Overview page",
  "worldLore": "World lore & background material",
  "privateDescription": "GM-only notes",
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
  "description": "New short overview",
  "worldLore": "New world lore & background",
  "privateDescription": "Updated GM notes"
}
```

**Response (200):** `CampaignResponseDTO`

---

### POST /api/campaigns/{id}/image — Upload campaign image

**Auth:** Yes  
**Status:** 200, 400, 401, 403, 413

**Request:** `multipart/form-data`, field name: `file`.

> Max file size: **5MB** per file (`app.upload.max-file-size`). The HTTP multipart limits (`spring.servlet.multipart.max-file-size` / `max-request-size`) are 8MB.

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

## 5. Player Characters

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
| `systemType` | enum | `DND5E` \| `OFFWORLDERS`. Optional: `null` (or omitted) creates a character with **no game system** — only the generic core fields are stored and neither nested block is attached |
| `notes` | string | Optional |
| `avatarUrl` | string | Optional |
| `appearance` | string | Optional; free-text appearance, generic to every system |
| `backstory` | string | Optional; free-text public backstory, visible to all campaign members (any system) |
| `privateBackstory` | string | Optional; free-text private backstory, returned only to the owner or the campaign GM. Owner-written (PATCH requires ownership) |
| `campaignId` | Long | Optional |
| `ownerId` | Long | Optional; defaults to logged-in user if null |
| `dnd5e` | object | Optional; applied only when `systemType === 'DND5E'`. Shape: `{ level (1–20), characterClass, race, hitPoints?, armorClass?, stats }` |
| `offworlders` | object | Optional; applied only when `systemType === 'OFFWORLDERS'`. Shape: `{ characterClass?, species?, xp?, health?, currentHealth?, healthModifier?, armor?, supply?, supplyMax?, credits?, stats?, skills?, abilities?, weapons?, items? }`. `stats = { strength, agility, intelligence, willpower }` (each −1…+3); `skills` / `abilities` are entry arrays `[ { name, description } ]` mixing canonical options with free text (canonical descriptions live in the frontend catalog; custom entries carry their own `description`); `health` is the maximum HP (client-derived from the attributes + `healthModifier`) and `currentHealth` the running total (may exceed `health` for temporary HP); `armor` is the single worn armor value 0–3 (0 None / 1 Light / 2 Heavy / 3 Assault); `characterClass` is optional (may be empty); `supplyMax` is fixed at `3`; `weapons` is a typed array `[ { type: 'Light'|'Medium'|'Heavy', description } ]` (damage and `heavy` are derived from `type`, not stored); `items` is a free-text array `[ { name, description } ]` |

> If `ownerId` is `null`, the logged-in user is automatically set as owner.

> `systemType` is optional. Leaving it out (or sending `null`) creates a system-less
> character — name, notes, avatarUrl, appearance, backstory and privateBackstory only,
> with no `dnd5e`/`offworlders` row. Nothing is defaulted to `DND5E`; the character stays
> generic until a system is chosen for it.


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
    "currentHealth": 15,
    "healthModifier": 0,
    "armor": 1,
    "supply": 2,
    "supplyMax": 3,
    "credits": 10,
    "stats": {
      "strength": 1, "agility": 3, "intelligence": 1, "willpower": 0
    },
    "skills": [
      { "name": "Pilot", "description": "" },
      { "name": "Sneak", "description": "" },
      { "name": "Tech", "description": "" }
    ],
    "abilities": [
      { "name": "Lucky", "description": "" },
      { "name": "Smuggle", "description": "" },
      { "name": "Shoot First", "description": "" }
    ],
    "weapons": [
      { "type": "Light", "description": "Snubnosed revolver" },
      { "type": "Medium", "description": "Hunting rifle" }
    ],
    "items": [
      { "name": "Band t-shirts", "description": "Rotating collection." }
    ]
  }
}
```

> Offworlders attributes range from −1 to +3 and Armor from 0 to 3. `health` is the maximum and is
> derived on the client as `max(1, 12 + strength + agility + healthModifier)`; `currentHealth` tracks
> the running HP and may exceed `health` to represent temporary HP. `armor` is a single value 0–3
> (0 None / 1 Light / 2 Heavy / 3 Assault); the displayed armor folds in the passive bonuses of the
> selected abilities (Hardy's +4 Health, Unstoppable's +1 armor) on top. `supplyMax` is always 3.
> Each weapon is `{ type: 'Light' | 'Medium' | 'Heavy', description }`; its damage (1D6 / 1D6+1 / 1D6+2)
> and the "heavy" flag follow from `type` and are not stored.
> `characterClass` is optional. `skills` and `abilities` are arrays of `{ name, description }` entries so
> players can combine the canonical catalogs with free-text custom entries (a custom entry carries its own
> `description`; canonical entries use an empty one, their text supplied by the frontend catalog).
> `credits`, `weapons` and `items` are optional.

> **Migration note:** Flyway `V8` rewrites pre-1.6 rows: the old `kind`-discriminated `items` array is
> split into `weapons` and free-text `items`, and any armor items are folded into the single `armor` value.

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
**Status:** 200, 400, 401, 403, 413

**Request:** `multipart/form-data`, field name: `file`.

> Max file size: **5MB** per file (`app.upload.max-file-size`). The HTTP multipart limits (`spring.servlet.multipart.max-file-size` / `max-request-size`) are 8MB.

**Response (200):** `CharacterOutputDTO` with updated `imageUrl`.

---

### POST /api/characters/{id}/images — Add an image to the character's gallery

**Auth:** Yes (ROLE_USER, must be owner)  
**Status:** 200, 400, 401, 403, 404, 413

**Request:** `multipart/form-data`, field name: `file`.

> Max file size: **5MB** per file (`app.upload.max-file-size`). The HTTP multipart limits (`spring.servlet.multipart.max-file-size` / `max-request-size`) are 8MB.

**Response (200):** `CharacterOutputDTO` with the image appended to `imageUrls`.

> The gallery sits beside the single `avatarUrl` portrait. In an Offworlders campaign the gallery images and `notes` are what the ship's crew card shows for this character — the crew is the campaign's characters, so nothing has to be saved on the ship.

---

### DELETE /api/characters/{id}/images?url={url} — Remove a gallery image

**Auth:** Yes (ROLE_USER, must be owner)  
**Status:** 200, 401, 403, 404

**Query params:** `url` (the stored image URL, e.g. `/images/character_14_gallery_ab12cd34.png`).

Removes the URL from `imageUrls`; the stored file itself is left on disk.

**Response (200):** `CharacterOutputDTO` with the image removed.

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

**Auth:** Yes (ROLE_USER; the character owner, or the campaign's GM/owner)  
**Status:** 200, 401, 403, 404

Sets `campaignId = null` without deleting the character.

**Response (200):** `CharacterOutputDTO`

---

### DELETE /api/characters/{characterId} — Delete character

**Auth:** Yes (ROLE_USER, must be owner)  
**Status:** 204, 401, 403, 404

---

## 6. Messages

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

## 7. Spells

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

## 6. Journals
`/api/journals/**` — the caller's own private journal in a campaign. **Auth:** Yes (ROLE_USER).

One journal per participant per campaign, a book of numbered markdown pages. Only
its owner can read or write it: another participant, the campaign owner and a GM
are all refused. A page id that belongs to somebody else answers 404 with the same
body as an unknown id, so another participant's writing is not discoverable; a
caller who is not a participant of the campaign gets 403.

The page after the last stored one (`pageCount + 1`) is a blank **virtual** page:
nothing is stored for it, so reading a journal creates no rows. Writing on it
creates a page; clearing a stored last page deletes it again (and any further
blank pages at the end), so no empty rows accumulate and a journal never has more
than one trailing blank page. The `journal` row itself appears with the first
write, so existing participants need no data fixing.

Limits: `app.journal.max-page-length` (default 10000 characters per page) and
`app.journal.max-pages` (default 500 stored pages per journal). Both are named in
the 400 they answer.

---

### GET /api/journals/campaign/{campaignId} — Get my journal

**Auth:** Yes (ROLE_USER, must be a campaign participant)  
**Status:** 200, 401, 403, 404

Pure read — nothing is created. A participant who has not written anything yet gets
an empty journal with no id.

**Response (200):**
```json
{
  "id": 4,
  "campaignId": 1,
  "userId": 7,
  "pageCount": 2,
  "pages": [
    {
      "id": 31,
      "pageNumber": 1,
      "content": "# Session 1\n\nWe met **Old Man Winters** at the docks.",
      "version": 0,
      "createdAt": "2026-05-29T10:00:00",
      "updatedAt": "2026-05-29T10:00:00"
    }
  ]
}
```

---

### POST /api/journals/campaign/{campaignId}/pages — Add a page

**Auth:** Yes (ROLE_USER, must be a campaign participant)  
**Status:** 201, 400, 401, 403, 404

**Request:**
```json
{ "afterPageId": 31, "content": "## Page after page 1" }
```

| Field | Type | Description |
|-------|------|-------------|
| `afterPageId` | Long or null | The page to insert after; `null` appends at the end of the book |
| `content` | string or null | Markdown source, max 10000 characters by default; may be blank, which keeps an empty page where it was inserted |

The book renumbers: inserted after page 3, the old page 4 becomes page 5. Appending
a blank page is a no-op (the trailing blank page is already virtual) and the journal
is returned unchanged.

Past `app.journal.max-pages` stored pages the answer is **400**.

**Response (201):** `JournalDTO` — the whole journal after the change.

---

### PATCH /api/journals/pages/{pageId} — Rewrite a page

**Auth:** Yes (ROLE_USER, must own the page)  
**Status:** 200, 400, 401, 403, 404, 409

**Request:**
```json
{ "content": "Rewritten", "version": 3 }
```

| Field | Type | Description |
|-------|------|-------------|
| `content` | string or null | Markdown source, max 10000 characters by default |
| `version` | Long | Required. The `version` of the page as it was loaded (optimistic locking); a missing one is rejected with **400** and a stale one with **409** |

Clearing the last stored page deletes it (and any further blank pages at the end);
a blank page in the middle stays as an empty page.

**Response (200):**
```json
{ "page": { "id": 32, "pageNumber": 2, "content": "Rewritten", "version": 4, "createdAt": "2026-05-29T10:05:00", "updatedAt": "2026-05-29T10:20:00" }, "pageCount": 2 }
```

`page` is `null` when writing the page removed it.

---

### DELETE /api/journals/pages/{pageId} — Delete a page

**Auth:** Yes (ROLE_USER, must own the page)  
**Status:** 200, 401, 403, 404

Deletes the page; the pages after it close the gap and keep their order.

**Response (200):** `JournalDTO` — the whole journal after the change.

---

## 8. Campaign add-ons

`/api/campaigns/{id}/addons` — the optional capabilities a campaign has switched
on. **Auth:** Yes (ROLE_USER). Reading which ones are on needs nothing extra
(every campaign answer already carries `addons`); switching one on or off is
**owner only**, on the ownership axis beside roster and roles.

The row's presence *is* the state, so both writes are idempotent — enabling what is
on, or disabling what is off, changes nothing and is not an error. An unknown key is
refused (400): the backend's `AddonKey` enum is the registry. Switching an add-on
off leaves its data alone, so switching it back on returns it.

Guest Demo has no backend at all and never reaches these endpoints; the `demo`
profile seeds the planner as already added in `demo-data.sql`, so the switch can be
exercised there.

### GET /api/campaigns/{id}/addons — which add-ons are on

- **Auth:** ROLE_USER, and the caller must be able to see the campaign (participant or owner) — otherwise 403
- **Response:** `["planner"]` — the enabled keys as the API spells them; `[]` when the campaign has none
- **Status Codes:** 200, 401, 403, 404

### POST /api/campaigns/{id}/addons/{addonKey} — switch one on

- **Auth:** ROLE_USER, campaign **owner** only — a participant who is not the owner gets 403
- **Path:** `addonKey` is case-insensitive (`planner`)
- **Response:** `CampaignResponseDTO` — the whole campaign, so a client rebuilds its navigation from one answer
- **Status Codes:** 200, 400 (unknown key), 401, 403, 404

### DELETE /api/campaigns/{id}/addons/{addonKey} — switch one off

- **Auth:** ROLE_USER, campaign **owner** only
- **Response:** `CampaignResponseDTO`
- **Status Codes:** 200, 400, 401, 403, 404

---

## 9. Planner

`/api/planner/**` — the planner, which is the first add-on. **Auth:** Yes
(ROLE_USER), and the add-on must be enabled for the campaign: while it is off, every
planner endpoint answers 400 (the campaign has no such feature right now).

Any **participant** may do almost everything here — start a plan, offer dates,
answer, decide, reopen — because finding a date is not an administrative act. Three
things are restricted:

| Action | Who |
|---|---|
| Remove **somebody else** from a plan's roster | the campaign **owner** |
| Delete a plan | its **creator** or the campaign **owner** |
| Correct or withdraw a date | the member who offered it, the plan's creator, or the campaign owner |

Every mutation answers with the whole `PlannerPlanDTO`, so a client never has to
guess what a change did to the rest of the plan. `version` on the plan and on each
date is an optimistic-concurrency token: a stale one is refused with 409 rather than
overwriting somebody else's change.

**The roster is implicit.** It is the campaign's participants minus the plan's
exclusion rows, so everybody is in a plan the moment it exists and a new participant
joins automatically. A member leaving (or being removed) creates one exclusion row;
their answers are kept, so rejoining restores them.

**An unanswered date counts against it.** A member with no answer has no attendance
row at all, and a date's share is *yes answers ÷ roster size* — which is what makes
the green marker mean "everybody still in the plan can come". The colour itself is
computed on the client (`plannerUtils.js`); the API sends the answers and the roster.

**A decided plan stops moving.** `DECIDED` refuses new dates, answers and roster
changes until somebody reopens it (`OPEN`, which clears the chosen date).

Limits, each named in the 400 it produces: `app.planner.max-title-length` (200),
`app.planner.max-description-length` (10000), `app.planner.max-note-length` (500),
`app.planner.max-plans-per-campaign` (100), `app.planner.max-dates-per-plan` (50).

### GET /api/planner/campaign/{campaignId}/plans — a campaign's plans

- **Auth:** ROLE_USER + participant + the add-on enabled
- **Response:** `[ PlannerPlanDTO ]` — open plans first, then the decided ones, newest first
- **Status Codes:** 200, 400 (add-on off), 401, 403, 404

### POST /api/planner/campaign/{campaignId}/plans — start a plan

- **Request:** `PlannerPlanCreateDTO { title, description }` — the title is required, trimmed, at most 200 characters
- **Response:** `PlannerPlanDTO`. Status Codes: 201, 400, 401, 403, 404

### GET /api/planner/plans/{planId} — one plan, whole

- **Response:** `PlannerPlanDTO` with its dates, everybody's answers and the roster
- **Status Codes:** 200, 400, 401, 403, 404

### PATCH /api/planner/plans/{planId} — the title and description

- **Request:** `PlannerPlanUpdateDTO { title, description, version }` — `version` required (stale → 409)
- **Response:** `PlannerPlanDTO`. Status Codes: 200, 400, 401, 403, 404, 409

### PATCH /api/planner/plans/{planId}/status — decide or reopen

- **Request:** `PlannerStatusUpdateDTO { status, decidedDateId, version }`
- `DECIDED` requires `decidedDateId` to name one of the plan's own dates, and is only possible while the plan is open (400 otherwise); `OPEN` clears the chosen date
- **Response:** `PlannerPlanDTO`. Status Codes: 200, 400, 401, 403, 404, 409

### DELETE /api/planner/plans/{planId} — delete a plan

- **Auth:** its creator or the campaign owner — otherwise 403
- **Response:** 204. Its dates and their answers go with it. Status Codes: 204, 400, 401, 403, 404

### POST /api/planner/plans/{planId}/dates — offer a date

- **Request:** `PlannerDateCreateDTO { date, startTime, endTime, note }` — the day is required; `endTime` without `startTime` is refused, and so is an end that is not after the start
- **Response:** `PlannerPlanDTO`. Status Codes: 201, 400, 401, 403, 404, 409 (that day is already offered)

### PATCH /api/planner/dates/{dateId} — correct a date

- **Request:** `PlannerDateUpdateDTO { date, startTime, endTime, note, version }`
- **Response:** `PlannerPlanDTO`. Status Codes: 200, 400, 401, 403, 404, 409

### DELETE /api/planner/dates/{dateId} — withdraw a date

- **Response:** `PlannerPlanDTO` — a plan that had decided on this date keeps its decision but loses the date
- **Status Codes:** 200, 400, 401, 403, 404

### PUT /api/planner/dates/{dateId}/attendance — answer a date

- **Request:** `PlannerAttendanceUpdateDTO { state }` — `YES`, `NO` or `MAYBE` (case-insensitive)
- One answer per member per date: a second call changes it in place. A member who left the plan gets 403
- **Response:** `PlannerPlanDTO`. Status Codes: 200, 400, 401, 403, 404

### DELETE /api/planner/dates/{dateId}/attendance — withdraw an answer

- **Response:** `PlannerPlanDTO` — the member is back to "not answered"
- **Status Codes:** 200, 400, 401, 403, 404

### PUT /api/planner/plans/{planId}/members/me — rejoin

- Refused with 403 when the campaign owner is the one who removed the caller; otherwise their exclusion row is cleared
- **Response:** `PlannerPlanDTO`. Status Codes: 200, 400, 401, 403, 404

### DELETE /api/planner/plans/{planId}/members/me — leave

- Creates the caller's exclusion row; their answers stay
- **Response:** `PlannerPlanDTO`. Status Codes: 200, 400, 401, 403, 404

### DELETE /api/planner/plans/{planId}/members/{memberId} — remove another member

- **Auth:** campaign **owner** only
- Removing themselves is a member leaving, so it stays undoable by them
- **Response:** `PlannerPlanDTO`. Status Codes: 200, 400, 401, 403, 404

### PUT /api/planner/plans/{planId}/members/{memberId} — put another member back

- **Auth:** campaign **owner** only — the other half of their removal, and the only way back for somebody the owner took out (that member's own `/members/me` rejoin is refused, see above)
- Idempotent, like leaving and rejoining: a member who is not out is left exactly as they are, not refused
- Their answers were never touched, so they count towards the dates again at once
- **Response:** `PlannerPlanDTO`. Status Codes: 200, 400, 401, 403, 404

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
  ],
  "addons": ["planner"]
}
```

`addons` lists the optional capabilities the campaign's owner has switched on, by
their API keys. Always present, `[]` when the campaign has none — it is what the
client's navigation filters its sections on (see section 8).

### PlannerPlanDTO
```json
{
  "id": 4,
  "campaignId": 1,
  "title": "Session 12 — when do we play next?",
  "description": "Pick a day and I will book the table.",
  "status": "OPEN",
  "decidedDateId": null,
  "createdByUserId": 7,
  "version": 3,
  "createdAt": "2026-11-01T19:02:11",
  "updatedAt": "2026-11-04T08:15:00",
  "dates": [
    {
      "id": 9,
      "date": "2026-11-07",
      "startTime": "18:00:00",
      "endTime": "22:00:00",
      "note": "Saturday evening at my place.",
      "createdByUserId": 7,
      "version": 0,
      "attendances": [
        { "userId": 7, "state": "YES" },
        { "userId": 8, "state": "MAYBE" }
      ]
    }
  ],
  "members": [
    { "userId": 7, "nickname": "The Hero", "role": "GM", "excluded": false, "removedByOwner": false },
    { "userId": 9, "nickname": "The Rogue", "role": "PLAYER", "excluded": true, "removedByOwner": false }
  ]
}
```

Two deliberate omissions. There is no counts or percentage field: a client derives
them from `attendances` and `members` (a member with no entry has not answered), so
the threshold exists in exactly one place. And `members` lists **every** campaign
participant — those who left are flagged instead of removed, so a client can show
"left this plan" with a way back in.

The requests are small records:
`PlannerPlanCreateDTO { title, description }`,
`PlannerPlanUpdateDTO { title, description, version }`,
`PlannerStatusUpdateDTO { status, decidedDateId, version }`,
`PlannerDateCreateDTO { date, startTime, endTime, note }`,
`PlannerDateUpdateDTO { date, startTime, endTime, note, version }` and
`PlannerAttendanceUpdateDTO { state }`.

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

### JournalDTO
```json
{
  "id": 4,
  "campaignId": 1,
  "userId": 7,
  "pageCount": 2,
  "pages": [
    {
      "id": 31,
      "pageNumber": 1,
      "content": "# Session 1\n\nWe met **Old Man Winters** at the docks.",
      "version": 0,
      "createdAt": "2026-05-29T10:00:00",
      "updatedAt": "2026-05-29T10:00:00"
    }
  ]
}
```

A participant who has not written anything yet — the read creates nothing:

```json
{ "id": null, "campaignId": 1, "userId": 7, "pageCount": 0, "pages": [] }
```

### JournalPageSaveDTO
```json
{
  "page": {
    "id": 32,
    "pageNumber": 2,
    "content": "Rewritten",
    "version": 4,
    "createdAt": "2026-05-29T10:05:00",
    "updatedAt": "2026-05-29T10:20:00"
  },
  "pageCount": 2
}
```

`page` is `null` when the page was blank and last, so writing it removed it
(together with any further blank pages at the end).

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
