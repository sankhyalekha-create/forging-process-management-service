-- V1_RB_58__add_tenant_configurations_column.sql
-- Rollback script for V1_58__add_tenant_configurations_column.sql
-- Removes tenant_configurations JSON column from tenant table

-- Drop the GIN index first
DROP INDEX IF EXISTS idx_tenant_configurations_gin;

-- Drop the JSON validation constraint
ALTER TABLE tenant DROP CONSTRAINT IF EXISTS tenant_configurations_is_json;

-- Remove the tenant_configurations column from tenant table
ALTER TABLE tenant DROP COLUMN IF EXISTS tenant_configurations;
