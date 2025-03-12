-- Rollback ForgeHeat Table
DROP INDEX IF EXISTS idx_forge_heat_forge_id;
DROP INDEX IF EXISTS idx_forge_heat_heat_id;
DROP TABLE IF EXISTS forge_heat;
DROP SEQUENCE IF EXISTS forge_heat_sequence;

-- Rollback Forge Table
DROP INDEX IF EXISTS idx_forge_forge_traceability_number_tenant;
DROP INDEX IF EXISTS idx_forge_forge_traceability_number;
DROP INDEX IF EXISTS idx_forge_processed_item_id;
DROP INDEX IF EXISTS idx_forge_forging_line_id;
DROP TABLE IF EXISTS forge;
DROP SEQUENCE IF EXISTS forge_sequence;

-- Rollback ProcessedItem Table
DROP INDEX IF EXISTS idx_processed_item_item_id;
DROP TABLE IF EXISTS processed_item;
DROP SEQUENCE IF EXISTS processed_item_sequence;

-- Rollback ForgingLine Table
DROP INDEX IF EXISTS idx_forging_line_name_tenant_id;
DROP TABLE IF EXISTS forging_line;
DROP SEQUENCE IF EXISTS forging_line_sequence;
