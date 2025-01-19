-- Drop index on gauge_name
DROP INDEX IF EXISTS idx_gauge_name;

-- Drop index on gauge_name and tenant_id
DROP INDEX IF EXISTS idx_gauge_name_tenant_id;

-- Drop the gauge table
DROP TABLE IF EXISTS gauge;

-- Drop the gauge_sequence
DROP SEQUENCE IF EXISTS gauge_sequence;
