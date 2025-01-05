-- Drop indexes for ProcessedItem
DROP INDEX IF EXISTS idx_available_machining_batch_pieces_count_processed_item;
DROP INDEX IF EXISTS idx_machining_batch_id_processed_item;

-- Remove columns added to ProcessedItem table
ALTER TABLE processed_item
DROP CONSTRAINT IF EXISTS fk_machining_batch,
    DROP COLUMN IF EXISTS machining_batch_id,
    DROP COLUMN IF EXISTS initial_machining_batch_pieces_count,
    DROP COLUMN IF EXISTS available_machining_batch_pieces_count;
    DROP COLUMN IF EXISTS initial_rework_machining_batch_pieces_count;
    DROP COLUMN IF EXISTS available_rework_machining_batch_pieces_count;

-- Drop indexes for DailyMachiningBatchDetail
DROP INDEX IF EXISTS idx_operation_date;
DROP INDEX IF EXISTS idx_machining_batch_id;

-- Drop DailyMachiningBatchDetail table
DROP TABLE IF EXISTS daily_machining_batch_detail;

-- Drop DailyMachiningBatchDetail sequence
DROP SEQUENCE IF EXISTS daily_machining_batch_detail_sequence;

-- Drop indexes for MachiningBatch
DROP INDEX IF EXISTS idx_machine_set;
DROP INDEX IF EXISTS idx_machining_batch_number;

-- Drop MachiningBatch table
DROP TABLE IF EXISTS machining_batch;

-- Drop MachiningBatch sequence
DROP SEQUENCE IF EXISTS machining_batch_sequence;

-- Drop MachineSet-Machine join table
DROP TABLE IF EXISTS machine_set_machine;

-- Drop indexes for MachineSet
DROP INDEX IF EXISTS idx_machine_set_name;

-- Drop MachineSet table
DROP TABLE IF EXISTS machine_set;

-- Drop MachineSet sequence
DROP SEQUENCE IF EXISTS machine_set_sequence;

-- Drop indexes for Machine
DROP INDEX IF EXISTS idx_machine_name;

-- Drop Machine table
DROP TABLE IF EXISTS machine;

-- Drop Machine sequence
DROP SEQUENCE IF EXISTS machine_sequence;
