-- V4: Offworlders credits, gear block, and corrected Supply defaults.
-- The character sheet tracks Credits, and chargen (rules p.6) gives every PC one
-- light weapon plus a choice of either extra Credits or Light armor and a second
-- weapon, so the gear block is stored as JSON in a single TEXT column via
-- OffworldersGearConverter (mirroring the stats/skills/abilities columns).
--
-- Supply is always capped at 3 (rules p.11), so the supply / supply_max column
-- defaults are corrected from 0 to 3 and existing rows are normalized.

ALTER TABLE offworlders_character_data
    ADD COLUMN credits INT NOT NULL DEFAULT 3;

ALTER TABLE offworlders_character_data
    ADD COLUMN gear TEXT NULL;

ALTER TABLE offworlders_character_data
    ALTER COLUMN supply SET DEFAULT 3;

ALTER TABLE offworlders_character_data
    ALTER COLUMN supply_max SET DEFAULT 3;

UPDATE offworlders_character_data SET supply_max = 3 WHERE supply_max <> 3;
