CREATE TABLE campaign
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    name          VARCHAR(255) NULL,
    `description` VARCHAR(255) NULL,
    created_at    datetime NULL,
    updated_at    datetime NULL,
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
    id             BIGINT AUTO_INCREMENT NOT NULL,
    owner_id       BIGINT NOT NULL,
    campaign_id    BIGINT NULL,
    character_data JSON NULL,
    created_at     datetime NULL,
    updated_at     datetime(6)           NULL,
    CONSTRAINT pk_game_character PRIMARY KEY (id)
);

CREATE TABLE message
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    campaign_id  BIGINT NULL,
    user_id      BIGINT NULL,
    message_body LONGTEXT NULL,
    created_at   datetime NULL,
    updated_at   datetime NULL,
    CONSTRAINT pk_message PRIMARY KEY (id)
);

ALTER TABLE user
    ADD created_at datetime NULL;

ALTER TABLE user
    ADD provider_id VARCHAR(255) NULL;

ALTER TABLE user
    ADD provider_type VARCHAR(255) NULL;

ALTER TABLE user
    ADD updated_at datetime NULL;

ALTER TABLE user
    MODIFY provider_id VARCHAR (255) NOT NULL;

ALTER TABLE user
    MODIFY provider_type VARCHAR (255) NOT NULL;

ALTER TABLE user
    ADD CONSTRAINT uc_user_provider UNIQUE (provider_id);

ALTER TABLE campaign_user
    ADD CONSTRAINT FK_CAMPAIGN_USER_ON_CAMPAIGN FOREIGN KEY (campaign_id) REFERENCES campaign (id);

ALTER TABLE campaign_user
    ADD CONSTRAINT FK_CAMPAIGN_USER_ON_USER FOREIGN KEY (user_id) REFERENCES user (id);

ALTER TABLE game_character
    ADD CONSTRAINT FK_GAME_CHARACTER_ON_CAMPAIGN FOREIGN KEY (campaign_id) REFERENCES campaign (id);

ALTER TABLE game_character
    ADD CONSTRAINT FK_GAME_CHARACTER_ON_OWNER FOREIGN KEY (owner_id) REFERENCES user (id);

ALTER TABLE message
    ADD CONSTRAINT FK_MESSAGE_ON_CAMPAIGN FOREIGN KEY (campaign_id) REFERENCES campaign (id);

ALTER TABLE message
    ADD CONSTRAINT FK_MESSAGE_ON_USER FOREIGN KEY (user_id) REFERENCES user (id);

ALTER TABLE user
DROP
COLUMN google_id;

ALTER TABLE user
    MODIFY email VARCHAR (255) NOT NULL;

ALTER TABLE user
    MODIFY user_name VARCHAR (255) NOT NULL;