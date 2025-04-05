-- Rollback Script

-- Drop Indexes for machining_heat
DROP INDEX IF EXISTS idx_machining_heat_heat_id;
DROP INDEX IF EXISTS idx_machining_heat_machining_batch_id;

-- Drop Tables
DROP TABLE IF EXISTS machining_heat;

-- Drop Sequences
DROP SEQUENCE IF EXISTS machining_heat_sequence;
