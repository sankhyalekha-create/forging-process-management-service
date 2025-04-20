-- Update any null item_weight values to prevent constraint violation
-- This assumes 0 is an acceptable default value; adjust as needed for your application
UPDATE item SET item_weight = 0.0 WHERE item_weight IS NULL;

-- Make item_weight NOT NULL again
ALTER TABLE item ALTER COLUMN item_weight SET NOT NULL;

-- Remove item_count column
ALTER TABLE item DROP COLUMN item_count; 