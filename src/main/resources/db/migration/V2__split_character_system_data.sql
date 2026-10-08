-- Split D&D 5e-specific fields out of the generic game_character table.
-- Adds system-agnostic columns (system_type, notes) and moves the 5e data
-- into a dedicated dnd5e_character_data table linked 1:1 by shared primary key.

ALTER TABLE game_character
    ADD COLUMN system_type VARCHAR(32) NOT NULL DEFAULT 'DND5E',
    ADD COLUMN notes TEXT NULL;

CREATE TABLE dnd5e_character_data
(
    character_id    BIGINT       NOT NULL,
    level           INT          NOT NULL,
    character_class VARCHAR(255) NOT NULL,
    race            VARCHAR(255) NOT NULL,
    hit_points      INT          NOT NULL DEFAULT 0,
    armor_class     INT          NOT NULL DEFAULT 10,
    stats           TEXT NULL,
    CONSTRAINT pk_dnd5e_character_data PRIMARY KEY (character_id)
);

-- Copy existing characters (all legacy rows are D&D 5e) into the new table.
INSERT INTO dnd5e_character_data (character_id, level, character_class, race, hit_points, armor_class, stats)
SELECT id, level, character_class, race, 0, 10, character_data
FROM game_character;

-- Remove the migrated columns from the generic table.
ALTER TABLE game_character
    DROP COLUMN level,
    DROP COLUMN character_class,
    DROP COLUMN race,
    DROP COLUMN character_data;

ALTER TABLE dnd5e_character_data
    ADD CONSTRAINT FK_DND5E_CHAR_DATA_ON_CHARACTER FOREIGN KEY (character_id) REFERENCES game_character (id);

-- Spells now belong to the D&D 5e data block; re-point the join table FK
-- accordingly. The values are identical (shared primary key), so this is safe.
ALTER TABLE character_spell
    DROP FOREIGN KEY fk_chaspe_on_player_character;

ALTER TABLE character_spell
    ADD CONSTRAINT fk_chaspe_on_dnd5e_character_data FOREIGN KEY (character_id) REFERENCES dnd5e_character_data (character_id);
