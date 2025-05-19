-- Rollback script for V1_25__add_original_inspection_batch_number.sql

-- Drop both indexes first
DROP INDEX IF EXISTS idx_inspection_batch_original_number_tenant;
DROP INDEX IF EXISTS idx_inspection_batch_active_number_tenant;

-- Drop the column from the inspection_batch table
ALTER TABLE inspection_batch
DROP COLUMN original_inspection_batch_number; 