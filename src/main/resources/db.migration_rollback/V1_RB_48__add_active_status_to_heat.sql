-- Rollback: Remove active status column from heat table

-- Drop indexes first
DROP INDEX IF EXISTS idx_heat_tenant_active;
DROP INDEX IF EXISTS idx_heat_active;

-- Remove the active column
ALTER TABLE heat DROP COLUMN IF EXISTS active; 