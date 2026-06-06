-- Widen device_commands so threshold pushes and the `auto` action stop
-- truncating. Previously command_type was ENUM('buzzer','led') and action was
-- ENUM('on','off','blink','sos'); update_threshold + JSON payloads hit
-- "Data truncated for column 'command_type'".
USE hz01_db;

ALTER TABLE device_commands
    MODIFY COLUMN command_type VARCHAR(32) NOT NULL,
    MODIFY COLUMN action       TEXT        NOT NULL;

-- Track the last hand-off time so the poller can re-deliver un-acknowledged
-- commands instead of dropping them after one fetch.
ALTER TABLE device_commands
    ADD COLUMN delivered_at DATETIME NULL AFTER created_at;

-- Recover commands that the old fire-once logic stranded in 'sent' but were
-- never acknowledged, so they get re-delivered under the new logic.
UPDATE device_commands
SET status = 'pending', delivered_at = NULL
WHERE status = 'sent' AND executed_at IS NULL;

SELECT id, device_id, command_type, status, created_at, delivered_at, executed_at
FROM device_commands
ORDER BY created_at DESC
LIMIT 20;
