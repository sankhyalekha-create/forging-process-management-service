-- Rollback Script for HeatTreatmentBatch

-- Drop Table HeatTreatmentBatch
DROP TABLE IF EXISTS heat_treatment_batch;

-- Drop Sequence for HeatTreatmentBatch
DROP SEQUENCE IF EXISTS heat_treatment_batch_sequence;

DROP INDEX IF EXISTS idx_heat_treatment_batch_furnace_id;

-- Drop indexes for Furnace table
DROP INDEX IF EXISTS idx_furnace_name;

-- Drop Furnace table and sequence
DROP TABLE IF EXISTS furnace;
DROP SEQUENCE IF EXISTS furnace_sequence;
