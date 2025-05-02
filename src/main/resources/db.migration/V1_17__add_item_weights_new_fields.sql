ALTER TABLE item
ADD COLUMN item_forged_weight DOUBLE PRECISION,
ADD COLUMN item_finished_weight DOUBLE PRECISION;

-- Update existing rows to set default values based on item_weight
UPDATE item SET item_forged_weight = item_weight WHERE item_forged_weight IS NULL;
UPDATE item SET item_finished_weight = item_weight WHERE item_finished_weight IS NULL; 