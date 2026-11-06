-- The character gains a list of small, individually titled notes ("sections")
-- beside its free-text `notes`.
--
-- They belong to the character, not to a campaign: they follow it across
-- campaigns, exports and imports exactly like `notes`, `backstory` and
-- `private_backstory`, which is why the column lives here rather than on a
-- campaign-scoped table.
--
-- `notes` is deliberately untouched: it stays a separate markdown field, it is
-- not migrated into this list, and existing rows keep working exactly as they
-- did. The new column is additive and nullable, so a character written before
-- this migration reads back as "no sections" (the converter maps NULL to an
-- empty list) rather than failing to load.
--
-- The value is a JSON array of `{ id, title, body }` objects, written by
-- `SectionListConverter` — the same TEXT-column pattern as `image_urls` and the
-- ship's `sections`. The array order is the order the owner arranged the
-- sections in; there is no ordering column, and nothing reorders the array on
-- the way in or out.
--
-- MySQL 8.4 syntax only — the `demo` profile builds the schema from the entity
-- instead, so nothing here runs there. See docs/MIGRATIONS.md for the numbering
-- rules and docs/INTEGRATION_CONTRACT.md (section "Characters") for the
-- contract.

ALTER TABLE `game_character`
  ADD COLUMN `sections` text DEFAULT NULL AFTER `notes`;
