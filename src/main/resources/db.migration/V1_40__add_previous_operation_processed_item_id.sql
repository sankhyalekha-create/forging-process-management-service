-- ======================================================================
-- Add previous_operation_processed_item_id to ProcessedItem tables
-- ======================================================================

BEGIN;

-- Add previous_operation_processed_item_id column to processed_item_heat_treatment_batch table
ALTER TABLE processed_item_heat_treatment_batch 
ADD COLUMN IF NOT EXISTS previous_operation_processed_item_id BIGINT;

-- Add previous_operation_processed_item_id column to processed_item_machining_batch table
ALTER TABLE processed_item_machining_batch 
ADD COLUMN IF NOT EXISTS previous_operation_processed_item_id BIGINT;

-- Add previous_operation_processed_item_id column to processed_item_inspection_batch table
ALTER TABLE processed_item_inspection_batch 
ADD COLUMN IF NOT EXISTS previous_operation_processed_item_id BIGINT;

-- Add previous_operation_processed_item_id column to processed_item_dispatch_batch table
ALTER TABLE processed_item_dispatch_batch 
ADD COLUMN IF NOT EXISTS previous_operation_processed_item_id BIGINT;

-- Add indexes for performance on the new columns
CREATE INDEX IF NOT EXISTS idx_processed_item_heat_treatment_batch_previous_operation 
ON processed_item_heat_treatment_batch(previous_operation_processed_item_id);

CREATE INDEX IF NOT EXISTS idx_processed_item_machining_batch_previous_operation 
ON processed_item_machining_batch(previous_operation_processed_item_id);

CREATE INDEX IF NOT EXISTS idx_processed_item_inspection_batch_previous_operation 
ON processed_item_inspection_batch(previous_operation_processed_item_id);

CREATE INDEX IF NOT EXISTS idx_processed_item_dispatch_batch_previous_operation 
ON processed_item_dispatch_batch(previous_operation_processed_item_id);

-- Add comments for the new columns
COMMENT ON COLUMN processed_item_heat_treatment_batch.previous_operation_processed_item_id IS 'ID of the processed item from the previous operation that was used for this heat treatment (enables precise traceability in workflow)';

COMMENT ON COLUMN processed_item_machining_batch.previous_operation_processed_item_id IS 'ID of the processed item from the previous operation that was used for this machining (enables precise traceability in workflow)';

COMMENT ON COLUMN processed_item_inspection_batch.previous_operation_processed_item_id IS 'ID of the processed item from the previous operation that was used for this inspection (enables precise traceability in workflow)';

COMMENT ON COLUMN processed_item_dispatch_batch.previous_operation_processed_item_id IS 'ID of the processed item from the previous operation that was used for this dispatch (enables precise traceability in workflow)';

COMMIT; 