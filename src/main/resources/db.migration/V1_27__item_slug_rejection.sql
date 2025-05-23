-- Combined migration script for V1.27 through V1.29

-- Add item_slug_weight column to the item table
ALTER TABLE item ADD COLUMN item_slug_weight DOUBLE PRECISION;

UPDATE item SET item_slug_weight = item_weight WHERE item_slug_weight IS NULL;

-- Add comment to explain the purpose of the column
COMMENT ON COLUMN item.item_slug_weight IS 'Weight of the item in slug stage between raw and forged'; 

-- Add rejection fields to processed_item table
ALTER TABLE processed_item
    ADD COLUMN rejected_forge_pieces_count INTEGER,
    ADD COLUMN other_forge_rejections_kg DOUBLE PRECISION;

-- Add comment to explain the fields
COMMENT ON COLUMN processed_item.rejected_forge_pieces_count IS 'Count of pieces rejected during the forging process';
COMMENT ON COLUMN processed_item.other_forge_rejections_kg IS 'Other rejections measured in kilograms during the forging process';

-- Create an index for rejected_forge_pieces_count for potential filtering/reporting
CREATE INDEX idx_processed_item_rejected_pieces ON processed_item(rejected_forge_pieces_count);

-- Add rejection fields to forge_heat table
ALTER TABLE forge_heat
    ADD COLUMN heat_quantity_used_in_rejected_pieces DOUBLE PRECISION,
    ADD COLUMN heat_quantity_used_in_other_rejections DOUBLE PRECISION,
    ADD COLUMN rejected_pieces INTEGER;

-- Add comments to explain the fields
COMMENT ON COLUMN forge_heat.heat_quantity_used_in_rejected_pieces IS 'Heat quantity used in rejected pieces during forging process';
COMMENT ON COLUMN forge_heat.heat_quantity_used_in_other_rejections IS 'Heat quantity used in other rejections during forging process';
COMMENT ON COLUMN forge_heat.rejected_pieces IS 'Number of rejected pieces from this heat during forging process';

-- Create indexes for the new columns for potentially improved query performance on reports
CREATE INDEX idx_forge_heat_rejected_pieces ON forge_heat(heat_quantity_used_in_rejected_pieces);
CREATE INDEX idx_forge_heat_other_rejections ON forge_heat(heat_quantity_used_in_other_rejections);
CREATE INDEX idx_forge_heat_rejected_pieces_count ON forge_heat(rejected_pieces);

ALTER TABLE forge ADD COLUMN IF NOT EXISTS item_weight_type VARCHAR(30);
UPDATE forge SET item_weight_type = 'ITEM_WEIGHT' WHERE item_weight_type IS NULL;