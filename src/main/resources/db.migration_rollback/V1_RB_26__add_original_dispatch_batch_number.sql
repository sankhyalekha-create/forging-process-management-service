-- Rollback script for V1_26__add_original_dispatch_batch_number.sql

-- Drop both indexes first
DROP INDEX IF EXISTS idx_dispatch_batch_original_number_tenant;
DROP INDEX IF EXISTS idx_dispatch_batch_active_number_tenant;

-- Drop the column from the dispatch_batch table
ALTER TABLE dispatch_batch
DROP COLUMN original_dispatch_batch_number; 