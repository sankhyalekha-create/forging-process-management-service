-- Drop indexes for the new fields
DROP INDEX IF EXISTS idx_supplier_phone_number;
DROP INDEX IF EXISTS idx_supplier_pan_number;
DROP INDEX IF EXISTS idx_supplier_gstin_number;

-- Remove new fields from supplier table
ALTER TABLE supplier
    DROP COLUMN IF EXISTS phone_number,
    DROP COLUMN IF EXISTS pan_number,
    DROP COLUMN IF EXISTS gstin_number; 