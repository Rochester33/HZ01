-- Clean up duplicate and incorrect threshold records
USE hz01_db;

-- Show all current thresholds
SELECT id, device_id, sensor_type, warning_min, warning_max, critical_min, critical_max
FROM alert_thresholds
ORDER BY sensor_type, device_id;

-- Delete the incorrect humidity record (100, 100)
DELETE FROM alert_thresholds
WHERE sensor_type = 'humidity'
AND warning_min = 100
AND warning_max = 100;

-- Ensure correct thresholds exist (device_id = NULL means global default)
-- If they don't exist, insert them; if they exist, update them

-- Temperature
INSERT INTO alert_thresholds (device_id, sensor_type, warning_min, warning_max, critical_min, critical_max, unit)
VALUES (NULL, 'temperature', -10, 40, -20, 45, '°C')
ON DUPLICATE KEY UPDATE
    warning_min = -10,
    warning_max = 40,
    critical_min = -20,
    critical_max = 45;

-- Humidity
INSERT INTO alert_thresholds (device_id, sensor_type, warning_min, warning_max, critical_min, critical_max, unit)
VALUES (NULL, 'humidity', 20, 80, 10, 95, '%')
ON DUPLICATE KEY UPDATE
    warning_min = 20,
    warning_max = 80,
    critical_min = 10,
    critical_max = 95;

-- CO
INSERT INTO alert_thresholds (device_id, sensor_type, warning_min, warning_max, critical_min, critical_max, unit)
VALUES (NULL, 'co_level', NULL, 2000, NULL, 3000, 'ppm')
ON DUPLICATE KEY UPDATE
    warning_max = 2000,
    critical_max = 3000;

-- Methane
INSERT INTO alert_thresholds (device_id, sensor_type, warning_min, warning_max, critical_min, critical_max, unit)
VALUES (NULL, 'methane_level', NULL, 2000, NULL, 3000, 'ppm')
ON DUPLICATE KEY UPDATE
    warning_max = 2000,
    critical_max = 3000;

-- Battery
INSERT INTO alert_thresholds (device_id, sensor_type, warning_min, warning_max, critical_min, critical_max, unit)
VALUES (NULL, 'battery_level', 20, NULL, 10, NULL, '%')
ON DUPLICATE KEY UPDATE
    warning_min = 20,
    critical_min = 10;

-- Show final result
SELECT sensor_type, warning_min, warning_max, critical_min, critical_max, unit
FROM alert_thresholds
WHERE device_id IS NULL
ORDER BY sensor_type;
