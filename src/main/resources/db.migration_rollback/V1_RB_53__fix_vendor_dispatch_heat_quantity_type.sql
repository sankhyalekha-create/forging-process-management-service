-- Rollback script for V1_53__fix_vendor_dispatch_heat_quantity_type.sql
-- This reverts the quantity_used column back to DECIMAL(10,3) type

BEGIN;

-- Revert the column type from DOUBLE PRECISION back to DECIMAL(10,3)
-- This restores the original column type definition
ALTER TABLE vendor_dispatch_heat 
ALTER COLUMN quantity_used TYPE DECIMAL(10,3);

-- Remove the comment added by the migration
COMMENT ON COLUMN vendor_dispatch_heat.quantity_used IS NULL;

COMMIT;
