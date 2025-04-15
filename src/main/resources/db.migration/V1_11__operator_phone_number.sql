-- Add phone number field to operator table
ALTER TABLE operator
    ADD COLUMN phone_number VARCHAR(15);

-- Add index for the phone number field
CREATE INDEX idx_operator_phone_number ON operator(phone_number);

-- Add comment for the new column
COMMENT ON COLUMN operator.phone_number IS 'Phone number of the operator'; 