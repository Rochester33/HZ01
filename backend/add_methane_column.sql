-- Migration: Add methane_level column to sensor_readings table
-- Run this SQL script on your database before restarting the backend

USE hz01_db;

-- Add methane_level column if it doesn't exist
ALTER TABLE sensor_readings
ADD COLUMN IF NOT EXISTS methane_level FLOAT NULL COMMENT 'ppm'
AFTER co_level;

-- Verify the column was added
DESCRIBE sensor_readings;
