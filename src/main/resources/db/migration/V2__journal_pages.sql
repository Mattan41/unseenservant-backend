-- Journal: one book of numbered pages per campaign participant. The journal is
-- private to the participant who owns it — the campaign owner and a GM have no
-- access to it either — and `journal_page.position` is the page's place in the
-- book (1-based), not its identity: the row keeps a stable id while the numbers
-- of the pages after it shift on insert and delete.
--
-- Deleting a campaign or a user takes the journals with it (ON DELETE CASCADE);
-- removing a participant from a campaign does not — the rows stay and are
-- reachable again if the person rejoins.
--
-- See docs/MIGRATIONS.md for the numbering rules and
-- docs/INTEGRATION_CONTRACT.md (section "Journals") for the contract.

CREATE TABLE `journal` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `campaign_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_journal_campaign_user` (`campaign_id`,`user_id`),
  KEY `fk_journal_user` (`user_id`),
  CONSTRAINT `fk_journal_campaign` FOREIGN KEY (`campaign_id`) REFERENCES `campaign` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_journal_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `journal_page` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `journal_id` bigint NOT NULL,
  `content` longtext,
  `position` int NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_journal_page_journal_position` (`journal_id`,`position`),
  CONSTRAINT `fk_journal_page_journal` FOREIGN KEY (`journal_id`) REFERENCES `journal` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
