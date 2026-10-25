-- Campaign planner — the first optional campaign add-on.
--
-- Two levels live here. `campaign_addon` is the capability switch: one row per
-- add-on a campaign owner has turned on, keyed by a stable string (`planner`), so
-- the next add-on is a row and not a migration. Every campaign endpoint that
-- answers with a campaign carries the enabled keys along.
--
-- The planner itself is a plan (a question the group is answering, "when do we
-- play next?") holding candidate dates, an attendance answer per member and date,
-- and an opt-out per member:
--
--   planner_plan              one plan; `status` is OPEN | DECIDED and
--                             `decided_date_id` the chosen date once decided
--   planner_date              one candidate date of a plan (unique per day)
--   planner_attendance        one member's answer: YES | NO | MAYBE. No row means
--                             "not answered yet", which counts against a date —
--                             green has to mean everyone still in said yes
--   planner_member_exclusion  a member who left the plan, or was removed by the
--                             campaign owner. The roster is the campaign's
--                             participants minus these rows, which is what makes
--                             "every participant is added automatically" true:
--                             nothing is inserted when a plan is created or when
--                             somebody joins the campaign later. Answers survive
--                             leaving, so rejoining restores them (as the journal
--                             keeps its pages).
--
-- Deleting a campaign or a user takes all of it along (ON DELETE CASCADE).
-- Removing a participant from a campaign does not.
--
-- See docs/MIGRATIONS.md for the numbering rules and
-- docs/INTEGRATION_CONTRACT.md (sections "Campaign add-ons" and "Planner") for
-- the contract. MySQL 8.4 syntax only — the demo profile builds these tables from
-- the entities instead, so nothing here runs there.

CREATE TABLE `campaign_addon` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `campaign_id` bigint NOT NULL,
  `addon_key` varchar(50) NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_campaign_addon_campaign_key` (`campaign_id`,`addon_key`),
  CONSTRAINT `fk_campaign_addon_campaign` FOREIGN KEY (`campaign_id`) REFERENCES `campaign` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `planner_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `campaign_id` bigint NOT NULL,
  `title` varchar(200) NOT NULL,
  `description` longtext,
  `status` varchar(20) NOT NULL DEFAULT 'OPEN',
  `decided_date_id` bigint DEFAULT NULL,
  `created_by_user_id` bigint NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_planner_plan_campaign_status` (`campaign_id`,`status`),
  KEY `fk_planner_plan_user` (`created_by_user_id`),
  CONSTRAINT `fk_planner_plan_campaign` FOREIGN KEY (`campaign_id`) REFERENCES `campaign` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_planner_plan_user` FOREIGN KEY (`created_by_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `planner_date` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` bigint NOT NULL,
  `plan_date` date NOT NULL,
  `start_time` time DEFAULT NULL,
  `end_time` time DEFAULT NULL,
  `note` varchar(500) DEFAULT NULL,
  `created_by_user_id` bigint NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_planner_date_plan_date` (`plan_id`,`plan_date`),
  KEY `fk_planner_date_user` (`created_by_user_id`),
  CONSTRAINT `fk_planner_date_plan` FOREIGN KEY (`plan_id`) REFERENCES `planner_plan` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_planner_date_user` FOREIGN KEY (`created_by_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- A plan's decision points at one of its own dates, so this constraint can only
-- be added once planner_date exists — hence the ALTER rather than a column-level
-- foreign key above.
ALTER TABLE `planner_plan`
  ADD CONSTRAINT `fk_planner_plan_decided_date` FOREIGN KEY (`decided_date_id`) REFERENCES `planner_date` (`id`) ON DELETE SET NULL;

CREATE TABLE `planner_attendance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_date_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `state` varchar(10) NOT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_planner_attendance_date_user` (`plan_date_id`,`user_id`),
  KEY `fk_planner_attendance_user` (`user_id`),
  CONSTRAINT `fk_planner_attendance_date` FOREIGN KEY (`plan_date_id`) REFERENCES `planner_date` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_planner_attendance_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `planner_member_exclusion` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `removed_by` varchar(255) DEFAULT NULL,
  `removed_by_owner` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_planner_exclusion_plan_user` (`plan_id`,`user_id`),
  KEY `fk_planner_exclusion_user` (`user_id`),
  CONSTRAINT `fk_planner_exclusion_plan` FOREIGN KEY (`plan_id`) REFERENCES `planner_plan` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_planner_exclusion_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
