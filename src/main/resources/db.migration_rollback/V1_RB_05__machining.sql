-- Drop Indexes for DailyMachiningBatch
DROP INDEX IF EXISTS idx_machining_batch_id_daily_machining_batch;

-- Drop DailyMachiningBatch Table
DROP TABLE IF EXISTS daily_machining_batch;

-- Drop Sequence for DailyMachiningBatch
DROP SEQUENCE IF EXISTS daily_machining_batch_sequence;

-- Drop Indexes for ProcessedItemMachiningBatch
DROP INDEX IF EXISTS idx_processed_item_id_processed_item_machining_batch;
DROP INDEX IF EXISTS idx_machining_batch_id_processed_item_machining_batch;
DROP INDEX IF EXISTS idx_available_inspection_batch_pieces_count_processed_item_machining_batch;

-- Drop ProcessedItemMachiningBatch Table
DROP TABLE IF EXISTS processed_item_machining_batch;

-- Drop Sequence for ProcessedItemMachiningBatch
DROP SEQUENCE IF EXISTS processed_item_machining_batch_sequence;

-- Drop MachiningBatch Table
DROP TABLE IF EXISTS machining_batch;

-- Drop Sequence for MachiningBatch
DROP SEQUENCE IF EXISTS machining_batch_sequence;

-- Drop MachineSet-Machine Join Table
DROP TABLE IF EXISTS machine_set_machine;

-- Drop Indexes for MachineSet
DROP INDEX IF EXISTS idx_machine_set_name;

-- Drop MachineSet Table
DROP TABLE IF EXISTS machine_set;

-- Drop Sequence for MachineSet
DROP SEQUENCE IF EXISTS machine_set_sequence;

-- Drop Indexes for Machine Table
DROP INDEX IF EXISTS idx_machine_name;
DROP INDEX IF EXISTS idx_machine_name_tenant_id;

-- Drop Machine Table
DROP TABLE IF EXISTS machine;

-- Drop Sequence for Machine
DROP SEQUENCE IF EXISTS machine_sequence;
