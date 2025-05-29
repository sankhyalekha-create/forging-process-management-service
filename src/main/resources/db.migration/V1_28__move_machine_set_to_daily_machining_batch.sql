-- Migration to move machine_set association from machining_batch to daily_machining_batch
-- This allows each daily machining batch to have its own machine set for more flexibility
-- Also adds create_at column to machining_batch table

-- Step 1: Add machine_set_id column to daily_machining_batch table
ALTER TABLE daily_machining_batch 
ADD COLUMN machine_set_id BIGINT;

-- Step 2: Add foreign key constraint for machine_set_id
ALTER TABLE daily_machining_batch 
ADD CONSTRAINT fk_daily_machining_batch_machine_set 
FOREIGN KEY (machine_set_id) REFERENCES machine_set(id);

-- Step 3: Migrate existing data - copy machine_set from machining_batch to all its daily_machining_batch records
UPDATE daily_machining_batch 
SET machine_set_id = (
    SELECT mb.machine_set 
    FROM machining_batch mb 
    WHERE mb.id = daily_machining_batch.machining_batch_id
);

-- Step 4: Make machine_set_id NOT NULL after data migration
ALTER TABLE daily_machining_batch 
ALTER COLUMN machine_set_id SET NOT NULL;

-- Step 5: Drop the foreign key constraint from machining_batch table
ALTER TABLE machining_batch 
DROP CONSTRAINT fk_machine_set;

-- Step 6: Remove machine_set column from machining_batch table
ALTER TABLE machining_batch 
DROP COLUMN machine_set;

-- Step 7: Rename apply_at column to create_at in machining_batch table
ALTER TABLE machining_batch 
RENAME COLUMN apply_at TO create_at;

-- Step 8: Create index for better query performance
CREATE INDEX idx_daily_machining_batch_machine_set_id 
ON daily_machining_batch(machine_set_id) 
WHERE deleted = false; 