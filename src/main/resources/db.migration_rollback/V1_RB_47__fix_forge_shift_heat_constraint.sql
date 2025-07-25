-- Rollback Migration: Revert forge shift heat constraint fix
-- Version: V1_RB_47
-- Description: Rollback the chk_forge_shift_heat_quantity_positive constraint modification
--              to restore the original behavior that requires heat_quantity_used > 0.0

BEGIN;

-- Drop the modified constraint that allows heat_quantity_used >= 0.0
ALTER TABLE forge_shift_heat 
DROP CONSTRAINT IF EXISTS chk_forge_shift_heat_quantity_positive;

-- Restore the original constraint that requires heat_quantity_used > 0.0
ALTER TABLE forge_shift_heat 
ADD CONSTRAINT chk_forge_shift_heat_quantity_positive 
    CHECK (heat_quantity_used > 0.0);

-- Remove the comment about allowing zero values
COMMENT ON CONSTRAINT chk_forge_shift_heat_quantity_positive ON forge_shift_heat IS NULL;

COMMIT;

-- WARNING: This rollback will fail if there are existing records with heat_quantity_used = 0.0
-- You may need to update or delete such records before running this rollback:
/*
-- Check for records that would violate the restored constraint
SELECT id, forge_shift_id, heat_id, heat_quantity_used 
FROM forge_shift_heat 
WHERE heat_quantity_used = 0.0 AND deleted = false;

-- Option 1: Update zero quantities to a small positive value
UPDATE forge_shift_heat 
SET heat_quantity_used = 0.001 
WHERE heat_quantity_used = 0.0 AND deleted = false;

-- Option 2: Soft delete records with zero quantities
UPDATE forge_shift_heat 
SET deleted = true, deleted_at = CURRENT_TIMESTAMP 
WHERE heat_quantity_used = 0.0 AND deleted = false;
*/ 