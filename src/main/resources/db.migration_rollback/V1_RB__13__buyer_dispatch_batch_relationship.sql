-- Drop indexes
DROP INDEX IF EXISTS idx_dispatch_batch_buyer_id;
DROP INDEX IF EXISTS idx_dispatch_batch_billing_entity_id;
DROP INDEX IF EXISTS idx_dispatch_batch_shipping_entity_id;

-- Drop foreign key constraints
ALTER TABLE dispatch_batch
DROP CONSTRAINT IF EXISTS fk_dispatch_batch_buyer,
DROP CONSTRAINT IF EXISTS fk_dispatch_batch_billing_entity,
DROP CONSTRAINT IF EXISTS fk_dispatch_batch_shipping_entity;

-- Drop columns
ALTER TABLE dispatch_batch
DROP COLUMN IF EXISTS buyer_id,
DROP COLUMN IF EXISTS billing_entity_id,
DROP COLUMN IF EXISTS shipping_entity_id; 