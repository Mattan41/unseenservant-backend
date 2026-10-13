-- V15: ship credits.
--
-- The ship sheet tracks a shared crew Credits balance (like the character's),
-- so the crew can spend and earn currency together. Minimum 0, no upper cap.

ALTER TABLE ship
    ADD COLUMN credits INT NOT NULL DEFAULT 0;
