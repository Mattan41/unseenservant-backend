-- V3: Add Offworlders character data.
-- Offworlders is a rules-light sci-fi system. Its sheet is stored in a
-- dedicated table linked 1:1 to game_character by a shared primary key,
-- mirroring the existing dnd5e_character_data layout.
--
-- Freeform-friendly lists (skills, abilities) and the attribute block are
-- stored as JSON in TEXT columns: the catalogs are small and players may add
-- custom entries, so join tables would be overkill.

CREATE TABLE offworlders_character_data
(
    character_id    BIGINT       NOT NULL,
    character_class VARCHAR(255) NOT NULL DEFAULT '',
    species         VARCHAR(255) NOT NULL DEFAULT '',
    look            VARCHAR(255) NOT NULL DEFAULT '',
    xp              INT          NOT NULL DEFAULT 0,
    health          INT          NOT NULL DEFAULT 12,
    armor           INT          NOT NULL DEFAULT 0,
    supply          INT          NOT NULL DEFAULT 0,
    supply_max      INT          NOT NULL DEFAULT 0,
    stats           TEXT NULL,
    skills          TEXT NULL,
    abilities       TEXT NULL,
    CONSTRAINT pk_offworlders_character_data PRIMARY KEY (character_id)
);

ALTER TABLE offworlders_character_data
    ADD CONSTRAINT FK_OFFWORLDERS_CHAR_DATA_ON_CHARACTER FOREIGN KEY (character_id) REFERENCES game_character (id);
