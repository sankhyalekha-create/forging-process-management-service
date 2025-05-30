-- Create Sequence for DailyMachiningBatchInspectionDistribution
CREATE SEQUENCE daily_machining_batch_inspection_distribution_sequence START WITH 1 INCREMENT BY 1;

-- Create DailyMachiningBatchInspectionDistribution Table
CREATE TABLE daily_machining_batch_inspection_distribution (
    id BIGINT PRIMARY KEY DEFAULT nextval('daily_machining_batch_inspection_distribution_sequence'),
    processed_item_inspection_batch_id BIGINT NOT NULL,
    daily_machining_batch_id BIGINT NOT NULL,
    rejected_pieces_count INTEGER NOT NULL DEFAULT 0,
    rework_pieces_count INTEGER NOT NULL DEFAULT 0,
    original_completed_pieces_count INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_daily_machining_batch_inspection_distribution_processed_item_inspection_batch 
        FOREIGN KEY (processed_item_inspection_batch_id) 
        REFERENCES processed_item_inspection_batch(id) ON DELETE CASCADE,
    CONSTRAINT fk_daily_machining_batch_inspection_distribution_daily_machining_batch 
        FOREIGN KEY (daily_machining_batch_id) 
        REFERENCES daily_machining_batch(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_daily_machining_batch_inspection_distribution_processed_item_inspection_batch 
    ON daily_machining_batch_inspection_distribution(processed_item_inspection_batch_id) 
    WHERE deleted = false;

CREATE INDEX idx_daily_machining_batch_inspection_distribution_daily_machining_batch 
    ON daily_machining_batch_inspection_distribution(daily_machining_batch_id) 
    WHERE deleted = false; 