-- Migration: Fix forge shift heat constraint to allow zero heat quantity
-- Version: V1_47
-- Description: Modify the chk_forge_shift_heat_quantity_positive constraint to allow zero values
--              for heat_quantity_used, as it's valid for a heat to not be used in a forge shift.

BEGIN;

-- Drop the existing constraint that requires heat_quantity_used > 0.0
ALTER TABLE forge_shift_heat 
DROP CONSTRAINT IF EXISTS chk_forge_shift_heat_quantity_positive;

-- Add the new constraint that allows heat_quantity_used >= 0.0 (including zero)
ALTER TABLE forge_shift_heat 
ADD CONSTRAINT chk_forge_shift_heat_quantity_positive 
    CHECK (heat_quantity_used >= 0.0);

-- Add a comment to document the reason for this change
COMMENT ON CONSTRAINT chk_forge_shift_heat_quantity_positive ON forge_shift_heat IS 
    'Allows zero heat quantity usage - valid when a heat is allocated but not actually used in a forge shift';

COMMIT;

-- Verification query (for manual testing - uncomment to run)
/*
-- Test that zero values are now allowed
INSERT INTO forge_shift_heat (
    forge_shift_id, 
    heat_id, 
    heat_quantity_used, 
    heat_pieces, 
    created_at, 
    updated_at
) VALUES (
    1,  -- Replace with actual forge_shift_id
    1,  -- Replace with actual heat_id
    0.0,  -- Zero quantity should now be allowed
    0,    -- Zero pieces
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
-- Remember to rollback this test insert
*/ 