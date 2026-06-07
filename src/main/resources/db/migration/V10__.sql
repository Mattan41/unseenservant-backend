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