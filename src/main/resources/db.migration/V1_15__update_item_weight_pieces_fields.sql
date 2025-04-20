-- Make item_weight nullable
ALTER TABLE item ALTER COLUMN item_weight DROP NOT NULL;

-- Add item_count column
ALTER TABLE item ADD COLUMN item_count INTEGER;

-- Add comment explaining the change
COMMENT ON COLUMN item.item_weight IS 'Weight of the item in KGs. Required when product uses UnitOfMeasurement.KGS';
COMMENT ON COLUMN item.item_count IS 'Count of the item in pieces. Required when product uses UnitOfMeasurement.PIECES'; 