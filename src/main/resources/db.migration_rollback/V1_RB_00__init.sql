-- Drop indexes for ForgeHeat table
DROP INDEX IF EXISTS idx_forge_heat_forge_id;
DROP INDEX IF EXISTS idx_forge_heat_heat_id;

-- Drop ForgeHeat table and sequence
DROP TABLE IF EXISTS forge_heat;
DROP SEQUENCE IF EXISTS forge_heat_sequence;

-- Drop indexes for Forge table
DROP INDEX IF EXISTS idx_forge_forging_line_id;
DROP INDEX IF EXISTS idx_forge_item_id;
DROP INDEX IF EXISTS idx_forge_forge_traceability_number;

-- Drop Forge table and sequence
DROP TABLE IF EXISTS forge;
DROP SEQUENCE IF EXISTS forge_sequence;

-- Drop indexes for ForgingLine table
DROP INDEX IF EXISTS unique_idx_forging_line_name;

-- Drop ForgingLine table and sequence
DROP TABLE IF EXISTS forging_line;
DROP SEQUENCE IF EXISTS forging_line_sequence;

-- Drop indexes for Furnace table
DROP INDEX IF EXISTS idx_furnace_name;

-- Drop Furnace table and sequence
DROP TABLE IF EXISTS furnace;
DROP SEQUENCE IF EXISTS furnace_sequence;

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
DROP INDEX IF EXISTS unique_product_sku_active;
DROP INDEX IF EXISTS unique_product_code_active;
DROP INDEX IF EXISTS unique_product_name_active;
DROP INDEX IF EXISTS idx_product_product_name;

-- Drop Product table and sequence
DROP TABLE IF EXISTS product;
DROP SEQUENCE IF EXISTS product_sequence;

-- Drop indexes for Supplier table
DROP INDEX IF EXISTS idx_supplier_supplier_name;
DROP INDEX IF EXISTS unique_supplier_name_active;

-- Drop Supplier table and sequence
DROP TABLE IF EXISTS supplier;
DROP SEQUENCE IF EXISTS supplier_sequence;

-- Drop indexes for Tenant table
DROP INDEX IF EXISTS idx_tenant_org_id;
DROP INDEX IF EXISTS idx_tenant_name;

-- Drop Tenant table and sequence
DROP TABLE IF EXISTS tenant;
DROP SEQUENCE IF EXISTS tenant_sequence;
