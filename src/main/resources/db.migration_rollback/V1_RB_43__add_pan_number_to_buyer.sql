-- Drop indexes for PAN number fields
DROP INDEX IF EXISTS idx_buyer_pan_number;
DROP INDEX IF EXISTS idx_buyer_entity_pan_number;

-- Remove PAN number field from buyer_entity table
ALTER TABLE buyer_entity
DROP COLUMN IF EXISTS pan_number;

-- Remove PAN number field from buyer table
ALTER TABLE buyer
DROP COLUMN IF EXISTS pan_number; 