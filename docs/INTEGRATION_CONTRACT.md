# Integration Contract — Frontend ↔ Backend

This document is a **verification checklist** used at release to confirm the frontend and backend are aligned.
The canonical API reference is `API_REFERENCE.md` — generated from source code.

## Core Principles

- **Backward compatibility:** Never break existing endpoints without frontend approval
- **Explicit responses:** All endpoints return DTOs (never raw entities)
- **Error consistency:** Use standard HTTP status codes + custom exception types
- **Authentication:** All `/api/**` endpoints require JWT (except `/api/auth/login` and `/api/auth/oauth-init`)
- **Timestamps:** every `createdAt` / `updatedAt` is a **UTC** wall clock in ISO-8601 local form
  (no offset, e.g. `2026-05-29T09:00:00`), written by JPA auditing (`JpaConfig`). A client that
  renders it in the reader's zone has to treat the value as UTC — `new Date("2026-05-29T09:00:00")`
  parses it as *local*, so append `Z` (or parse the parts explicitly) before formatting.

---

## Authentication

**POST /api/auth/login**
- Request: `{ username, password }`
- Response body: `AuthDTO { id, username, email, role }`
- JWT returned in `Authorization` response header: `Bearer <token>` — **not** in the body
- Status: 200, 400, 401, 429
- Throttled on two axes — the client address and the username (`LoginAttemptLimiter`) — because a guess costs a BCrypt comparison and a distributed attack on one account is invisible to a proxy. Over budget the answer is `429` with `Retry-After` in seconds, and a body that says nothing about whether the account exists. The frontend must treat 429 as "wait and retry", never as a logout: `src/api/lib/axios.js` has a branch for it, and nginx's own `limit_req` on the same path answers 429 too.

**GET /api/auth/oauth-init**
- Query params: `provider` (google|github), `origin`
- Redirects to OAuth2 provider; sets HttpOnly `oauth_redirect_origin` cookie
- Disabled in `demo` profile
- Account linking: a login is matched by provider identity (`providerType` + `providerId`), then by email (case-insensitive) — an existing account with the same email is reused, no duplicate user is created
- Google requires a verified email (`email_verified = true`); a rejected login (unverified or non-whitelisted email) redirects to `{FRONTEND_URL}/under-construction`, not `/oauth-redirect`
- Status: 302, 400, 429 (the handshake start is throttled per client address)

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
- Request: `UserProfileUpdateDTO { displayName }` — the display name only; trimmed, max 50 chars
- Refused with 400: `userName` and `email`. The email is the OAuth account-linking
  key (`findOrCreateOAuthUser` matches by it) and the whitelist key, so it is not
  self-service; the username is the form-login identity. A request carrying either
  changes nothing.
- Response: `UserDTO`
- Status: 200, 400, 401, 403, 404, 409 (display name already in use)

---

## Admin

Every endpoint below requires **ROLE_ADMIN**, twice over: the whole `/api/admin/**`
prefix is matched in `SecurityConfig`, and each controller repeats the rule with
`@PreAuthorize("hasRole('ROLE_ADMIN')")`.

**GET /api/admin/users**
- Response: `[ UserDTO ]` — every account
- Status: 200, 401, 403

