-- Drop index for the phone number field
DROP INDEX IF EXISTS idx_operator_phone_number;

-- Remove phone number field from operator table
ALTER TABLE operator
    DROP COLUMN IF EXISTS phone_number; 