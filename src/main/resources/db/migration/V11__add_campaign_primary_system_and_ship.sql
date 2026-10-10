-- V11: per-campaign primary system + Offworlders ship.
--
-- primary_system is optional on a campaign and drives which system-specific
-- sub-sections are shown (OFFWORLDERS -> Ship, DND5E -> Spells).
--
-- ship is 1:1 with a campaign (unique FK). Defaults mirror the Offworlders
-- rulebook (p.13): 15 Hull, 0 Armor, 1D6 Damage, 4 Max Drive Fuel.

ALTER TABLE campaign
    ADD COLUMN primary_system VARCHAR(32) NULL;

CREATE TABLE ship
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    campaign_id      BIGINT       NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    name             VARCHAR(255) NOT NULL DEFAULT '',
    hull             INT          NOT NULL DEFAULT 15,
    hull_max         INT          NOT NULL DEFAULT 15,
    armor            INT          NOT NULL DEFAULT 0,
    damage           VARCHAR(32)  NOT NULL DEFAULT '1D6',
    drive_fuel       INT          NOT NULL DEFAULT 4,
    max_drive_fuel   INT          NOT NULL DEFAULT 4,
    upgrades         TEXT         NULL,
    notes            TEXT         NULL,
    created_at       datetime     NULL,
    updated_at       datetime     NULL,
    last_modified_by VARCHAR(255) NULL,
    CONSTRAINT pk_ship PRIMARY KEY (id),
    CONSTRAINT uq_ship_campaign UNIQUE (campaign_id),
    CONSTRAINT fk_ship_campaign FOREIGN KEY (campaign_id) REFERENCES campaign (id) ON DELETE CASCADE
);
