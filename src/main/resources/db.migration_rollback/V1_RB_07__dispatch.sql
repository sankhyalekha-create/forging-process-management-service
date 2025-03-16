-- Drop foreign key constraints
ALTER TABLE dispatch_processed_item_inspection DROP CONSTRAINT fk_dispatch_processed_item_inspection_dispatch_batch;
ALTER TABLE dispatch_processed_item_inspection DROP CONSTRAINT fk_dispatch_processed_item_inspection_processed_item_inspection_batch;
ALTER TABLE processed_item_dispatch_batch DROP CONSTRAINT fk_processed_item_dispatch_batch_dispatch_batch;
ALTER TABLE processed_item_dispatch_batch DROP CONSTRAINT fk_processed_item_dispatch_batch_processed_item;

-- Drop tables
DROP TABLE IF EXISTS dispatch_processed_item_inspection;
DROP TABLE IF EXISTS processed_item_dispatch_batch;
DROP TABLE IF EXISTS dispatch_batch;

-- Drop sequences
DROP SEQUENCE IF EXISTS dispatch_processed_item_inspection_sequence;
DROP SEQUENCE IF EXISTS processed_item_dispatch_batch_sequence;
DROP SEQUENCE IF EXISTS dispatch_batch_sequence;

-- Drop indexes
DROP INDEX IF EXISTS idx_dispatch_processed_item_inspection_dispatch_batch;
DROP INDEX IF EXISTS idx_dispatch_processed_item_inspection_processed_item_inspection_batch;
DROP INDEX IF EXISTS idx_dispatched_pieces_count;
DROP INDEX IF EXISTS idx_total_dispatch_pieces_count_processed_item_dispatch_batch;
DROP INDEX IF EXISTS idx_item_status_processed_item_dispatch_batch;
DROP INDEX IF EXISTS idx_dispatch_batch_number;
DROP INDEX IF EXISTS idx_dispatch_batch_status;
DROP INDEX IF EXISTS idx_dispatched_at;

-- Remove column from processed_item_inspection_batch
ALTER TABLE processed_item_inspection_batch DROP COLUMN IF EXISTS dispatched_pieces_count;
