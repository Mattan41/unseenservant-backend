-- V12: private campaign description.
--
-- A second free-text description alongside the public one. It is returned only
-- to the campaign owner or a campaign GM; everyone else receives null
-- (mirrors the character's private_backstory field).

ALTER TABLE campaign
    ADD COLUMN private_description LONGTEXT NULL;
