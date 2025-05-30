-- Rollback script for V1_31__add_daily_machining_batch_number.sql
-- Author: System Generated
-- Description: Rollback the addition of daily_machining_batch_number field

-- Drop the performance indexes first
DROP INDEX IF EXISTS idx_daily_machining_batch_number;
DROP INDEX IF EXISTS idx_daily_machining_batch_machining_batch_id;

-- Drop the unique constraint
ALTER TABLE daily_machining_batch 
DROP CONSTRAINT IF EXISTS uk_daily_machining_batch_number_machining_batch_id_deleted;

-- Drop the daily_machining_batch_number column
-- This will automatically remove all data stored in this column
ALTER TABLE daily_machining_batch 
DROP COLUMN IF EXISTS daily_machining_batch_number; 