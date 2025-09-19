-- V1_58__add_tenant_configurations_column.sql
-- Add tenant_configurations JSON column to tenant table for storing tenant-specific configuration values

-- Add tenant_configurations column to tenant table
ALTER TABLE tenant ADD COLUMN tenant_configurations TEXT;

-- Add comment to explain the purpose of the column
COMMENT ON COLUMN tenant.tenant_configurations IS 'JSON column for storing tenant-specific configurations like identification tag format numbers';

-- Create an index for better performance when searching within JSON configurations
-- Cast TEXT to JSONB for GIN index support
CREATE INDEX idx_tenant_configurations_gin ON tenant USING GIN ((tenant_configurations::jsonb));

-- Add a constraint to ensure valid JSON format
ALTER TABLE tenant ADD CONSTRAINT tenant_configurations_is_json CHECK (tenant_configurations IS NULL OR tenant_configurations::json IS NOT NULL);
