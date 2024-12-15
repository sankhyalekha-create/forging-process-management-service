-- Drop indexes for ItemProduct table
DROP INDEX IF EXISTS idx_item_product_item_id_product_id;
DROP INDEX IF EXISTS idx_item_product_product_id;
DROP INDEX IF EXISTS idx_item_product_item_id;

-- Drop ItemProduct table and sequence
DROP TABLE IF EXISTS item_product;
DROP SEQUENCE IF EXISTS item_product_sequence;

-- Drop indexes for Item table
DROP INDEX IF EXISTS idx_item_name;

-- Drop Item table and sequence
DROP TABLE IF EXISTS item;
DROP SEQUENCE IF EXISTS item_sequence;
