-- Drop foreign key from processed_item_inspection_batch
ALTER TABLE processed_item_inspection_batch
DROP CONSTRAINT IF EXISTS fk_processed_item_inspection_batch_dispatch_batch;

-- Remove dispatch_batch_id column from processed_item_inspection_batch
ALTER TABLE processed_item_inspection_batch
DROP COLUMN IF EXISTS dispatch_batch_id;

ALTER TABLE processed_item_inspection_batch
DROP COLUMN IF EXISTS dispatched_pieces_count;

-- Drop unique constraint enforcing one-to-one relationship
ALTER TABLE processed_item_dispatch_batch
DROP CONSTRAINT IF EXISTS uq_processed_item_dispatch_batch_dispatch_batch;

-- Drop indexes from processed_item_dispatch_batch
DROP INDEX IF EXISTS idx_total_dispatch_pieces_count_processed_item_dispatch_batch;
DROP INDEX IF EXISTS idx_item_status_processed_item_dispatch_batch;

-- Drop foreign key constraints from processed_item_dispatch_batch
ALTER TABLE processed_item_dispatch_batch
DROP CONSTRAINT IF EXISTS fk_processed_item_dispatch_batch_dispatch_batch;
ALTER TABLE processed_item_dispatch_batch
DROP CONSTRAINT IF EXISTS fk_processed_item_dispatch_batch_processed_item;

-- Drop ProcessedItemDispatchBatch table
DROP TABLE IF EXISTS processed_item_dispatch_batch;

-- Drop sequence for ProcessedItemDispatchBatch
DROP SEQUENCE IF EXISTS processed_item_dispatch_batch_sequence;

-- Drop indexes from dispatch_batch
DROP INDEX IF EXISTS idx_dispatch_batch_number;
DROP INDEX IF EXISTS idx_dispatch_batch_status;
DROP INDEX IF EXISTS idx_dispatched_at;

-- Drop foreign key constraint from dispatch_batch
ALTER TABLE dispatch_batch
DROP CONSTRAINT IF EXISTS fk_dispatch_batch_tenant;

-- Drop unique constraint from dispatch_batch
ALTER TABLE dispatch_batch
DROP CONSTRAINT IF EXISTS uq_dispatch_batch_number_tenant;

-- Drop DispatchBatch table
DROP TABLE IF EXISTS dispatch_batch;

-- Drop sequence for DispatchBatch
DROP SEQUENCE IF EXISTS dispatch_batch_sequence;
