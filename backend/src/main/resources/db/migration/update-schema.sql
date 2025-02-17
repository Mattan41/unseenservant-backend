CREATE TABLE user
(
    id        BIGINT AUTO_INCREMENT NOT NULL,
    google_id VARCHAR(255)          NULL,
    user_name VARCHAR(255)          NULL,
    full_name VARCHAR(255)          NULL,
    email     VARCHAR(255)          NULL,
    `role`    VARCHAR(255)          NULL,
    password  VARCHAR(255)          NULL,
    CONSTRAINT pk_user PRIMARY KEY (id)
);

ALTER TABLE user
    ADD CONSTRAINT uc_user_email UNIQUE (email);

ALTER TABLE user
    ADD CONSTRAINT uc_user_google UNIQUE (google_id);

ALTER TABLE user
    ADD CONSTRAINT uc_user_user_name UNIQUE (user_name);

SELECT *
FROM user;