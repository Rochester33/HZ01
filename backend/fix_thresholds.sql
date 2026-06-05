-- Fix incorrect CO and methane thresholds
-- These sensors measure in ppm, not percentage

USE hz01_db;

-- Update CO thresholds (should be in ppm)
UPDATE alert_thresholds
SET warning_max = 2000, critical_max = 3000,
    warning_min = NULL, critical_min = NULL
WHERE sensor_type = 'co_level';

-- Update methane thresholds (should be in ppm)
UPDATE alert_thresholds
SET warning_max = 2000, critical_max = 3000,
    warning_min = NULL, critical_min = NULL
WHERE sensor_type = 'methane_level';

-- Remove oxygen thresholds (sensor not used)
DELETE FROM alert_thresholds WHERE sensor_type = 'oxygen';

-- Verify the changes
SELECT sensor_type, warning_min, warning_max, critical_min, critical_max, unit
FROM alert_thresholds
ORDER BY sensor_type;
