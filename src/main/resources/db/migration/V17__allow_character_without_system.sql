-- A character may be created without a game system: only the generic fields on
-- game_character exist then, and no dnd5e_character_data / offworlders_character_data
-- row is attached.
--
-- NULL is the absence of a value, so system_type becomes nullable and V2's
-- 'DND5E' default goes away — the player's "no system" choice must not be
-- silently rewritten to a ruleset they never picked. Characters that do have a
-- system keep it (existing rows are untouched by this migration).
ALTER TABLE game_character
    MODIFY system_type VARCHAR(32) NULL;

-- NULL is allowed, but the column may still only hold a system the code can
-- render. This CHECK keeps a bogus value (manual SQL, a future non-enum writer,
-- a bad import) out of the table instead of letting it reach a DTO nobody can
-- map.
--
-- NOTE: extend this constraint whenever a constant is added to
-- org.kruskopf.backend.playercharacter.entity.GameSystem (the enum's javadoc
-- points here). A forgotten constant then fails the insert, not the read.
ALTER TABLE game_character
    ADD CONSTRAINT chk_character_system_type
        CHECK (system_type IS NULL OR system_type IN ('DND5E', 'OFFWORLDERS'));
