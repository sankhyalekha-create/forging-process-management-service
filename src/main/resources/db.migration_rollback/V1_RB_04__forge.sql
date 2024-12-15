-- Drop indexes for ForgeHeat table
DROP INDEX IF EXISTS idx_forge_heat_forge_id;
DROP INDEX IF EXISTS idx_forge_heat_heat_id;

-- Drop ForgeHeat table and sequence
DROP TABLE IF EXISTS forge_heat;
DROP SEQUENCE IF EXISTS forge_heat_sequence;

-- Drop indexes for Forge
DROP INDEX IF EXISTS idx_forge_processed_item_id;
DROP INDEX IF EXISTS idx_forge_forging_line_id;

-- Drop table Forge
DROP TABLE IF EXISTS forge;

-- Drop sequence for Forge
DROP SEQUENCE IF EXISTS forge_sequence;

-- Drop indexes for ProcessedItem
DROP INDEX IF EXISTS idx_processed_item_item_status;
DROP INDEX IF EXISTS idx_processed_item_item_id;

-- Drop table ProcessedItem
DROP TABLE IF EXISTS processed_item;

-- Drop sequence for ProcessedItem
DROP SEQUENCE IF EXISTS processed_item_sequence;

-- Drop indexes for ForgingLine table
DROP INDEX IF EXISTS unique_idx_forging_line_name;

-- Drop ForgingLine table and sequence
DROP TABLE IF EXISTS forging_line;
DROP SEQUENCE IF EXISTS forging_line_sequence;

