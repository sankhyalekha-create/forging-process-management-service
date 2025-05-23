-- Combined rollback script for V1.27 through V1.29

-- Drop indexes from forge_heat table
DROP INDEX IF EXISTS idx_forge_heat_rejected_pieces;
DROP INDEX IF EXISTS idx_forge_heat_other_rejections;
DROP INDEX IF EXISTS idx_forge_heat_rejected_pieces_count;

-- Remove rejection fields from forge_heat table
ALTER TABLE forge_heat
    DROP COLUMN IF EXISTS heat_quantity_used_in_rejected_pieces,
    DROP COLUMN IF EXISTS heat_quantity_used_in_other_rejections,
    DROP COLUMN IF EXISTS rejected_pieces;

-- Drop index from processed_item table
DROP INDEX IF EXISTS idx_processed_item_rejected_pieces;

-- Remove rejection fields from processed_item table
ALTER TABLE processed_item
    DROP COLUMN IF EXISTS rejected_forge_pieces_count,
    DROP COLUMN IF EXISTS other_forge_rejections_kg;

-- Remove item_slug_weight column from item table
ALTER TABLE item DROP COLUMN IF EXISTS item_slug_weight; 

-- Remove item_weight_type column from forge table
ALTER TABLE forge DROP COLUMN IF EXISTS item_weight_type;