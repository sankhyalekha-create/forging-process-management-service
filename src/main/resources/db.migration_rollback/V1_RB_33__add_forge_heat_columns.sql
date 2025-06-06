-- Rollback script for V1_34__add_forge_heat_columns.sql
-- This removes the heat_quantity_returned column and related database objects

-- Drop the index for returned quantities
DROP INDEX IF EXISTS idx_forge_heat_returned_quantity;

-- Drop the constraint for returned quantity validation
ALTER TABLE forge_heat 
DROP CONSTRAINT IF EXISTS chk_forge_heat_returned_quantity_positive;

-- Drop the heat_quantity_returned column (this also removes the column comment)
ALTER TABLE forge_heat 
DROP COLUMN IF EXISTS heat_quantity_returned; 