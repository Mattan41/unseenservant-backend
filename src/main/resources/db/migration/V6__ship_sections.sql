-- The ship gains a list of small, individually titled notes ("sections") beside
-- its free-text `notes`.
--
-- `notes` is deliberately untouched: it stays a separate markdown field, it is
-- not migrated into this list, and existing rows keep working exactly as they
-- did. The new column is additive and nullable, so a ship written before this
-- migration reads back as "no sections" (the converter maps NULL to an empty
-- list) rather than failing to load.
--
-- The value is a JSON array of `{ id, title, body }` objects, written by
-- `SectionListConverter` — the same TEXT-column pattern as `upgrades` and
-- `image_urls`. The array order is the order the crew arranged the sections in;
-- there is no ordering column, and nothing reorders the array on the way in or
-- out.
--
-- MySQL 8.4 syntax only — the `demo` profile builds the schema from the entity
-- instead, so nothing here runs there. See docs/MIGRATIONS.md for the numbering
-- rules and docs/INTEGRATION_CONTRACT.md (section "Campaign Ship") for the
-- contract.

ALTER TABLE `ship`
  ADD COLUMN `sections` text DEFAULT NULL AFTER `notes`;
