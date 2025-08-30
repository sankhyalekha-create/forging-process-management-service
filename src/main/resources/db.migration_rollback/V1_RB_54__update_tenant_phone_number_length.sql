-- Rollback tenant phone_number column length change
-- WARNING: This may truncate data if phone numbers longer than 10 characters exist
ALTER TABLE tenant 
    ALTER COLUMN phone_number TYPE VARCHAR(10);

-- Remove comment
COMMENT ON COLUMN tenant.phone_number IS NULL;
