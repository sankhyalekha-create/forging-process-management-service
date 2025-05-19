-- Rollback script for V1_24__add_original_heat_treatment_batch_number.sql

-- Drop both indexes first
DROP INDEX IF EXISTS idx_heat_treatment_batch_original_number_tenant;
DROP INDEX IF EXISTS idx_heat_treatment_batch_active_number_tenant;

-- Drop the column from the heat_treatment_batch table
ALTER TABLE heat_treatment_batch
DROP COLUMN original_heat_treatment_batch_number; 