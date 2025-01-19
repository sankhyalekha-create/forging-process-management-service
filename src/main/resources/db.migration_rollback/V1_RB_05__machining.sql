-- Rollback Script

ALTER TABLE processed_item_machining_batch DROP CONSTRAINT IF EXISTS fk_machining_batch_id;

ALTER TABLE processed_item_machining_batch DROP COLUMN IF EXISTS machining_batch_id;


-- Drop Indexes for DailyMachiningBatch Table
DROP INDEX IF EXISTS idx_machining_batch_id_daily_machining_batch;

-- Drop DailyMachiningBatch Table and Sequence
DROP TABLE IF EXISTS daily_machining_batch CASCADE;
DROP SEQUENCE IF EXISTS daily_machining_batch_sequence;

-- Drop Indexes for MachiningBatch Table
DROP INDEX IF EXISTS idx_machining_batch_id_daily_machining_batch;

-- Drop MachiningBatch Table and Sequence
DROP TABLE IF EXISTS machining_batch CASCADE;
DROP SEQUENCE IF EXISTS machining_batch_sequence;

DROP INDEX IF EXISTS idx_machining_batch_number_machining_batch;
DROP INDEX IF EXISTS idx_machining_batch_number_machining_batch;
DROP INDEX IF EXISTS idx_machining_batch_number_machining_batch;

-- Drop Indexes for ProcessedItemMachiningBatch Table
DROP INDEX IF EXISTS idx_processed_item_id_processed_item_machining_batch;
DROP INDEX IF EXISTS idx_available_inspection_batch_pieces_count_processed_item_machining_batch;
DROP INDEX IF EXISTS idx_available_machining_batch_pieces_count_processed_item_machining_batch;
DROP INDEX IF EXISTS idx_reject_machining_batch_pieces_count_processed_item_machining_batch;
DROP INDEX IF EXISTS idx_rework_pieces_count_processed_item_machining_batch;

-- Drop ProcessedItemMachiningBatch Table and Sequence
DROP TABLE IF EXISTS processed_item_machining_batch CASCADE;
DROP SEQUENCE IF EXISTS processed_item_machining_batch_sequence;

-- Drop MachineSet-Machine Join Table
DROP TABLE IF EXISTS machine_set_machine;

-- Drop Indexes for MachineSet Table
DROP INDEX IF EXISTS idx_machine_set_name;

-- Drop MachineSet Table and Sequence
DROP TABLE IF EXISTS machine_set CASCADE;
DROP SEQUENCE IF EXISTS machine_set_sequence;

-- Drop Indexes for Machine Table
DROP INDEX IF EXISTS idx_machine_name;
DROP INDEX IF EXISTS idx_machine_name_tenant_id;

-- Drop Machine Table and Sequence
DROP TABLE IF EXISTS machine CASCADE;
DROP SEQUENCE IF EXISTS machine_sequence;
