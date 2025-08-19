-- Migration: Fix Raw Material Unique Constraint
-- Version: V1_51
-- Description: Fix unique constraint to only apply to active (non-deleted) records
-- This prevents duplicate key violations when multiple records with same invoice number are soft-deleted

BEGIN;

-- Drop the existing problematic unique constraint
ALTER TABLE raw_material 
DROP CONSTRAINT IF EXISTS unique_raw_material_invoice_tenant_deleted;

-- Create a partial unique index that only applies to active records (deleted = false)
-- This allows multiple deleted records with the same invoice number and tenant
CREATE UNIQUE INDEX unique_raw_material_invoice_tenant_active
    ON raw_material (raw_material_invoice_number, tenant_id)
    WHERE deleted = false;

COMMIT;
