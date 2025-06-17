-- Rollback Script for V1_38__create_dispatch_heat_table.sql

-- Drop Indexes for dispatch_heat
DROP INDEX IF EXISTS idx_dispatch_heat_heat_id;
DROP INDEX IF EXISTS idx_dispatch_heat_dispatch_batch_id;

-- Drop Tables
DROP TABLE IF EXISTS dispatch_heat;

-- Drop Sequences
DROP SEQUENCE IF EXISTS dispatch_heat_sequence; 