-- V13: ship images.
--
-- image_url  - single profile image shown for the ship (replaces the campaign
--              image in the ship view);
-- image_urls - JSON array of gallery images (drawings, maps, etc.).

ALTER TABLE ship
    ADD COLUMN image_url  VARCHAR(1024) NULL,
    ADD COLUMN image_urls TEXT NULL;
