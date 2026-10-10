-- V10: promote the Offworlders `look` to a generic character `appearance`.
--
-- Appearance is meaningful for every game system, so it moves out of the
-- system-specific offworlders_character_data table and onto the core
-- game_character table (like notes/backstory). Existing values are copied
-- across before the old column is dropped.

ALTER TABLE game_character
    ADD COLUMN appearance TEXT NULL;

UPDATE game_character c
    JOIN offworlders_character_data o ON o.character_id = c.id
SET c.appearance = o.look;

ALTER TABLE offworlders_character_data
    DROP COLUMN look;
