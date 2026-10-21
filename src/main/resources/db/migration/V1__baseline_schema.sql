-- Baseline schema: the production schema as of the 2026-10-20 backup, squashed
-- from the former V1-V17 migrations (the SQL files and the Java V5/V6/V8).
-- See docs/MIGRATIONS.md. Existing databases are marked as already baselined;
-- a fresh database is built by running this one file.
-- Tables are ordered so every foreign key points at a table created above it.

CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `provider_id` varchar(255) NOT NULL,
  `provider_type` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `full_name` varchar(255) DEFAULT NULL,
  `user_name` varchar(255) NOT NULL,
  `display_name` varchar(50) DEFAULT NULL,
  `role` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uc_users_email` (`email`),
  UNIQUE KEY `uc_users_provider` (`provider_id`),
  UNIQUE KEY `uc_users_username` (`user_name`),
  UNIQUE KEY `uc_users_display_name` (`display_name`),
  KEY `idx_user_email` (`email`),
  KEY `idx_user_provider_id` (`provider_id`),
  KEY `idx_user_username` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `email_whitelist` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `role` varchar(255) NOT NULL,
  `active` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uc_email_whitelist_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `spell` (
  `slug` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `raw_json_data` text NOT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `campaign` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `description` longtext,
  `image_url` varchar(1024) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  `owner_id` bigint NOT NULL,
  `primary_system` varchar(32) DEFAULT NULL,
  `private_description` longtext,
  `world_lore` longtext,
  PRIMARY KEY (`id`),
  KEY `FK_CAMPAIGN_ON_OWNER` (`owner_id`),
  CONSTRAINT `FK_CAMPAIGN_ON_OWNER` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `game_character` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `owner_id` bigint NOT NULL,
  `campaign_id` bigint DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  `system_type` varchar(32) DEFAULT NULL,
  `notes` text,
  `backstory` text,
  `private_backstory` text,
  `appearance` text,
  PRIMARY KEY (`id`),
  KEY `FK_GAME_CHARACTER_ON_CAMPAIGN` (`campaign_id`),
  KEY `idx_character_owner_id` (`owner_id`),
  CONSTRAINT `FK_GAME_CHARACTER_ON_CAMPAIGN` FOREIGN KEY (`campaign_id`) REFERENCES `campaign` (`id`),
  CONSTRAINT `FK_GAME_CHARACTER_ON_OWNER` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_character_system_type` CHECK (((`system_type` is null) or (`system_type` in (_utf8mb4'DND5E',_utf8mb4'OFFWORLDERS'))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `dnd5e_character_data` (
  `character_id` bigint NOT NULL,
  `level` int NOT NULL,
  `character_class` varchar(255) NOT NULL,
  `race` varchar(255) NOT NULL,
  `hit_points` int NOT NULL DEFAULT '0',
  `armor_class` int NOT NULL DEFAULT '10',
  `stats` text,
  PRIMARY KEY (`character_id`),
  CONSTRAINT `FK_DND5E_CHAR_DATA_ON_CHARACTER` FOREIGN KEY (`character_id`) REFERENCES `game_character` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `offworlders_character_data` (
  `character_id` bigint NOT NULL,
  `character_class` varchar(255) NOT NULL DEFAULT '',
  `species` varchar(255) NOT NULL DEFAULT '',
  `xp` int NOT NULL DEFAULT '0',
  `health` int NOT NULL DEFAULT '12',
  `armor` int NOT NULL DEFAULT '0',
  `supply` int NOT NULL DEFAULT '3',
  `supply_max` int NOT NULL DEFAULT '3',
  `stats` text,
  `skills` text,
  `abilities` text,
  `credits` int NOT NULL DEFAULT '3',
  `items` text,
  `current_health` int NOT NULL DEFAULT '12',
  `health_modifier` int NOT NULL DEFAULT '0',
  `weapons` text,
  PRIMARY KEY (`character_id`),
  CONSTRAINT `FK_OFFWORLDERS_CHAR_DATA_ON_CHARACTER` FOREIGN KEY (`character_id`) REFERENCES `game_character` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `campaign_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `message_body` longtext,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_message_created_at` (`created_at`),
  KEY `idx_message_campaign_id` (`campaign_id`),
  KEY `idx_message_user_id` (`user_id`),
  CONSTRAINT `FK_MESSAGES_ON_CAMPAIGN` FOREIGN KEY (`campaign_id`) REFERENCES `campaign` (`id`),
  CONSTRAINT `FK_MESSAGES_ON_USER` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `ship` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `campaign_id` bigint NOT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  `name` varchar(255) NOT NULL DEFAULT '',
  `hull` int NOT NULL DEFAULT '15',
  `hull_max` int NOT NULL DEFAULT '15',
  `armor` int NOT NULL DEFAULT '0',
  `damage` varchar(32) NOT NULL DEFAULT '1D6',
  `drive_fuel` int NOT NULL DEFAULT '4',
  `max_drive_fuel` int NOT NULL DEFAULT '4',
  `upgrades` text,
  `notes` text,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  `image_url` varchar(1024) DEFAULT NULL,
  `image_urls` text,
  `credits` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_ship_campaign` (`campaign_id`),
  CONSTRAINT `fk_ship_campaign` FOREIGN KEY (`campaign_id`) REFERENCES `campaign` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `campaign_user` (
  `role` varchar(255) DEFAULT NULL,
  `nickname` varchar(255) NOT NULL,
  `campaign_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`campaign_id`,`user_id`),
  KEY `FK_CAMPAIGN_USER_ON_USER` (`user_id`),
  CONSTRAINT `FK_CAMPAIGN_USER_ON_CAMPAIGN` FOREIGN KEY (`campaign_id`) REFERENCES `campaign` (`id`),
  CONSTRAINT `FK_CAMPAIGN_USER_ON_USER` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `character_spell` (
  `character_id` bigint NOT NULL,
  `spell_slug` varchar(255) NOT NULL,
  PRIMARY KEY (`character_id`,`spell_slug`),
  KEY `fk_chaspe_on_spell` (`spell_slug`),
  CONSTRAINT `fk_chaspe_on_dnd5e_character_data` FOREIGN KEY (`character_id`) REFERENCES `dnd5e_character_data` (`character_id`),
  CONSTRAINT `fk_chaspe_on_spell` FOREIGN KEY (`spell_slug`) REFERENCES `spell` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `ship_crew` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ship_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `notes` text,
  `image_urls` text,
  `characters` text,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_ship_crew_ship_user` (`ship_id`,`user_id`),
  KEY `fk_ship_crew_user` (`user_id`),
  CONSTRAINT `fk_ship_crew_ship` FOREIGN KEY (`ship_id`) REFERENCES `ship` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ship_crew_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
