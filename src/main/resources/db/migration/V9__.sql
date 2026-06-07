CREATE TABLE character_spell
(
    character_id BIGINT       NOT NULL,
    spell_slug   VARCHAR(255) NOT NULL,
    CONSTRAINT pk_character_spell PRIMARY KEY (character_id, spell_slug)
);

CREATE TABLE spell
(
    slug          VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    raw_json_data TEXT         NOT NULL,
    created_at    datetime NULL,
    updated_at    datetime NULL,
    CONSTRAINT pk_spell PRIMARY KEY (slug)
);

ALTER TABLE character_spell
    ADD CONSTRAINT fk_chaspe_on_player_character FOREIGN KEY (character_id) REFERENCES game_character (id);

ALTER TABLE character_spell
    ADD CONSTRAINT fk_chaspe_on_spell FOREIGN KEY (spell_slug) REFERENCES spell (slug);

ALTER TABLE game_character
DROP
COLUMN character_data;

ALTER TABLE game_character
    ADD character_data TEXT NULL;

ALTER TABLE users
DROP
COLUMN provider_type;

ALTER TABLE users
DROP
COLUMN `role`;

ALTER TABLE users
    ADD provider_type VARCHAR(255) NOT NULL;

ALTER TABLE campaign_user
DROP
COLUMN `role`;

ALTER TABLE campaign_user
    ADD `role` VARCHAR(255) NULL;

ALTER TABLE email_whitelist
DROP
COLUMN `role`;

ALTER TABLE email_whitelist
    ADD `role` VARCHAR(255) NOT NULL;

ALTER TABLE users
    ADD `role` VARCHAR(255) NULL;

CREATE INDEX idx_user_email ON users (email);

CREATE INDEX idx_user_provider_id ON users (provider_id);

CREATE INDEX idx_user_username ON users (user_name);