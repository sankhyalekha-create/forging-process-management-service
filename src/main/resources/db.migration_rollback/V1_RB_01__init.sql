-- Drop indexes first
DROP INDEX IF EXISTS idx_usr_username;
DROP INDEX IF EXISTS idx_usr_tenant_id;

-- Drop roles table
DROP TABLE IF EXISTS usr_roles;

-- Drop user table
DROP TABLE IF EXISTS "usr";

-- Drop sequence
DROP SEQUENCE IF EXISTS usr_sequence;
