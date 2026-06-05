-- Migration: Add methane_level column to sensor_readings table
-- Run this SQL script on your database before restarting the backend

USE hz01_db;

-- Check if column exists and add it if it doesn't
SET @dbname = DATABASE();
SET @tablename = 'sensor_readings';
SET @columnname = 'methane_level';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT ''Column already exists'' AS message;',
  'ALTER TABLE sensor_readings ADD COLUMN methane_level FLOAT NULL COMMENT ''ppm'' AFTER co_level;'
));

PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Verify the column was added
DESCRIBE sensor_readings;
