-- Rollback Script for V1_36__create_inspection_heat_table.sql

-- Drop Indexes for inspection_heat
DROP INDEX IF EXISTS idx_inspection_heat_heat_id;
DROP INDEX IF EXISTS idx_inspection_heat_inspection_batch_id;

-- Drop Tables
DROP TABLE IF EXISTS inspection_heat;

-- Drop Sequences
DROP SEQUENCE IF EXISTS inspection_heat_sequence; 