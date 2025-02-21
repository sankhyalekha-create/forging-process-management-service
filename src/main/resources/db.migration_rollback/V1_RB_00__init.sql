-- Drop indexes for Heat table
DROP INDEX IF EXISTS idx_heat_raw_material_product_id;
DROP INDEX IF EXISTS idx_heat_number;

-- Drop Heat table and sequence
DROP TABLE IF EXISTS heat;
DROP SEQUENCE IF EXISTS heat_sequence;

-- Drop indexes for RawMaterialProduct table
DROP INDEX IF EXISTS idx_raw_material_product_raw_material_id_product_id;

-- Drop RawMaterialProduct table and sequence
DROP TABLE IF EXISTS raw_material_product;
DROP SEQUENCE IF EXISTS raw_material_product_sequence;

-- Drop indexes for RawMaterial table
DROP INDEX IF EXISTS idx_raw_material_tenant_id;
DROP INDEX IF EXISTS idx_raw_material_hsn_code;
DROP INDEX IF EXISTS idx_raw_material_invoice_number;

-- Drop RawMaterial table and sequence
DROP TABLE IF EXISTS raw_material;
DROP SEQUENCE IF EXISTS raw_material_sequence;

-- Drop indexes for ProductSupplier table
DROP TABLE IF EXISTS product_supplier;

-- Drop indexes for Product table
DROP INDEX IF EXISTS unique_product_code_active;
DROP INDEX IF EXISTS unique_product_name_active;
DROP INDEX IF EXISTS idx_product_product_name;

-- Drop Product table and sequence
DROP TABLE IF EXISTS product;
DROP SEQUENCE IF EXISTS product_sequence;

-- Drop indexes for Supplier table
DROP INDEX IF EXISTS idx_supplier_supplier_name;
DROP INDEX IF EXISTS idx_supplier_name_tenant_id;

-- Drop Supplier table and sequence
DROP TABLE IF EXISTS supplier;
DROP SEQUENCE IF EXISTS supplier_sequence;

-- Drop indexes for Tenant table
DROP INDEX IF EXISTS idx_tenant_org_id;
DROP INDEX IF EXISTS idx_tenant_name;

-- Drop Tenant table and sequence
DROP TABLE IF EXISTS tenant;
DROP SEQUENCE IF EXISTS tenant_sequence;
