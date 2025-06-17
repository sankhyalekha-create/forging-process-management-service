-- Rollback Migration: Drop heat_treatment_heat table
-- Description: Rollback script to remove heat_treatment_heat table and related objects
-- Author: System
-- Date: 2024-12-20
-- Rollback for: V1_0_0_20241220_001__create_heat_treatment_heat_table.sql

-- Remove comments
COMMENT ON COLUMN heat_treatment_heat.deleted IS NULL;
COMMENT ON COLUMN heat_treatment_heat.deleted_at IS NULL;
COMMENT ON COLUMN heat_treatment_heat.updated_at IS NULL;
COMMENT ON COLUMN heat_treatment_heat.created_at IS NULL;
COMMENT ON COLUMN heat_treatment_heat.pieces_used IS NULL;
COMMENT ON COLUMN heat_treatment_heat.heat_id IS NULL;
COMMENT ON COLUMN heat_treatment_heat.heat_treatment_batch_id IS NULL;
COMMENT ON COLUMN heat_treatment_heat.id IS NULL;
COMMENT ON TABLE heat_treatment_heat IS NULL;

-- Drop unique constraint/index
DROP INDEX IF EXISTS uk_heat_treatment_heat_batch_heat_deleted;

-- Drop indexes
DROP INDEX IF EXISTS idx_heat_treatment_heat_created_at;
DROP INDEX IF EXISTS idx_heat_treatment_heat_deleted;
DROP INDEX IF EXISTS idx_heat_treatment_heat_heat_id;
DROP INDEX IF EXISTS idx_heat_treatment_heat_batch_id;

-- Drop foreign key constraints (will be dropped automatically with table, but explicit for clarity)
-- ALTER TABLE heat_treatment_heat DROP CONSTRAINT IF EXISTS fk_heat_treatment_heat_heat;
-- ALTER TABLE heat_treatment_heat DROP CONSTRAINT IF EXISTS fk_heat_treatment_heat_batch;

-- Drop check constraint (will be dropped automatically with table, but explicit for clarity)
-- ALTER TABLE heat_treatment_heat DROP CONSTRAINT IF EXISTS chk_heat_treatment_heat_pieces_used_positive;

-- Drop the table (this will also drop all constraints and indexes)
DROP TABLE IF EXISTS heat_treatment_heat;

-- Drop the sequence
DROP SEQUENCE IF EXISTS heat_treatment_heat_sequence;

-- Revoke permissions (uncomment and adjust as needed)
-- REVOKE USAGE, SELECT ON heat_treatment_heat_sequence FROM forging_app_user;
-- REVOKE SELECT, INSERT, UPDATE, DELETE ON heat_treatment_heat FROM forging_app_user; 