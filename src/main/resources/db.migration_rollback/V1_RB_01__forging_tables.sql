-- Drop the heat_treatment_batch_forge_traceability join table
DROP TABLE IF EXISTS heat_treatment_batch_forge_traceability;

-- Drop the heat_treatment_batch table
DROP INDEX IF EXISTS idx_heat_treatment_furnace_id;
DROP TABLE IF EXISTS heat_treatment_batch;

-- Drop indexes and forge_traceability table
DROP INDEX IF EXISTS idx_forge_traceability_heat_id;
DROP INDEX IF EXISTS idx_forge_traceability_forging_line_id;
DROP TABLE IF EXISTS forge_traceability;

-- Drop indexes and forging_line table
DROP INDEX IF EXISTS idx_forging_line_name;
DROP TABLE IF EXISTS forging_line;

-- Drop indexes and furnace table
DROP INDEX IF EXISTS idx_furnace_name;
DROP TABLE IF EXISTS furnace;
