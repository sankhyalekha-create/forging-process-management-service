-- Migration: Fix vendor_dispatch_heat quantity_used column type to match application pattern
-- Version: V1_53
-- Description: Change quantity_used from DECIMAL(10,3) to DOUBLE PRECISION to align with
--              the standard pattern used throughout the application for quantity fields

BEGIN;

-- Change the column type from DECIMAL(10,3) to DOUBLE PRECISION
-- This aligns with the pattern used for all other quantity fields in the application:
-- - heat.heat_quantity (DOUBLE PRECISION)
-- - forge_heat.heat_quantity_used (DOUBLE PRECISION) 
-- - forge_shift_heat.heat_quantity_used (DOUBLE PRECISION)
-- - vendor_inventory columns (mapped to Double without precision/scale)
ALTER TABLE vendor_dispatch_heat 
ALTER COLUMN quantity_used TYPE DOUBLE PRECISION;

-- Add comment to document the alignment with application pattern
COMMENT ON COLUMN vendor_dispatch_heat.quantity_used IS 
    'Heat quantity consumed - aligned with standard DOUBLE PRECISION pattern used for all quantity fields';

COMMIT;
