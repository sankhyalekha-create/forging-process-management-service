-- Migration script to add daily_machining_batch_number column to daily_machining_batch table
-- Author: System Generated
-- Description: Add daily_machining_batch_number field to identify each daily machining batch within a machining batch

-- Add the daily_machining_batch_number column
ALTER TABLE daily_machining_batch 
ADD COLUMN daily_machining_batch_number VARCHAR(255) NOT NULL DEFAULT 'TEMP';

-- Update existing records with a meaningful pattern: <MachiningBatchNumber>-Shift-<counter>
-- This ensures proper identification within the context of each machining batch
WITH numbered_batches AS (
    SELECT 
        dmb.id,
        CONCAT(
            mb.machining_batch_number, 
            '-Shift-', 
            ROW_NUMBER() OVER (PARTITION BY dmb.machining_batch_id ORDER BY dmb.id)
        ) AS new_batch_number
    FROM daily_machining_batch dmb
    INNER JOIN machining_batch mb ON dmb.machining_batch_id = mb.id
    WHERE dmb.daily_machining_batch_number = 'TEMP'
)
UPDATE daily_machining_batch 
SET daily_machining_batch_number = numbered_batches.new_batch_number
FROM numbered_batches
WHERE daily_machining_batch.id = numbered_batches.id;

-- Remove the default value since new records should explicitly set this field
ALTER TABLE daily_machining_batch 
ALTER COLUMN daily_machining_batch_number DROP DEFAULT;

-- Create the unique constraint for daily_machining_batch_number, machining_batch_id, and deleted
ALTER TABLE daily_machining_batch 
ADD CONSTRAINT uk_daily_machining_batch_number_machining_batch_id_deleted 
UNIQUE (daily_machining_batch_number, machining_batch_id, deleted);

-- Add index for better query performance on daily_machining_batch_number
CREATE INDEX idx_daily_machining_batch_number 
ON daily_machining_batch(daily_machining_batch_number);

-- Add index for better query performance on machining_batch_id (if not already exists)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'daily_machining_batch' 
        AND indexname = 'idx_daily_machining_batch_machining_batch_id'
    ) THEN
        CREATE INDEX idx_daily_machining_batch_machining_batch_id 
        ON daily_machining_batch(machining_batch_id);
    END IF;
END $$; 