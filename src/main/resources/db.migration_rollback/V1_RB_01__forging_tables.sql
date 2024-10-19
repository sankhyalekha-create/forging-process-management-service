-- Drop Indexes
DROP INDEX IF EXISTS idx_furnace_name;
DROP INDEX IF EXISTS idx_forging_line_name;
DROP INDEX IF EXISTS idx_forge_tracebility_heat_id;
DROP INDEX IF EXISTS idx_forge_tracebility_forging_line_id;
DROP INDEX IF EXISTS idx_heat_treatment_batch_status;

-- Drop Tables
DROP TABLE IF EXISTS heat_treatment_batch CASCADE;
DROP TABLE IF EXISTS forge_tracebility CASCADE;
DROP TABLE IF EXISTS forging_line CASCADE;
DROP TABLE IF EXISTS furnace CASCADE;
