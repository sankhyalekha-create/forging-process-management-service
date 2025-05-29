-- Rollback script for V1_28__move_machine_set_to_daily_machining_batch.sql
-- This script reverses the changes made in the migration
-- WARNING: This rollback will lose data if multiple machine sets were used in daily batches

-- Step 1: Drop the index created for daily_machining_batch.machine_set_id
DROP INDEX IF EXISTS idx_daily_machining_batch_machine_set_id;

-- Step 2: Remove create_at column from machining_batch table
ALTER TABLE machining_batch 
DROP COLUMN IF EXISTS create_at;

-- Step 3: Add machine_set column back to machining_batch table
ALTER TABLE machining_batch 
ADD COLUMN machine_set BIGINT;

-- Step 4: Add foreign key constraint back to machining_batch table
ALTER TABLE machining_batch 
ADD CONSTRAINT fk_machine_set 
FOREIGN KEY (machine_set) REFERENCES machine_set(id);

-- Step 5: Migrate data back - copy machine_set_id from first daily_machining_batch to machining_batch
-- WARNING: If different daily batches used different machine sets, only the first one will be preserved
UPDATE machining_batch 
SET machine_set = (
    SELECT dmb.machine_set_id 
    FROM daily_machining_batch dmb 
    WHERE dmb.machining_batch_id = machining_batch.id 
    AND dmb.deleted = false
    ORDER BY dmb.created_at ASC 
    LIMIT 1
);

-- Step 6: Make machine_set NOT NULL after data migration (if needed)
-- Note: Uncomment the following line if machine_set was originally NOT NULL
-- ALTER TABLE machining_batch 
-- ALTER COLUMN machine_set SET NOT NULL;

-- Step 7: Drop foreign key constraint from daily_machining_batch table
ALTER TABLE daily_machining_batch 
DROP CONSTRAINT IF EXISTS fk_daily_machining_batch_machine_set;

-- Step 8: Remove machine_set_id column from daily_machining_batch table
ALTER TABLE daily_machining_batch 
DROP COLUMN IF EXISTS machine_set_id; 