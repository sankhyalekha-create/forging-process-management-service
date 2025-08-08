-- Rollback script for V1_49__add_multi_parent_dispatch_support.sql
-- This script reverses all changes made for multi-parent dispatch functionality

-- 1. Drop comments (reverse step 7)
COMMENT ON COLUMN processed_item_dispatch_batch.is_multi_parent_dispatch IS NULL;
COMMENT ON COLUMN processed_item_dispatch_batch.total_parent_entities_count IS NULL;
COMMENT ON COLUMN dispatch_processed_item_consumption.batch_identifier IS NULL;
COMMENT ON COLUMN dispatch_processed_item_consumption.consumed_pieces_count IS NULL;
COMMENT ON COLUMN dispatch_processed_item_consumption.previous_operation_type IS NULL;
COMMENT ON COLUMN dispatch_processed_item_consumption.previous_operation_entity_id IS NULL;
COMMENT ON TABLE dispatch_processed_item_consumption IS NULL;

-- 2. Remove migrated data from consumption table (reverse step 6)
-- This will remove all consumption records that were migrated from inspection data
DELETE FROM dispatch_processed_item_consumption 
WHERE previous_operation_type = 'QUALITY' 
AND batch_identifier LIKE 'InspectionBatch-%'
AND entity_context LIKE 'Migrated from inspection batch%';

-- 3. Reset existing data in processed_item_dispatch_batch (reverse step 5)
-- Remove the default values we set for the new columns
UPDATE processed_item_dispatch_batch 
SET 
    total_parent_entities_count = NULL,
    is_multi_parent_dispatch = NULL;

-- 4. Drop indexes (reverse step 4)
DROP INDEX IF EXISTS idx_dispatch_consumption_deleted;
DROP INDEX IF EXISTS idx_dispatch_consumption_operation_type;
DROP INDEX IF EXISTS idx_dispatch_consumption_entity_id;
DROP INDEX IF EXISTS idx_dispatch_consumption_batch_id;

-- 5. Remove new columns from processed_item_dispatch_batch table (reverse step 3)
ALTER TABLE processed_item_dispatch_batch 
DROP COLUMN IF EXISTS is_multi_parent_dispatch,
DROP COLUMN IF EXISTS total_parent_entities_count;

-- 6. Drop sequence (reverse step 2)
DROP SEQUENCE IF EXISTS dispatch_processed_item_consumption_sequence;

-- 7. Drop the new table (reverse step 1)
-- Note: This will cascade and remove all related consumption records
DROP TABLE IF EXISTS dispatch_processed_item_consumption;

-- 8. Verification queries (optional - can be uncommented for manual verification)
-- These queries can be run to verify the rollback was successful

-- Verify table no longer exists
-- SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'dispatch_processed_item_consumption';

-- Verify sequence no longer exists  
-- SELECT COUNT(*) FROM information_schema.sequences WHERE sequence_name = 'dispatch_processed_item_consumption_sequence';

-- Verify columns were removed from processed_item_dispatch_batch
-- SELECT column_name FROM information_schema.columns 
-- WHERE table_name = 'processed_item_dispatch_batch' 
-- AND column_name IN ('total_parent_entities_count', 'is_multi_parent_dispatch');

-- Verify indexes were dropped
-- SELECT indexname FROM pg_indexes WHERE tablename = 'dispatch_processed_item_consumption';

-- Note: After running this rollback script, you may need to restart your application
-- to ensure that any cached schema information is refreshed.