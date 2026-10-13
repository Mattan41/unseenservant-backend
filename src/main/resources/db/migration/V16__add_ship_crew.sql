-- V16: ship crew entries (one per participant).
--
-- Each participant has an entry on the ship's crew manifest: free-text notes,
-- gallery images (shown in the ship's single gallery) and the characters they
-- have aboard (name + role on the ship, stored as JSON text).
--
-- Keyed on user_id (not campaign_user) so an entry survives the participant
-- leaving the campaign and re-activates unchanged when they rejoin. Whether an
-- entry is visible is derived from campaign membership, never stored.

CREATE TABLE ship_crew
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    ship_id          BIGINT       NOT NULL,
    user_id          BIGINT       NOT NULL,
    notes            TEXT         NULL,
    image_urls       TEXT         NULL,
    characters       TEXT         NULL,
    created_at       datetime     NULL,
    updated_at       datetime     NULL,
    last_modified_by VARCHAR(255) NULL,
    CONSTRAINT pk_ship_crew PRIMARY KEY (id),
    CONSTRAINT uq_ship_crew_ship_user UNIQUE (ship_id, user_id),
    CONSTRAINT fk_ship_crew_ship FOREIGN KEY (ship_id) REFERENCES ship (id) ON DELETE CASCADE,
    CONSTRAINT fk_ship_crew_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
