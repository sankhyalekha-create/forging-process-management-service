-- Drop indexes in reverse order of creation
DROP INDEX IF EXISTS idx_heat_raw_material_product_id;
DROP INDEX IF EXISTS idx_heat_number;
DROP INDEX IF EXISTS idx_raw_material_product_raw_material_id_product_id;
DROP INDEX IF EXISTS idx_raw_material_tenant_id;
DROP INDEX IF EXISTS idx_raw_material_hsn_code;
DROP INDEX IF EXISTS idx_raw_material_invoice_number;
DROP INDEX IF EXISTS idx_item_product_item_id_product_id;
DROP INDEX IF EXISTS idx_item_item_code;
DROP INDEX IF EXISTS idx_product_product_name;
DROP INDEX IF EXISTS unique_product_name_active;
DROP INDEX IF EXISTS unique_product_code_active;
DROP INDEX IF EXISTS unique_product_sku_active;
DROP INDEX IF EXISTS idx_supplier_supplier_name;
DROP INDEX IF EXISTS unique_supplier_name_active;
DROP INDEX IF EXISTS idx_tenant_org_id;
DROP INDEX IF EXISTS idx_tenant_name;

-- Drop tables in reverse order of creation
DROP TABLE IF EXISTS heat;
DROP TABLE IF EXISTS raw_material_product;
DROP TABLE IF EXISTS raw_material;
DROP TABLE IF EXISTS item_product;
DROP TABLE IF EXISTS item;
DROP TABLE IF EXISTS product_supplier;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS supplier;
DROP TABLE IF EXISTS Tenant;

-- Drop sequences in reverse order of creation
DROP SEQUENCE IF EXISTS heat_sequence;
DROP SEQUENCE IF EXISTS raw_material_product_sequence;
DROP SEQUENCE IF EXISTS raw_material_sequence;
DROP SEQUENCE IF EXISTS item_product_sequence;
DROP SEQUENCE IF EXISTS item_sequence;
DROP SEQUENCE IF EXISTS product_sequence;
DROP SEQUENCE IF EXISTS supplier_sequence;
DROP SEQUENCE IF EXISTS tenant_sequence;
