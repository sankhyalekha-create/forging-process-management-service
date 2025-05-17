-- Rollback script for V1_RB_23__add_original_machining_batch_number.sql

-- Drop both indexes first
DROP INDEX IF EXISTS idx_machining_batch_original_number_tenant;
DROP INDEX IF EXISTS idx_machining_batch_active_number_tenant;

-- Drop the column from the machining_batch table
ALTER TABLE machining_batch
DROP COLUMN original_machining_batch_number; 