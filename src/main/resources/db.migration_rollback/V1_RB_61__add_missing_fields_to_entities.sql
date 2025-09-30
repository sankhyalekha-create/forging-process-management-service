-- Rollback migration: V1_RB_61__add_missing_fields_to_entities.sql
-- Remove the fields added in V1_61

-- Remove email, stateCode, and pincode from buyer table
ALTER TABLE buyer 
DROP COLUMN IF EXISTS email,
DROP COLUMN IF EXISTS state_code,
DROP COLUMN IF EXISTS pincode;

-- Remove email, stateCode, and pincode from buyer_entity table
ALTER TABLE buyer_entity 
DROP COLUMN IF EXISTS email,
DROP COLUMN IF EXISTS state_code,
DROP COLUMN IF EXISTS pincode;

-- Remove fields from tenant table
ALTER TABLE tenant 
DROP COLUMN IF EXISTS state_code,
DROP COLUMN IF EXISTS pincode;

-- Remove fields from supplier table
ALTER TABLE supplier 
DROP COLUMN IF EXISTS address,
DROP COLUMN IF EXISTS email,
DROP COLUMN IF EXISTS state_code,
DROP COLUMN IF EXISTS pincode;

-- Remove email, stateCode, and pincode from vendor table
ALTER TABLE vendor 
DROP COLUMN IF EXISTS email,
DROP COLUMN IF EXISTS state_code,
DROP COLUMN IF EXISTS pincode;

-- Remove email, stateCode, and pincode from vendor_entity table
ALTER TABLE vendor_entity 
DROP COLUMN IF EXISTS email,
DROP COLUMN IF EXISTS state_code,
DROP COLUMN IF EXISTS pincode;
