-- Add missing methane_level threshold
USE hz01_db;

-- Insert methane_level if it doesn't exist
INSERT INTO alert_thresholds (sensor_type, warning_min, warning_max, critical_min, critical_max, unit, device_id)
VALUES ('methane_level', NULL, 2000, NULL, 3000, 'ppm', NULL)
ON DUPLICATE KEY UPDATE
    warning_max = 2000,
    critical_max = 3000,
    unit = 'ppm';

-- Verify all thresholds
SELECT sensor_type, warning_min, warning_max, critical_min, critical_max, unit
FROM alert_thresholds
ORDER BY sensor_type;
