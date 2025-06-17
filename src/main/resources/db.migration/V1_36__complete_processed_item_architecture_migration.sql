-- ======================================================================
-- MIGRATION PHASE 1: ProcessedItemHeatTreatmentBatch
-- ======================================================================

BEGIN;

-- Step 1.1: Add new item_id column
ALTER TABLE processed_item_heat_treatment_batch 
ADD COLUMN item_id BIGINT;

-- Step 1.2: Add workflow columns
--ALTER TABLE processed_item_heat_treatment_batch 
--ADD COLUMN workflow_identifier VARCHAR(255),
--ADD COLUMN item_workflow_id BIGINT;

-- Step 1.3: Populate item_id from ProcessedItem relationship
UPDATE processed_item_heat_treatment_batch pihtb 
SET item_id = (
    SELECT pi.item_id 
    FROM processed_item pi 
    WHERE pi.id = pihtb.processed_item_id
)
WHERE pihtb.processed_item_id IS NOT NULL;

-- Step 1.4: Validation checkpoint
SELECT 'ProcessedItemHeatTreatmentBatch records migrated:', COUNT(*) 
FROM processed_item_heat_treatment_batch 
WHERE item_id IS NOT NULL;

SELECT 'ProcessedItemHeatTreatmentBatch records still needing migration:', COUNT(*) 
FROM processed_item_heat_treatment_batch 
WHERE processed_item_id IS NOT NULL AND item_id IS NULL;

-- Step 1.5: Create foreign key constraint
ALTER TABLE processed_item_heat_treatment_batch 
ADD CONSTRAINT fk_processed_item_heat_treatment_batch_item 
FOREIGN KEY (item_id) REFERENCES item(id);

-- Step 1.6: Create index for performance
CREATE INDEX idx_processed_item_heat_treatment_batch_item_id 
ON processed_item_heat_treatment_batch(item_id);

-- Step 1.7: Make item_id NOT NULL (after ensuring all records have values)
ALTER TABLE processed_item_heat_treatment_batch 
ALTER COLUMN item_id SET NOT NULL;

-- Step 1.8: Drop old foreign key constraint
ALTER TABLE processed_item_heat_treatment_batch 
DROP CONSTRAINT IF EXISTS fk_processed_item_heat_treatment_batch_processed_item;

-- Step 1.9: Drop old processed_item_id column
ALTER TABLE processed_item_heat_treatment_batch 
DROP COLUMN processed_item_id;

COMMIT;

-- ======================================================================
-- MIGRATION PHASE 2: ProcessedItemMachiningBatch
-- ======================================================================

BEGIN;

-- Step 2.1: Add new item_id column
ALTER TABLE processed_item_machining_batch 
ADD COLUMN item_id BIGINT;

-- Step 2.2: Add workflow columns
ALTER TABLE processed_item_machining_batch 
ADD COLUMN workflow_identifier VARCHAR(255),
ADD COLUMN item_workflow_id BIGINT;

-- Step 2.3: Populate item_id from ProcessedItem relationship
UPDATE processed_item_machining_batch pimb 
SET item_id = (
    SELECT pi.item_id 
    FROM processed_item pi 
    WHERE pi.id = pimb.processed_item_id
)
WHERE pimb.processed_item_id IS NOT NULL;

-- Step 2.4: Validation checkpoint
SELECT 'ProcessedItemMachiningBatch records migrated:', COUNT(*) 
FROM processed_item_machining_batch 
WHERE item_id IS NOT NULL;

SELECT 'ProcessedItemMachiningBatch records still needing migration:', COUNT(*) 
FROM processed_item_machining_batch 
WHERE processed_item_id IS NOT NULL AND item_id IS NULL;

-- Step 2.5: Create foreign key constraint
ALTER TABLE processed_item_machining_batch 
ADD CONSTRAINT fk_processed_item_machining_batch_item 
FOREIGN KEY (item_id) REFERENCES item(id);

-- Step 2.6: Create index for performance
CREATE INDEX idx_processed_item_machining_batch_item_id 
ON processed_item_machining_batch(item_id);

-- Step 2.7: Make item_id NOT NULL (after ensuring all records have values)
ALTER TABLE processed_item_machining_batch 
ALTER COLUMN item_id SET NOT NULL;

-- Step 2.8: Drop old foreign key constraint
ALTER TABLE processed_item_machining_batch 
DROP CONSTRAINT IF EXISTS fk_processed_item_machining_batch_processed_item;

