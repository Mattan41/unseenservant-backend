-- The ship's crew stops being its own per-participant entry and becomes the
-- campaign's characters.
--
-- A crew entry used to belong to a *participant* (`ship_crew`, keyed
-- `ship_id` + `user_id`) and held free-text notes, a gallery and a free-text
-- list of characters aboard. The ship view now reads the party instead: one card
-- per character, with the character's own notes and its gallery — which is why
-- `game_character` gains `image_urls` here.
--
-- This migration moves the data that would otherwise be lost and then drops the
-- old table:
--
--   * the entry's notes are appended to the participant's character in that
--     campaign (a blank line apart, so the two paragraphs stay readable),
--   * the entry's gallery images are carried over to that character,
--   * the free-text "characters aboard" list is deliberately dropped: the
--     characters it named are real characters now, and the list only ever
--     duplicated their names.
--
-- Which character receives an entry: the participant's first one in that
-- campaign, ordered by name and then by id. Alphabetical because that is the
-- order the crew list renders in, and the id tie-break keeps it deterministic
-- when two characters share a name. Nobody has more than one character in a
-- campaign in practice, so the rule has never had to arbitrate.
--
-- The mapping goes through a temporary table rather than a JOIN on a derived
-- table of `game_character`: MySQL refuses an UPDATE whose subquery reads the
-- table being updated (error 1093), and a derived table that is merged back into
-- the statement is exactly that.
--
-- MySQL 8.4 syntax only — the `demo` profile builds the schema from the entities
-- instead, so nothing here runs there. See docs/MIGRATIONS.md for the numbering
-- rules and docs/INTEGRATION_CONTRACT.md (section "Characters") for the contract.

ALTER TABLE `game_character`
  ADD COLUMN `image_urls` text DEFAULT NULL AFTER `image_url`;

CREATE TEMPORARY TABLE `ship_crew_into_character` (
  `character_id` bigint NOT NULL,
  `notes` text,
  `image_urls` text,
  PRIMARY KEY (`character_id`)
);

INSERT INTO `ship_crew_into_character` (`character_id`, `notes`, `image_urls`)
SELECT `character`.`id`, `crew`.`notes`, `crew`.`image_urls`
FROM `ship_crew` `crew`
JOIN `ship` ON `ship`.`id` = `crew`.`ship_id`
JOIN `game_character` `character`
  ON `character`.`campaign_id` = `ship`.`campaign_id`
 AND `character`.`owner_id` = `crew`.`user_id`
WHERE `character`.`id` = (
  SELECT `first_character`.`id`
  FROM `game_character` `first_character`
  WHERE `first_character`.`campaign_id` = `ship`.`campaign_id`
    AND `first_character`.`owner_id` = `crew`.`user_id`
  ORDER BY `first_character`.`name`, `first_character`.`id`
  LIMIT 1
);

UPDATE `game_character` `character`
JOIN `ship_crew_into_character` `entry` ON `entry`.`character_id` = `character`.`id`
SET `character`.`notes` = CASE
      -- An entry with no notes changes nothing.
      WHEN COALESCE(`entry`.`notes`, '') = '' THEN `character`.`notes`
      -- A character with no notes simply takes the entry's.
      WHEN COALESCE(`character`.`notes`, '') = '' THEN `entry`.`notes`
      ELSE CONCAT(`character`.`notes`, '\n\n', `entry`.`notes`)
    END,
    -- Gallery images are appended to whatever the character already has; the
    -- column is new here, so in practice it is the entry's list in full.
    `character`.`image_urls` = CASE
      WHEN JSON_LENGTH(COALESCE(`entry`.`image_urls`, '[]')) > 0
        THEN `entry`.`image_urls`
      ELSE `character`.`image_urls`
    END;

DROP TEMPORARY TABLE `ship_crew_into_character`;

DROP TABLE `ship_crew`;
