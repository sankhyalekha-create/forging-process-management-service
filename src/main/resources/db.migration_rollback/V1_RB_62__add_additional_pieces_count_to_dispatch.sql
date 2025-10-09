-- Rollback script for V1_62__add_additional_pieces_count_to_dispatch.sql
-- This script removes the additional_pieces_count field from processed_item_dispatch_batch table

-- Drop the index first
DROP INDEX IF EXISTS idx_processed_item_dispatch_batch_additional_pieces;

-- Remove the comment
COMMENT ON COLUMN processed_item_dispatch_batch.additional_pieces_count IS NULL;

-- Drop the column
ALTER TABLE processed_item_dispatch_batch 
DROP COLUMN IF EXISTS additional_pieces_count;
