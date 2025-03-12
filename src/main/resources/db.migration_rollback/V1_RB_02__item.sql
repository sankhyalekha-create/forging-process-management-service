-- Drop indexes for 'item_product' table
DROP INDEX IF EXISTS idx_item_product_item_id_product_id;
DROP INDEX IF EXISTS idx_item_product_product_id;
DROP INDEX IF EXISTS idx_item_product_item_id;

-- Drop table 'item_product'
DROP TABLE IF EXISTS item_product;

-- Drop sequence for 'item_product'
DROP SEQUENCE IF EXISTS item_product_sequence;

-- Drop indexes for 'item' table
DROP INDEX IF EXISTS idx_item_name;
DROP INDEX IF EXISTS idx_item_tenant_id;
DROP INDEX IF EXISTS idx_item_name_tenant_id;
DROP INDEX IF EXISTS idx_item_code_tenant_id;

-- Drop table 'item'
DROP TABLE IF EXISTS item;

-- Drop sequence for 'item'
DROP SEQUENCE IF EXISTS item_sequence;
