-- V9: character backstory.
--
-- Two free-text fields on the core character (system-agnostic, i.e. shared by
-- every game system):
--   * backstory         - visible to all campaign members
--   * private_backstory - visible to the owner and the campaign GM only
--
-- Writes are owner-only (enforced in GameCharacterService); the campaign GM has
-- read access but cannot edit the private field.

ALTER TABLE game_character
    ADD COLUMN backstory         TEXT NULL,
    ADD COLUMN private_backstory TEXT NULL;