-- Step 2.9: Drop old processed_item_id column
ALTER TABLE processed_item_machining_batch 
DROP COLUMN processed_item_id;

COMMIT;

-- ======================================================================
-- MIGRATION PHASE 3: ProcessedItemInspectionBatch
-- ======================================================================

BEGIN;

-- Step 3.1: Add new item_id column
ALTER TABLE processed_item_inspection_batch 
ADD COLUMN item_id BIGINT;

-- Step 3.2: Add workflow columns
ALTER TABLE processed_item_inspection_batch 
ADD COLUMN workflow_identifier VARCHAR(255),
ADD COLUMN item_workflow_id BIGINT;

-- Step 3.3: Populate item_id from ProcessedItem relationship
UPDATE processed_item_inspection_batch piib 
SET item_id = (
    SELECT pi.item_id 
    FROM processed_item pi 
    WHERE pi.id = piib.processed_item_id
)
WHERE piib.processed_item_id IS NOT NULL;

-- Step 3.4: Validation checkpoint
SELECT 'ProcessedItemInspectionBatch records migrated:', COUNT(*) 
FROM processed_item_inspection_batch 
WHERE item_id IS NOT NULL;

SELECT 'ProcessedItemInspectionBatch records still needing migration:', COUNT(*) 
FROM processed_item_inspection_batch 
WHERE processed_item_id IS NOT NULL AND item_id IS NULL;

-- Step 3.5: Create foreign key constraint
ALTER TABLE processed_item_inspection_batch 
ADD CONSTRAINT fk_processed_item_inspection_batch_item 
FOREIGN KEY (item_id) REFERENCES item(id);

-- Step 3.6: Create index for performance
CREATE INDEX idx_processed_item_inspection_batch_item_id 
ON processed_item_inspection_batch(item_id);

-- Step 3.7: Make item_id NOT NULL (after ensuring all records have values)
ALTER TABLE processed_item_inspection_batch 
ALTER COLUMN item_id SET NOT NULL;

-- Step 3.8: Drop old foreign key constraint
ALTER TABLE processed_item_inspection_batch 
DROP CONSTRAINT IF EXISTS fk_processed_item_inspection_batch_processed_item;

-- Step 3.9: Drop old processed_item_id column
ALTER TABLE processed_item_inspection_batch 
DROP COLUMN processed_item_id;

COMMIT;

-- ======================================================================
-- MIGRATION PHASE 4: ProcessedItemDispatchBatch
-- ======================================================================

BEGIN;

-- Step 4.1: Add new item_id column
ALTER TABLE processed_item_dispatch_batch 
ADD COLUMN item_id BIGINT;

-- Step 4.2: Add workflow columns
ALTER TABLE processed_item_dispatch_batch 
ADD COLUMN workflow_identifier VARCHAR(255),
ADD COLUMN item_workflow_id BIGINT;

-- Step 4.3: Populate item_id from ProcessedItem relationship
UPDATE processed_item_dispatch_batch pidb 
SET item_id = (
    SELECT pi.item_id 
    FROM processed_item pi 
    WHERE pi.id = pidb.processed_item_id
)
WHERE pidb.processed_item_id IS NOT NULL;

-- Step 4.4: Validation checkpoint
SELECT 'ProcessedItemDispatchBatch records migrated:', COUNT(*) 
FROM processed_item_dispatch_batch 
WHERE item_id IS NOT NULL;

SELECT 'ProcessedItemDispatchBatch records still needing migration:', COUNT(*) 
FROM processed_item_dispatch_batch 
WHERE processed_item_id IS NOT NULL AND item_id IS NULL;

-- Step 4.5: Create foreign key constraint
ALTER TABLE processed_item_dispatch_batch 
ADD CONSTRAINT fk_processed_item_dispatch_batch_item 
FOREIGN KEY (item_id) REFERENCES item(id);

-- Step 4.6: Create index for performance
CREATE INDEX idx_processed_item_dispatch_batch_item_id 
ON processed_item_dispatch_batch(item_id);

-- Step 4.7: Make item_id NOT NULL (after ensuring all records have values)
ALTER TABLE processed_item_dispatch_batch 
ALTER COLUMN item_id SET NOT NULL;

-- Step 4.8: Drop old foreign key constraint
ALTER TABLE processed_item_dispatch_batch 
DROP CONSTRAINT IF EXISTS fk_processed_item_dispatch_batch_processed_item;

-- Step 4.9: Drop old processed_item_id column
ALTER TABLE processed_item_dispatch_batch 
DROP COLUMN processed_item_id;

COMMIT;
