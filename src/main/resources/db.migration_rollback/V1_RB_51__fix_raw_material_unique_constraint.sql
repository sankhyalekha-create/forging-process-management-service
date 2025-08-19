-- Rollback Migration: Fix Raw Material Unique Constraint
-- Version: V1_RB_51
-- Description: Rollback changes from V1_51 - restore original unique constraint with deleted column
-- This reverts the partial unique index back to the problematic unique constraint

BEGIN;

-- Drop the partial unique index created in V1_51
DROP INDEX IF EXISTS unique_raw_material_invoice_tenant_active;

-- Restore the original problematic unique constraint that includes deleted column
ALTER TABLE raw_material 
ADD CONSTRAINT unique_raw_material_invoice_tenant_deleted 
UNIQUE (raw_material_invoice_number, tenant_id, deleted);

COMMIT;
