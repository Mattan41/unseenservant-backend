CREATE TABLE campaign
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    name             VARCHAR(255) NULL,
    `description`    VARCHAR(255) NULL,
    created_at       datetime NULL,
    updated_at       datetime NULL,
    created_by       VARCHAR(255) NULL,
    last_modified_by VARCHAR(255) NULL,
    CONSTRAINT pk_campaign PRIMARY KEY (id)
);

CREATE TABLE campaign_user
(
    `role`      VARCHAR(255) NULL,
    nickname    VARCHAR(255) NOT NULL,
    campaign_id BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    CONSTRAINT pk_campaign_user PRIMARY KEY (campaign_id, user_id)
);

CREATE TABLE game_character
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    owner_id         BIGINT NOT NULL,
    campaign_id      BIGINT NULL,
    character_data   JSON NULL,
    created_at       datetime NULL,
    updated_at       datetime NULL,
    created_by       VARCHAR(255) NULL,
    last_modified_by VARCHAR(255) NULL,
    CONSTRAINT pk_game_character PRIMARY KEY (id)
);

CREATE TABLE messages
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    campaign_id      BIGINT NULL,
    user_id          BIGINT NULL,
    message_body     LONGTEXT NULL,
    created_at       datetime NULL,
    updated_at       datetime NULL,
    last_modified_by VARCHAR(255) NULL,
    CONSTRAINT pk_messages PRIMARY KEY (id)
);

CREATE TABLE users
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    provider_id   VARCHAR(255) NOT NULL,
    provider_type VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NULL,
    user_name     VARCHAR(255) NOT NULL,
    display_name  VARCHAR(50) NULL,
    `role`        VARCHAR(255) NULL,
    password      VARCHAR(255) NULL,
    created_at    datetime NULL,
    updated_at    datetime NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uc_users_provider UNIQUE (provider_id);

ALTER TABLE users
    ADD CONSTRAINT uc_users_username UNIQUE (user_name);

CREATE INDEX idx_message_created_at ON messages (created_at);

CREATE INDEX idx_user_email ON users (email);

CREATE INDEX idx_user_provider_id ON users (provider_id);

CREATE INDEX idx_user_username ON users (user_name);

ALTER TABLE campaign_user
    ADD CONSTRAINT FK_CAMPAIGN_USER_ON_CAMPAIGN FOREIGN KEY (campaign_id) REFERENCES campaign (id);

ALTER TABLE campaign_user
    ADD CONSTRAINT FK_CAMPAIGN_USER_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE game_character
    ADD CONSTRAINT FK_GAME_CHARACTER_ON_CAMPAIGN FOREIGN KEY (campaign_id) REFERENCES campaign (id);

ALTER TABLE game_character
    ADD CONSTRAINT FK_GAME_CHARACTER_ON_OWNER FOREIGN KEY (owner_id) REFERENCES users (id);

CREATE INDEX idx_character_owner_id ON game_character (owner_id);

ALTER TABLE messages
    ADD CONSTRAINT FK_MESSAGES_ON_CAMPAIGN FOREIGN KEY (campaign_id) REFERENCES campaign (id);

CREATE INDEX idx_message_campaign_id ON messages (campaign_id);

ALTER TABLE messages
    ADD CONSTRAINT FK_MESSAGES_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

CREATE INDEX idx_message_user_id ON messages (user_id);