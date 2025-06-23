-- ======================================================================
-- Rollback script for V1_40__add_previous_operation_processed_item_id.sql
-- Remove previous_operation_processed_item_id from ProcessedItem tables
-- ======================================================================

BEGIN;

-- Drop the indexes first
DROP INDEX IF EXISTS idx_processed_item_heat_treatment_batch_previous_operation;
DROP INDEX IF EXISTS idx_processed_item_machining_batch_previous_operation;
DROP INDEX IF EXISTS idx_processed_item_inspection_batch_previous_operation;
DROP INDEX IF EXISTS idx_processed_item_dispatch_batch_previous_operation;

-- Remove the previous_operation_processed_item_id column from all processed item tables
ALTER TABLE processed_item_heat_treatment_batch 
DROP COLUMN IF EXISTS previous_operation_processed_item_id;

ALTER TABLE processed_item_machining_batch 
DROP COLUMN IF EXISTS previous_operation_processed_item_id;

ALTER TABLE processed_item_inspection_batch 
DROP COLUMN IF EXISTS previous_operation_processed_item_id;

ALTER TABLE processed_item_dispatch_batch 
DROP COLUMN IF EXISTS previous_operation_processed_item_id;

COMMIT; 