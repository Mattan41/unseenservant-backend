-- The audit trail: one row per user- or administration-related event (an account
-- created, a login, a role change, a whitelist change), read back by an
-- administrator under Settings -> Logs.
--
-- Three properties of this table are deliberate and should not be "fixed":
--
--   * There is **no foreign key to `users`**. Every other table in this schema
--     has one, but a trail that disappears when the subject of it is deleted is
--     not a trail. `actor_name` / `target_name` are denormalised for the same
--     reason: the row keeps reading correctly after an account is renamed or
--     removed. Deleting a user therefore leaves their rows behind, naming an id
--     nothing points at any more.
--   * The rows are **append-only**. Nothing in the application updates or deletes
--     one; the only delete is the retention prune
--     (`app.audit.retention-days`, `AuditRetentionRunner`), and there is no
--     endpoint that can reach a row other than by reading it.
--   * `event_type` is a Java enum name stored as a string (`AuditEventType`), so
--     the client can badge and filter on it. 40 characters is headroom over the
--     longest value today (`ROLE_CHANGE_REFUSED`).
--
-- `client_ip` is the address `ClientIpResolver` documents (behind the tunnel:
-- `CF-Connecting-IP`) and is personal data like the rest of the row - which is
-- what the retention period exists for.
--
-- MySQL 8.4 syntax only: the `demo` profile builds the schema from the JPA
-- entity instead, so nothing here runs there. See docs/MIGRATIONS.md for the
-- numbering rules and docs/ARCHITECTURE.md ("Audit trail") for the behaviour.

CREATE TABLE `audit_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_type` varchar(40) NOT NULL,
  `actor_id` bigint DEFAULT NULL,
  `actor_name` varchar(255) DEFAULT NULL,
  `target_id` bigint DEFAULT NULL,
  `target_name` varchar(255) DEFAULT NULL,
  `detail` varchar(500) DEFAULT NULL,
  `client_ip` varchar(45) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_event_created_at` (`created_at`),
  KEY `idx_audit_event_type_created_at` (`event_type`, `created_at`),
  KEY `idx_audit_event_actor_id` (`actor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
