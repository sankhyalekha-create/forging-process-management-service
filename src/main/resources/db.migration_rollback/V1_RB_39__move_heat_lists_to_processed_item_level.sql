-- Rollback Script for V1_39__move_heat_lists_to_processed_item_level.sql
-- This reverts the heat consumption lists from processed item level back to batch level

BEGIN;

-- ======================================================================
-- PART 1: Revert HeatTreatmentHeat to reference HeatTreatmentBatch
-- ======================================================================

-- Add back heat_treatment_batch_id column
ALTER TABLE heat_treatment_heat 
ADD COLUMN heat_treatment_batch_id BIGINT;

-- Populate the old column by mapping through the processed item relationship
UPDATE heat_treatment_heat hth
SET heat_treatment_batch_id = pihtb.heat_treatment_batch_id
FROM processed_item_heat_treatment_batch pihtb
WHERE pihtb.id = hth.processed_item_heat_treatment_batch_id;

-- Add foreign key constraint for the old column
ALTER TABLE heat_treatment_heat
ADD CONSTRAINT fk_heat_treatment_heat_heat_treatment_batch 
FOREIGN KEY (heat_treatment_batch_id) REFERENCES heat_treatment_batch(id);

-- Create index for performance
CREATE INDEX idx_heat_treatment_heat_heat_treatment_batch_id 
ON heat_treatment_heat(heat_treatment_batch_id) WHERE deleted=false;

-- Make the old column NOT NULL after population
ALTER TABLE heat_treatment_heat 
ALTER COLUMN heat_treatment_batch_id SET NOT NULL;

-- Drop new foreign key constraint and column
DROP INDEX IF EXISTS idx_heat_treatment_heat_processed_item_heat_treatment_batch_id;
ALTER TABLE heat_treatment_heat 
DROP CONSTRAINT IF EXISTS fk_heat_treatment_heat_processed_item_heat_treatment_batch;
ALTER TABLE heat_treatment_heat 
DROP COLUMN processed_item_heat_treatment_batch_id;

-- ======================================================================
-- PART 2: Revert MachiningHeat to reference MachiningBatch
-- ======================================================================

-- Add back machining_batch_id column
ALTER TABLE machining_heat 
ADD COLUMN machining_batch_id BIGINT;

-- Populate the old column by mapping through the processed item relationship
UPDATE machining_heat mh
SET machining_batch_id = pimb.machining_batch_id
FROM processed_item_machining_batch pimb
WHERE pimb.id = mh.processed_item_machining_batch_id;

-- Add foreign key constraint for the old column
ALTER TABLE machining_heat
ADD CONSTRAINT fk_machining_heat_machining_batch 
FOREIGN KEY (machining_batch_id) REFERENCES machining_batch(id);

-- Create index for performance
CREATE INDEX idx_machining_heat_machining_batch_id 
ON machining_heat(machining_batch_id) WHERE deleted=false;

-- Make the old column NOT NULL after population
ALTER TABLE machining_heat 
ALTER COLUMN machining_batch_id SET NOT NULL;

-- Drop new foreign key constraint and column
DROP INDEX IF EXISTS idx_machining_heat_processed_item_machining_batch_id;
ALTER TABLE machining_heat 
DROP CONSTRAINT IF EXISTS fk_machining_heat_processed_item_machining_batch;
ALTER TABLE machining_heat 
DROP COLUMN processed_item_machining_batch_id;

-- ======================================================================
-- PART 3: Revert InspectionHeat to reference InspectionBatch
-- ======================================================================

-- Add back inspection_batch_id column
ALTER TABLE inspection_heat 
ADD COLUMN inspection_batch_id BIGINT;

-- Populate the old column by mapping through the processed item relationship
UPDATE inspection_heat ih
SET inspection_batch_id = piib.inspection_batch_id
FROM processed_item_inspection_batch piib
WHERE piib.id = ih.processed_item_inspection_batch_id;

-- Add foreign key constraint for the old column
ALTER TABLE inspection_heat
ADD CONSTRAINT fk_inspection_heat_inspection_batch 
FOREIGN KEY (inspection_batch_id) REFERENCES inspection_batch(id);

-- Create index for performance
CREATE INDEX idx_inspection_heat_inspection_batch_id 
ON inspection_heat(inspection_batch_id) WHERE deleted=false;

-- Make the old column NOT NULL after population
ALTER TABLE inspection_heat 
ALTER COLUMN inspection_batch_id SET NOT NULL;

-- Drop new foreign key constraint and column
DROP INDEX IF EXISTS idx_inspection_heat_processed_item_inspection_batch_id;
ALTER TABLE inspection_heat 
DROP CONSTRAINT IF EXISTS fk_inspection_heat_processed_item_inspection_batch;
ALTER TABLE inspection_heat 
DROP COLUMN processed_item_inspection_batch_id;

-- ======================================================================
-- PART 4: Revert DispatchHeat to reference DispatchBatch
-- ======================================================================

-- Add back dispatch_batch_id column
ALTER TABLE dispatch_heat 
ADD COLUMN dispatch_batch_id BIGINT;

-- Populate the old column by mapping through the processed item relationship
UPDATE dispatch_heat dh
SET dispatch_batch_id = pidb.dispatch_batch_id
FROM processed_item_dispatch_batch pidb
WHERE pidb.id = dh.processed_item_dispatch_batch_id;

-- Add foreign key constraint for the old column
ALTER TABLE dispatch_heat
ADD CONSTRAINT fk_dispatch_heat_dispatch_batch 
FOREIGN KEY (dispatch_batch_id) REFERENCES dispatch_batch(id);

-- Create index for performance
CREATE INDEX idx_dispatch_heat_dispatch_batch_id 
ON dispatch_heat(dispatch_batch_id) WHERE deleted=false;

-- Make the old column NOT NULL after population
ALTER TABLE dispatch_heat 
ALTER COLUMN dispatch_batch_id SET NOT NULL;

-- Drop new foreign key constraint and column
DROP INDEX IF EXISTS idx_dispatch_heat_processed_item_dispatch_batch_id;
ALTER TABLE dispatch_heat 
DROP CONSTRAINT IF EXISTS fk_dispatch_heat_processed_item_dispatch_batch;
ALTER TABLE dispatch_heat 
DROP COLUMN processed_item_dispatch_batch_id;

COMMIT; 