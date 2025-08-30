-- Update tenant phone_number column to support landline numbers with dashes
ALTER TABLE tenant 
    ALTER COLUMN phone_number TYPE VARCHAR(15);

-- Add comment to document the change
COMMENT ON COLUMN tenant.phone_number IS 'Phone number of the tenant (supports both mobile and landline formats)';
