-- Rollback script for V1_30__daily_machining_batch_inspection_distribution.sql
-- This script undoes the creation of daily_machining_batch_inspection_distribution table and related objects

-- Drop indexes first (they depend on the table)
DROP INDEX IF EXISTS idx_daily_machining_batch_inspection_distribution_daily_machining_batch;
DROP INDEX IF EXISTS idx_daily_machining_batch_inspection_distribution_processed_item_inspection_batch;

-- Drop the table (it depends on the sequence)
DROP TABLE IF EXISTS daily_machining_batch_inspection_distribution;

-- Drop the sequence last
DROP SEQUENCE IF EXISTS daily_machining_batch_inspection_distribution_sequence; 