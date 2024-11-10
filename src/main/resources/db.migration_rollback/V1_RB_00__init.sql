-- Drop foreign key constraints
ALTER TABLE raw_material DROP CONSTRAINT fk_raw_material_tenant;
ALTER TABLE supplier DROP CONSTRAINT fk_supplier_tenant;
ALTER TABLE product_supplier DROP CONSTRAINT fk_product;
ALTER TABLE product_supplier DROP CONSTRAINT fk_supplier;
ALTER TABLE item_product DROP CONSTRAINT fk_item_product_item;
ALTER TABLE item_product DROP CONSTRAINT fk_item_product_product;
ALTER TABLE raw_material_product DROP CONSTRAINT fk_raw_material_product_raw_material;
ALTER TABLE raw_material_product DROP CONSTRAINT fk_raw_material_product_product;
ALTER TABLE raw_material_product DROP CONSTRAINT fk_raw_material_product_supplier;
ALTER TABLE heat DROP CONSTRAINT fk_heat_raw_material_product;

-- Drop tables
DROP TABLE IF EXISTS heat;
DROP TABLE IF EXISTS raw_material_product;
DROP TABLE IF EXISTS item_product;
DROP TABLE IF EXISTS item;
DROP TABLE IF EXISTS product_supplier;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS supplier;
DROP TABLE IF EXISTS raw_material;
DROP TABLE IF EXISTS tenant;

-- Drop indexes
DROP INDEX IF EXISTS idx_tenant_name;
DROP INDEX IF EXISTS idx_tenant_org_id;
DROP INDEX IF EXISTS idx_raw_material_invoice_number;
DROP INDEX IF EXISTS idx_raw_material_hsn_code;
DROP INDEX IF EXISTS idx_raw_material_tenant_id;
DROP INDEX IF EXISTS idx_supplier_supplier_name;
DROP INDEX IF EXISTS idx_product_product_name;
DROP INDEX IF EXISTS idx_item_item_code;
DROP INDEX IF EXISTS idx_item_product_item_id_product_id;
DROP INDEX IF EXISTS idx_raw_material_product_raw_material_id_product_id;
DROP INDEX IF EXISTS idx_heat_number;
DROP INDEX IF EXISTS idx_heat_raw_material_product_id;

-- Drop sequences
DROP SEQUENCE IF EXISTS tenant_sequence;
DROP SEQUENCE IF EXISTS raw_material_sequence;
DROP SEQUENCE IF EXISTS supplier_sequence;
DROP SEQUENCE IF EXISTS product_sequence;
DROP SEQUENCE IF EXISTS item_sequence;
DROP SEQUENCE IF EXISTS item_product_sequence;
DROP SEQUENCE IF EXISTS raw_material_product_sequence;
DROP SEQUENCE IF EXISTS heat_sequence;
