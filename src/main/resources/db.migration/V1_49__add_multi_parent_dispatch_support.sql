-- Migration script for supporting multiple parent entities in dispatch batches
-- This script adds the new table and columns to support multi-parent dispatch functionality

-- 1. Create the new dispatch_processed_item_consumption table
CREATE TABLE dispatch_processed_item_consumption (
    id BIGINT NOT NULL,
    dispatch_batch_id BIGINT NOT NULL,
    previous_operation_entity_id BIGINT NOT NULL,
    previous_operation_type VARCHAR(50) NOT NULL,
    consumed_pieces_count INTEGER NOT NULL,
    available_pieces_count INTEGER,
    batch_identifier VARCHAR(255),
    entity_context VARCHAR(500),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    
    PRIMARY KEY (id),
    CONSTRAINT fk_dispatch_consumption_batch FOREIGN KEY (dispatch_batch_id) REFERENCES dispatch_batch(id)
);

-- 2. Create sequence for the new table
CREATE SEQUENCE dispatch_processed_item_consumption_sequence START WITH 1 INCREMENT BY 1;

-- 3. Add new columns to processed_item_dispatch_batch table
ALTER TABLE processed_item_dispatch_batch 
ADD COLUMN total_parent_entities_count INTEGER,
ADD COLUMN is_multi_parent_dispatch BOOLEAN DEFAULT FALSE;

-- 4. Create indexes for better performance
CREATE INDEX idx_dispatch_consumption_batch_id ON dispatch_processed_item_consumption(dispatch_batch_id);
CREATE INDEX idx_dispatch_consumption_entity_id ON dispatch_processed_item_consumption(previous_operation_entity_id);
CREATE INDEX idx_dispatch_consumption_operation_type ON dispatch_processed_item_consumption(previous_operation_type);
CREATE INDEX idx_dispatch_consumption_deleted ON dispatch_processed_item_consumption(deleted);

-- 5. Update existing data to set default values for new fields
UPDATE processed_item_dispatch_batch 
SET 
    total_parent_entities_count = CASE 
        WHEN previous_operation_processed_item_id IS NOT NULL THEN 1 
        ELSE 0 
    END,
    is_multi_parent_dispatch = FALSE
WHERE total_parent_entities_count IS NULL;

-- 6. Migrate existing inspection-based dispatch data to new consumption structure
-- This creates consumption records for existing dispatch batches that use inspection data
INSERT INTO dispatch_processed_item_consumption (
    id,
    dispatch_batch_id,
    previous_operation_entity_id,
    previous_operation_type,
    consumed_pieces_count,
    available_pieces_count,
    batch_identifier,
    entity_context,
    created_at,
    updated_at,
    deleted
)
SELECT 
    NEXTVAL('dispatch_processed_item_consumption_sequence'),
    dpi.dispatch_batch_id,
    dpi.processed_item_inspection_batch_id,
    'QUALITY',
    dpi.dispatched_pieces_count,
    piib.available_dispatch_pieces_count,
    CONCAT('InspectionBatch-', dpi.processed_item_inspection_batch_id),
    CONCAT('Migrated from inspection batch with ', piib.available_dispatch_pieces_count, ' available pieces'),
    dpi.created_at,
    dpi.updated_at,
    dpi.deleted
FROM dispatch_processed_item_inspection dpi
JOIN processed_item_inspection_batch piib ON dpi.processed_item_inspection_batch_id = piib.id
WHERE dpi.deleted = FALSE;

-- 7. Add comments for documentation
COMMENT ON TABLE dispatch_processed_item_consumption IS 'Tracks pieces consumed from previous operations for dispatch batches. Supports multiple parent operations per dispatch.';
COMMENT ON COLUMN dispatch_processed_item_consumption.previous_operation_entity_id IS 'ID of the entity from the previous operation (e.g., ProcessedItemInspectionBatch, ProcessedItemMachiningBatch, etc.)';
COMMENT ON COLUMN dispatch_processed_item_consumption.previous_operation_type IS 'Type of the previous operation (FORGING, HEAT_TREATMENT, MACHINING, QUALITY, VENDOR, DISPATCH)';
COMMENT ON COLUMN dispatch_processed_item_consumption.consumed_pieces_count IS 'Number of pieces consumed from this previous operation for the dispatch';
COMMENT ON COLUMN dispatch_processed_item_consumption.batch_identifier IS 'Human-readable identifier for the batch (e.g., InspectionBatch-123)';

COMMENT ON COLUMN processed_item_dispatch_batch.total_parent_entities_count IS 'Total number of parent entities consumed for this dispatch batch';
COMMENT ON COLUMN processed_item_dispatch_batch.is_multi_parent_dispatch IS 'Indicates if this dispatch batch consumes from multiple parent entities';

-- 8. Grant necessary permissions (adjust based on your user setup)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON dispatch_processed_item_consumption TO your_app_user;
-- GRANT USAGE, SELECT ON dispatch_processed_item_consumption_sequence TO your_app_user;