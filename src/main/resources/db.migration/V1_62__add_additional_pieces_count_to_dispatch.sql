-- Migration script to add additional_pieces_count field to processed_item_dispatch_batch table
-- This field tracks the difference between actual dispatched pieces and pieces from previous operations

-- Add the new column
ALTER TABLE processed_item_dispatch_batch 
ADD COLUMN additional_pieces_count INTEGER DEFAULT 0;

-- Add comment for documentation
COMMENT ON COLUMN processed_item_dispatch_batch.additional_pieces_count IS 'Additional pieces count (positive for increment, negative for decrement) from actual dispatch vs previous operation pieces. Formula: totalDispatchPiecesCount = sum(previousOperationPieces) + additionalPiecesCount';

-- Create index for better performance on queries that might filter by additional pieces
CREATE INDEX idx_processed_item_dispatch_batch_additional_pieces ON processed_item_dispatch_batch(additional_pieces_count);

-- Update existing records to have default value of 0 (no additional pieces)
UPDATE processed_item_dispatch_batch 
SET additional_pieces_count = 0 
WHERE additional_pieces_count IS NULL;