**PATCH /api/admin/users/{id}/role**
- Request: `{ role: "USER" | "ADMIN" }`
- Response: `UserDTO`
- Status: 200, 400, 401, 403, 404
- Rules: 400 when the caller targets themselves ("You cannot change your own role.")
  or when the target is the last administrator ("The last administrator cannot be
  demoted."). Authorities are rebuilt from the database on every request
  (`JwtAuthenticationFilter`), so the new role is effective on the account's next
  request — no re-login needed. An **active** whitelist row for the same email is
  set to the same role, so a rebuilt database does not silently demote the account
  at its next login.

**GET /api/admin/whitelist**
- Response: `[ WhitelistEntryDTO { id, email, role, active } ]` — includes revoked rows
- Status: 200, 401, 403

**POST /api/admin/whitelist**
- Request: `{ email, role }` — the address is stored lower-cased
- Response: `WhitelistEntryDTO`
- Status: 201, 400, 401, 403
- Rules: re-adding an address whose row was revoked **re-activates that row**; the
  `email` column is UNIQUE and removal is a soft delete, so an insert would fail.

**PATCH /api/admin/whitelist/{id}**
- Request: `{ role?, active? }` — a null field is left unchanged; 400 when neither is given
- Response: `WhitelistEntryDTO`
- Status: 200, 400, 401, 403, 404

**DELETE /api/admin/whitelist/{id}**
- Soft delete: the row remains with `active = false`, so a decision can be revisited
- Status: 204, 401, 403, 404

**Whitelist semantics — two different switches**

- The whitelist is the **login gate**: an OAuth login whose email has no active row
  is refused (`CustomOAuth2SuccessHandler`, redirecting to `/under-construction`).
- Its `role` only decides what a **first** login is granted. An existing account is
  returned untouched by `findOrCreateOAuthUser`, so changing a whitelist role has no
  effect on an account that already exists — that is what `PATCH /api/admin/users/{id}/role`
  is for. The Admin UI states this rather than pretending one control does both.
- Lookups on the login path are case-insensitive, and addresses are stored lower-cased
  on write (whitelist seeding included), so hand-typed casing cannot lock anyone out.

---

## Characters

**POST /api/characters**
- Requires: ROLE_USER
- Request: `CharacterInputDTO { ownerId?, campaignId?, name, systemType, notes?, avatarUrl?, appearance?, backstory?, privateBackstory?, dnd5e?, offworlders? }`
  - `systemType`: `DND5E` | `OFFWORLDERS`, optional — `null`/omitted creates a character with **no game system**: only the generic core fields (name, notes, avatarUrl, appearance, backstory, privateBackstory) exist and neither nested block is stored
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
- Response: `CharacterOutputDTO { id, ownerId, campaignId, name, systemType, notes, avatarUrl, imageUrls, appearance, backstory, privateBackstory, dnd5e, offworlders, createdAt, updatedAt }`
  - `imageUrls` is the character's gallery (empty, never null); the portrait is the separate `avatarUrl`
  - `privateBackstory` is `null` unless the requester is the character's owner or the campaign GM
  - `systemType` is `null` when the character was created without a game system (see POST) — nothing is defaulted in its place
  - `dnd5e` is a nullable nested block `{ level, characterClass, race, hitPoints, armorClass, stats }`, present only when `systemType === 'DND5E'`
  - `offworlders` is a nullable nested block `{ characterClass, species, xp, health, currentHealth, healthModifier, armor, supply, supplyMax, credits, stats, skills, abilities, weapons, items }`, present only when `systemType === 'OFFWORLDERS'`
  - both blocks are `null` on a system-less character
- Status: 200, 401, 404

**PATCH /api/characters/{id}**
- Requires: ROLE_USER, must be owner
- Request: `CharacterInputDTO` — partial, null-safe patch. Omitted/null fields are left unchanged, `systemType` included (a null never clears or re-defaults the stored system). The nested `dnd5e` object is patch-applied field-by-field when `systemType === 'DND5E'`; likewise the nested `offworlders` object when `systemType === 'OFFWORLDERS'`.
- Response: `CharacterOutputDTO`
- Status: 200, 400, 401, 403, 404

**POST /api/characters/{id}/image**
- Requires: ROLE_USER, must be owner
- Request: `multipart/form-data`, field: `file`
- Max file size: 5MB per file (configurable via `app.upload.max-file-size`; multipart limits are 8MB)
- Response: `CharacterOutputDTO` (with updated avatarUrl)
- Status: 200, 400, 401, 403

**POST /api/characters/{id}/images**
- Requires: ROLE_USER, must be owner
- Request: `multipart/form-data`, field: `file`
- Max file size: 5MB per file (configurable via `app.upload.max-file-size`; multipart limits are 8MB)
- Appends the stored image to the character's gallery (`imageUrls`), alongside the single `avatarUrl` portrait
- In an Offworlders campaign the gallery images and `notes` are what the ship's crew card shows for this character — the crew is the campaign's characters, so there is no separate crew entry to keep in step
- Response: `CharacterOutputDTO` (with the image appended to `imageUrls`)
- Status: 200, 400, 401, 403, 404

**DELETE /api/characters/{id}/images?url={url}**
- Requires: ROLE_USER, must be owner
- Removes the matching URL from the character's gallery; the stored file itself is left in place
- Response: `CharacterOutputDTO` (with the image removed)
- Status: 200, 401, 403, 404

**PATCH /api/characters/{id}/campaign**
- Requires: ROLE_USER, must be owner
- Request: `{ campaignId: Long }`
- Response: `CharacterOutputDTO`
- Status: 200, 400, 401, 403, 404

**DELETE /api/characters/{id}/campaign**
- Requires: ROLE_USER; the character's owner, or the campaign's GM/owner
- Sets campaignId = null without deleting the character
- Response: `CharacterOutputDTO`
- Status: 200, 401, 403, 404

**DELETE /api/characters/{id}**
- Requires: ROLE_USER, must be owner
- Status: 204, 401, 403, 404

---

## Campaigns

**POST /api/campaigns**
- Request: `CampaignCreationDTO { name, description, worldLore?, privateDescription?, ownerId?, participants?, primarySystem? }`
- `ownerId` defaults to logged-in user if null
- `description`: short overview, rendered on the campaign's Overview page
- `worldLore`: World Lore & Background material (public — returned to every participant)
- `privateDescription`: GM-only notes (returned to the owner/GM only; labelled "GM notes" in the UI)
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
- Response: `CampaignResponseDTO { id, name, description, worldLore, privateDescription, imageUrl, primarySystem, ownerId, participants: [ { id, nickname, role } ] }`
  - `privateDescription` is `null` unless the requester is a campaign GM (a non-GM owner does not receive it); `worldLore` is always included
- Status: 200, 401, 403, 404

**PUT /api/campaigns/{id}**
- Requires: campaign GM
- Request: `CampaignUpdateDTO { name, description, worldLore, privateDescription, primarySystem }` (`primarySystem`: `DND5E` | `OFFWORLDERS` | null)
- Setting `primarySystem` to `OFFWORLDERS` creates the campaign's default ship the first time (idempotent). Changing away from `OFFWORLDERS` keeps the ship.
- Response: `CampaignResponseDTO`
- Status: 200, 400, 401, 403, 404

**POST /api/campaigns/{id}/image**
- Requires: campaign GM
- Request: `multipart/form-data`, field: `file`
- Max file size: 5MB per file (configurable via `app.upload.max-file-size`; multipart limits are 8MB)
- Response: `CampaignResponseDTO` (with updated imageUrl)
- Status: 200, 400, 401, 403

**PATCH /api/campaigns/{id}/participants**
- Requires: campaign owner
- Request: `UpdateParticipantsDTO { participantsToAdd: [ ParticipantResponseDTO ], participantIdsToRemove: [ Long ] }` (`nickname` optional, defaults server-side)
- Response: `CampaignResponseDTO`
- Status: 200, 400, 401, 403, 404

**PATCH /api/campaigns/{id}/participants/{participantId}/nickname**
- Requires: the participant themselves, or the campaign owner to rename someone else
- Request body: plain string
- Response: `CampaignResponseDTO`
- Status: 200, 401, 403, 404

**PATCH /api/campaigns/{id}/participants/{participantId}/role**
- Requires: campaign owner
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

**Campaign permission model**
- Two independent axes: **ownership** (administration) and **table role** (GM = world-building).
- Content operations — `PUT /api/campaigns/{id}`, `POST /api/campaigns/{id}/image`, world lore and GM notes — require a campaign **GM**.
- Administration — `PATCH /participants`, `/participants/{id}/nickname` (for another participant), `/participants/{id}/role`, `/owner`, and `DELETE /api/campaigns/{id}` — requires the campaign **owner**.
- The campaign creator is seeded as a **GM** participant, so owner-as-GM works out of the box; a non-GM owner is not treated as a GM.
- Enforced in `CampaignService` via `CampaignPermissionService.isGameMaster` / `isCampaignOwner`; the frontend mirrors the same rules (`src/features/campaign/campaignPermissions.js`).

---

## Campaign Ship (Offworlders)

A ship is `1:1` with a campaign (unique `campaign_id` FK) and only meaningful when the
campaign's `primarySystem` is `OFFWORLDERS`. Defaults mirror the Offworlders rulebook
(p.13): 15 Hull, 0 Armor, 1D6 Damage, 4 Max Drive Fuel, 0 Credits, no upgrades. Every
campaign participant may view and edit; there is **no delete**.

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
- Max file size: 5MB per file (configurable via `app.upload.max-file-size`; multipart limits are 8MB)
- Replaces the ship's profile image (`imageUrl`); the UI falls back to `/defaultShip.svg`
- Response: `ShipDTO`
- Status: 200, 400, 401, 403, 404

**POST /api/campaigns/{id}/ship/images**
- Requires: campaign participant
- Request: multipart/form-data with field `file`
- Max file size: 5MB per file (configurable via `app.upload.max-file-size`; multipart limits are 8MB)
- Appends an image to the ship's gallery (`imageUrls`)
- Response: `ShipDTO`
- Status: 200, 400, 401, 403, 404

**DELETE /api/campaigns/{id}/ship/images?url={url}**
- Requires: campaign participant
- Removes the matching image from the ship's gallery (`imageUrls`)
- Response: `ShipDTO`
- Status: 200, 401, 403, 404

### The crew (no endpoint of its own)

In an Offworlders campaign the ship view also shows a crew manifest, but it is not
a resource of the ship: **the crew is the campaign's characters.** Each card is
one character — its name, the participant who plays it (`ownerId` matched against
the campaign's participants), the character's `notes` and its `imageUrls` gallery
— read from `GET /api/characters?campaignId={id}`, editable from the character
(`PATCH /api/characters/{id}`, `POST`/`DELETE /api/characters/{id}/images`) by its
owner.

There is therefore no crew to save, no `/me` entry and no crew permissions: the
ship's own surface is the ship (`PUT /api/campaigns/{id}/ship` and its images),
and everything about a crew member belongs to the character.

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

## Journals

Each campaign participant has one private journal: a book of numbered markdown
pages.

**Only its owner can read or write it.** Another participant, the campaign owner
and a GM are all refused. That is enforced in the backend on every call
(`JournalService`) — no endpoint takes another user's journal id, and a page id
that belongs to somebody else answers with exactly the same 404 as an unknown id,
so nothing about another participant's writing is even discoverable. A caller who
is not a participant of the campaign gets 403.

A journal always shows one blank page after the last stored one
(`lastPageNumber = pageCount + 1`). That trailing page is **virtual**: nothing is
stored for it, so opening a journal creates no rows. Writing on it creates a page;
clearing a stored last page deletes it again (together with any further blank
pages at the end — a blank page in the middle is kept), so a journal never has
more than one trailing blank page.

The `journal` row itself is created by the first write, never by a read, so
existing participants need no data fixing.

**GET /api/journals/campaign/{campaignId}**
- Requires: campaign participant (the caller's own journal only)
- Response: `JournalDTO { id, campaignId, userId, pageCount, pages }`
- Pure read — nothing is created. Nothing written yet answers
  `{ "id": null, "campaignId": …, "userId": …, "pageCount": 0, "pages": [] }`
- Status: 200, 401, 403, 404

**POST /api/journals/campaign/{campaignId}/pages**
- Requires: campaign participant
- Request: `{ afterPageId, content }`
  - `afterPageId`: Long or `null` — `null` appends the page at the end
  - `content`: string (max `app.journal.max-page-length`, default 10000 characters); may be blank, which keeps an empty page where it was inserted
- The page is inserted and the book renumbers: inserted after page 3, the old page 4 becomes page 5, and so on
- Appending a **blank** page is a no-op — the trailing blank page is already virtual — and the journal is returned unchanged
- Response: `JournalDTO` (the whole journal after the change)
- Status: 201, 400, 401, 403, 404
- Note: past `app.journal.max-pages` (default 500) stored pages the answer is 400

**PATCH /api/journals/pages/{pageId}**
- Requires: the page's owner (a campaign participant)
- Request: `{ content, version }`
  - `content`: string (max `app.journal.max-page-length`, default 10000 characters)
  - `version`: the `version` of the page as it was loaded (optimistic locking; required — a missing one is 400, a stale one 409)
- Clearing the last stored page deletes it (and any further blank pages at the end); a blank page in the middle stays as an empty page
- Response: `JournalPageSaveDTO { page, pageCount }` — `page` is the saved `JournalPageDTO`, or `null` when writing the page removed it
- Status: 200, 400, 401, 403, 404, 409
- Note: a page that belongs to another user answers 404 (same body as an unknown id); a stale `version` answers 409

**DELETE /api/journals/pages/{pageId}**
- Requires: the page's owner (a campaign participant)
- Deletes the page; the pages after it close the gap and keep their order
- Response: `JournalDTO` (the whole journal after the change)
- Status: 200, 401, 403, 404

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

Every error body is an RFC 9457 `ProblemDetail`, served as
`application/problem+json`. The sentence to show the user is **`detail`**; there
is no `message` member. A client should read `detail` and, when present, `field`.

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Character not found with id: 999",
  "instance": "/api/characters/999"
}
```

A validation failure names the offending input in an extension property:

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

A unique-constraint conflict carries `field` and `value` the same way. The same
shape comes back for framework failures (malformed JSON, wrong parameter type,
unknown path, upload past the multipart limit) and from the security filter chain
for its 401/403, so one parser covers every status.

**Common status codes:**
- `200` — Success
- `201` — Created
- `204` — No Content (successful DELETE)
- `400` — Bad Request (validation error, malformed body or parameter)
- `401` — Unauthorized (missing/invalid JWT)
- `403` — Forbidden (not owner/insufficient permissions)
- `404` — Not Found
- `409` — Conflict (unique constraint violation, concurrent update)
- `413` — Content Too Large (upload past the 5MB application limit or the 8MB multipart limit)
- `429` — Too Many Requests (login throttle; carries `Retry-After`)
- `500` — Server Error (a generic `detail`; the cause is logged, not returned)

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
  "imageUrls": ["/images/character_14_gallery_ab12cd34.png"],
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
  "description": "Short overview, shown on the Overview page.",
  "worldLore": "World lore & background, shown to every participant.",
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
  "credits": 0,
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
    },
    {
      "id": 32,
      "pageNumber": 2,
      "content": "",
      "version": 3,
      "createdAt": "2026-05-29T10:05:00",
      "updatedAt": "2026-05-29T10:12:00"
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
(together with any further blank pages at the end); `pageCount` then describes the
smaller book.

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

## Campaign add-ons

Optional capabilities a campaign owner switches on. The first one is the **planner**.
A campaign carries the enabled keys in every answer (`CampaignResponseDTO.addons`),
which is what a client filters its navigation on.

**GET /api/campaigns/{id}/addons**
- Requires: ROLE_USER, and the campaign must be visible to the caller (participant or owner)
- Response: `["planner"]` — the enabled keys as the API spells them; `[]` when there are none
- Status: 200, 401, 403, 404

**POST /api/campaigns/{id}/addons/{addonKey}** / **DELETE /api/campaigns/{id}/addons/{addonKey}**
- Requires: ROLE_USER, campaign **owner** only — a non-owner participant gets 403
- `addonKey` is case-insensitive. Both are idempotent (enabling what is on, disabling what is off, is a no-op), an unknown key answers 400, and the add-on's own data is kept, so switching one back on returns it
- Response: `CampaignResponseDTO`
- Status: 200, 400, 401, 403, 404

**Guest Demo** has no backend at all and never reaches these endpoints: a demo
campaign carries its add-ons in `demoData.js`, and the Add-ons block in Campaign
Settings renders read-only for a guest session. The **`demo` profile** seeds the
planner as already added (`demo-data.sql`), which is where the switch is exercised
end to end.

---

## Planner

The first add-on. `DECIDED` versus `OPEN`, the implicit roster and what an
unanswered date means are described in `API_REFERENCE.md` (section 9); this is the
checklist.

| Endpoint | Who |
|---|---|
| `GET /api/planner/campaign/{campaignId}/plans` | participant (add-on on) |
| `POST /api/planner/campaign/{campaignId}/plans` | any participant |
| `GET /api/planner/plans/{planId}` | participant |
| `PATCH /api/planner/plans/{planId}` | any participant, while open |
| `PATCH /api/planner/plans/{planId}/status` | any participant (decide requires one of the plan's own dates) |
| `DELETE /api/planner/plans/{planId}` | the plan's creator or the campaign owner, else 403 |
| `POST /api/planner/plans/{planId}/dates` | any participant, while open |
| `PATCH`/`DELETE /api/planner/dates/{dateId}` | the member who offered it, the plan's creator, or the owner |
| `PUT`/`DELETE /api/planner/dates/{dateId}/attendance` | any member of the roster |
| `PUT`/`DELETE /api/planner/plans/{planId}/members/me` | the caller's own seat (rejoin is refused after an owner removal) |
| `DELETE /api/planner/plans/{planId}/members/{memberId}` | the campaign owner |
| `PUT /api/planner/plans/{planId}/members/{memberId}` | the campaign owner — the way back after their removal |

- Every mutation answers with the whole plan; a stale plan or date `version` is 409
- The add-on gates all of it: while the planner is switched off, every endpoint answers 400
- The same day cannot be offered twice in one plan (409), and a decided plan refuses dates, answers and roster changes until it is reopened (400)
- Limits: `app.planner.max-title-length`, `-description-length`, `-note-length`, `-plans-per-campaign`, `-dates-per-plan` (see the backend README)

---

## Notes for Teams

**Frontend:** This contract defines what you can expect. If backend deviates, flag it immediately.

**Backend:** This contract is binding. Changes require frontend approval and must be backward compatible or coordinated with a versioned API change.

**Both:** Use this document during code review to ensure compliance. Consider mirroring this contract as a runnable Bruno collection so drift between this document and the real endpoints is caught automatically instead of found during manual review.