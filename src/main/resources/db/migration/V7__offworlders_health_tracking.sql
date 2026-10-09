-- V7: Offworlders health tracking and manual Vitals modifiers.
--
-- `health` stays the maximum (derived by the client from the attributes plus
-- `health_modifier`) and `armor` the effective rating of the worn armor items.
-- `health_modifier` lets ability text such as Hardy (+4 Health) be recorded
-- without breaking automatic derivation.
--
-- `current_health` is the running total the player ticks down and may exceed
-- `health` to represent temporary HP; existing rows start at full health.

ALTER TABLE offworlders_character_data
    ADD COLUMN current_health  INT NOT NULL DEFAULT 12,
    ADD COLUMN health_modifier INT NOT NULL DEFAULT 0;

UPDATE offworlders_character_data
SET current_health = health;
