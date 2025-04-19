-- Drop indexes first
DROP INDEX IF EXISTS idx_buyer_entity_buyer_id;
DROP INDEX IF EXISTS idx_buyer_entity_name;
DROP INDEX IF EXISTS idx_buyer_tenant_id;
DROP INDEX IF EXISTS idx_buyer_name;

-- Drop tables
DROP TABLE IF EXISTS buyer_entity;
DROP TABLE IF EXISTS buyer;

-- Drop sequences
DROP SEQUENCE IF EXISTS buyer_entity_sequence;
DROP SEQUENCE IF EXISTS buyer_sequence; 