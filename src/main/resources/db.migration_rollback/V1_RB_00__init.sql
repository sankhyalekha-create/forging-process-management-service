-- Drop indexes in reverse order of creation
DROP INDEX IF EXISTS idx_heat_raw_material_product_id;
DROP INDEX IF EXISTS idx_heat_number;
DROP INDEX IF EXISTS idx_raw_material_product_raw_material_id_product_id;
DROP INDEX IF EXISTS idx_raw_material_tenant_id;
DROP INDEX IF EXISTS idx_raw_material_hsn_code;
DROP INDEX IF EXISTS idx_raw_material_invoice_number;
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
DROP TABLE IF EXISTS product_supplier;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS supplier;
DROP TABLE IF EXISTS Tenant;

-- Drop sequences in reverse order of creation
DROP SEQUENCE IF EXISTS heat_sequence;
DROP SEQUENCE IF EXISTS raw_material_product_sequence;
DROP SEQUENCE IF EXISTS raw_material_sequence;
DROP SEQUENCE IF EXISTS product_sequence;
DROP SEQUENCE IF EXISTS supplier_sequence;
DROP SEQUENCE IF EXISTS tenant_sequence;

-- Drop indexes on 'item_product' table
DROP INDEX IF EXISTS idx_item_product_product_id;
DROP INDEX IF EXISTS idx_item_product_item_id;
DROP INDEX IF EXISTS idx_item_product_item_id_product_id;

-- Drop index on 'item' table
DROP INDEX IF EXISTS idx_item_name;

-- Drop 'item_product' table
DROP TABLE IF EXISTS item_product;

-- Drop Sequence for 'item_product' table
DROP SEQUENCE IF EXISTS item_product_sequence;

-- Drop 'item' table
DROP TABLE IF EXISTS item;

-- Drop Sequence for 'item' table
DROP SEQUENCE IF EXISTS item_sequence;


-- Drop the heat_treatment_batch_forge_traceability join table
DROP TABLE IF EXISTS heat_treatment_batch_forge_traceability;

-- Drop the heat_treatment_batch table
DROP INDEX IF EXISTS idx_heat_treatment_furnace_id;
DROP SEQUENCE heat_treatment_batch_sequence;
DROP TABLE IF EXISTS heat_treatment_batch;

-- Drop indexes and forge_traceability table
DROP INDEX IF EXISTS idx_forge_traceability_heat_id;
DROP INDEX IF EXISTS idx_forge_traceability_forging_line_id;
DROP SEQUENCE forge_traceability_sequence;
DROP TABLE IF EXISTS forge_traceability;

-- Drop indexes and forging_line table
DROP INDEX IF EXISTS idx_forging_line_name;
DROP SEQUENCE forging_line_sequence;
DROP TABLE IF EXISTS forging_line;

-- Drop indexes and furnace table
DROP INDEX IF EXISTS idx_furnace_name;
DROP SEQUENCE furnace_sequence;
DROP TABLE IF EXISTS furnace;

