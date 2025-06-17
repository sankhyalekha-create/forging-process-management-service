-- Migration to move heat consumption lists from batch level to processed item level
-- This involves updating foreign keys in the heat tables to reference processed items instead of batches

BEGIN;

-- ======================================================================
-- PART 1: Update HeatTreatmentHeat to reference ProcessedItemHeatTreatmentBatch
-- ======================================================================

-- Add new column for processed_item_heat_treatment_batch_id
ALTER TABLE heat_treatment_heat 
ADD COLUMN processed_item_heat_treatment_batch_id BIGINT;

-- Populate the new column by mapping through the heat_treatment_batch relationship
UPDATE heat_treatment_heat hth
SET processed_item_heat_treatment_batch_id = pihtb.id
FROM processed_item_heat_treatment_batch pihtb
WHERE pihtb.heat_treatment_batch_id = hth.heat_treatment_batch_id;

-- Add foreign key constraint for the new column
ALTER TABLE heat_treatment_heat
ADD CONSTRAINT fk_heat_treatment_heat_processed_item_heat_treatment_batch 
FOREIGN KEY (processed_item_heat_treatment_batch_id) REFERENCES processed_item_heat_treatment_batch(id);

-- Create index for performance
CREATE INDEX idx_heat_treatment_heat_processed_item_heat_treatment_batch_id 
ON heat_treatment_heat(processed_item_heat_treatment_batch_id) WHERE deleted=false;

-- Make the new column NOT NULL after population
ALTER TABLE heat_treatment_heat 
ALTER COLUMN processed_item_heat_treatment_batch_id SET NOT NULL;

-- Drop old foreign key constraint and column
DROP INDEX IF EXISTS idx_heat_treatment_heat_heat_treatment_batch_id;
ALTER TABLE heat_treatment_heat 
DROP CONSTRAINT IF EXISTS fk_heat_treatment_heat_heat_treatment_batch;
ALTER TABLE heat_treatment_heat 
DROP COLUMN heat_treatment_batch_id;

-- ======================================================================
-- PART 2: Update MachiningHeat to reference ProcessedItemMachiningBatch
-- ======================================================================

-- Add new column for processed_item_machining_batch_id
ALTER TABLE machining_heat 
ADD COLUMN processed_item_machining_batch_id BIGINT;

-- Populate the new column by mapping through the machining_batch relationship
UPDATE machining_heat mh
SET processed_item_machining_batch_id = pimb.id
FROM processed_item_machining_batch pimb
WHERE pimb.machining_batch_id = mh.machining_batch_id;

-- Add foreign key constraint for the new column
ALTER TABLE machining_heat
ADD CONSTRAINT fk_machining_heat_processed_item_machining_batch 
FOREIGN KEY (processed_item_machining_batch_id) REFERENCES processed_item_machining_batch(id);

-- Create index for performance
CREATE INDEX idx_machining_heat_processed_item_machining_batch_id 
ON machining_heat(processed_item_machining_batch_id) WHERE deleted=false;

-- Make the new column NOT NULL after population
ALTER TABLE machining_heat 
ALTER COLUMN processed_item_machining_batch_id SET NOT NULL;

-- Drop old foreign key constraint and column
DROP INDEX IF EXISTS idx_machining_heat_machining_batch_id;
ALTER TABLE machining_heat 
DROP CONSTRAINT IF EXISTS fk_machining_heat_machining_batch;
ALTER TABLE machining_heat 
DROP COLUMN machining_batch_id;

-- ======================================================================
-- PART 3: Update InspectionHeat to reference ProcessedItemInspectionBatch
-- ======================================================================

-- Add new column for processed_item_inspection_batch_id
ALTER TABLE inspection_heat 
ADD COLUMN processed_item_inspection_batch_id BIGINT;

-- Populate the new column by mapping through the inspection_batch relationship
UPDATE inspection_heat ih
SET processed_item_inspection_batch_id = piib.id
FROM processed_item_inspection_batch piib
WHERE piib.inspection_batch_id = ih.inspection_batch_id;

-- Add foreign key constraint for the new column
ALTER TABLE inspection_heat
ADD CONSTRAINT fk_inspection_heat_processed_item_inspection_batch 
FOREIGN KEY (processed_item_inspection_batch_id) REFERENCES processed_item_inspection_batch(id);

-- Create index for performance
CREATE INDEX idx_inspection_heat_processed_item_inspection_batch_id 
ON inspection_heat(processed_item_inspection_batch_id) WHERE deleted=false;

-- Make the new column NOT NULL after population
ALTER TABLE inspection_heat 
ALTER COLUMN processed_item_inspection_batch_id SET NOT NULL;

-- Drop old foreign key constraint and column
DROP INDEX IF EXISTS idx_inspection_heat_inspection_batch_id;
ALTER TABLE inspection_heat 
DROP CONSTRAINT IF EXISTS fk_inspection_heat_inspection_batch;
ALTER TABLE inspection_heat 
DROP COLUMN inspection_batch_id;

-- ======================================================================
-- PART 4: Update DispatchHeat to reference ProcessedItemDispatchBatch
-- ======================================================================

-- Add new column for processed_item_dispatch_batch_id
ALTER TABLE dispatch_heat 
ADD COLUMN processed_item_dispatch_batch_id BIGINT;

-- Populate the new column by mapping through the dispatch_batch relationship
UPDATE dispatch_heat dh
SET processed_item_dispatch_batch_id = pidb.id
FROM processed_item_dispatch_batch pidb
WHERE pidb.dispatch_batch_id = dh.dispatch_batch_id;

-- Add foreign key constraint for the new column
ALTER TABLE dispatch_heat
ADD CONSTRAINT fk_dispatch_heat_processed_item_dispatch_batch 
FOREIGN KEY (processed_item_dispatch_batch_id) REFERENCES processed_item_dispatch_batch(id);

-- Create index for performance
CREATE INDEX idx_dispatch_heat_processed_item_dispatch_batch_id 
ON dispatch_heat(processed_item_dispatch_batch_id) WHERE deleted=false;

-- Make the new column NOT NULL after population
ALTER TABLE dispatch_heat 
ALTER COLUMN processed_item_dispatch_batch_id SET NOT NULL;

-- Drop old foreign key constraint and column
DROP INDEX IF EXISTS idx_dispatch_heat_dispatch_batch_id;
ALTER TABLE dispatch_heat 
DROP CONSTRAINT IF EXISTS fk_dispatch_heat_dispatch_batch;
ALTER TABLE dispatch_heat 
DROP COLUMN dispatch_batch_id;

COMMIT; 