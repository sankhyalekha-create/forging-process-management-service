-- Add isInternal and isActive columns to tenant table
ALTER TABLE tenant ADD COLUMN is_internal BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE tenant ADD COLUMN is_active BOOLEAN DEFAULT TRUE NOT NULL; 