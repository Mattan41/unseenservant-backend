-- V14: campaign world lore & background.
--
-- A third free-text block on a campaign, alongside the short overview
-- (`description`, shown on the Overview page) and the GM-only
-- `private_description`. World lore is public: it is returned to every
-- participant, and rendered in the "World Lore & Background" section.
-- (V13 is `add_ship_images`.)

ALTER TABLE campaign
    ADD COLUMN world_lore LONGTEXT NULL;
