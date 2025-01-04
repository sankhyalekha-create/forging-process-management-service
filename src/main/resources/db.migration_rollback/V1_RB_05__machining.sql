-- Drop indexes
DROP INDEX IF EXISTS idx_processed_item_machining_batch_id;
DROP INDEX IF EXISTS idx_piece_status;
DROP INDEX IF EXISTS idx_machining_batch_number;
DROP INDEX IF EXISTS idx_machining_batch_status;
DROP INDEX IF EXISTS idx_machine_set_name;
DROP INDEX IF EXISTS idx_machine_name;

-- Remove the foreign key constraint and column from processed_item
ALTER TABLE processed_item
DROP CONSTRAINT IF EXISTS fk_machining_batch_in_processed_item,
    DROP COLUMN IF EXISTS machining_batch_id;

-- Drop the machining_batch_piece_detail table
DROP TABLE IF EXISTS machining_batch_piece_detail;

-- Drop the machining_batch table
DROP TABLE IF EXISTS machining_batch;

-- Drop the machine_set_machine join table
DROP TABLE IF EXISTS machine_set_machine;

-- Drop the machine_set table
DROP TABLE IF EXISTS machine_set;

-- Drop the machine table
DROP TABLE IF EXISTS machine;

-- Drop sequences
DROP SEQUENCE IF EXISTS piece_detail_sequence;
DROP SEQUENCE IF EXISTS machining_batch_sequence;
DROP SEQUENCE IF EXISTS machine_set_sequence;
DROP SEQUENCE IF EXISTS machine_sequence;
