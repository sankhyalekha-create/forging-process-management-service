-- Migration Script: Move quantity and pricing fields from order_items to order_item_workflows
-- Version: 1.0.1
-- Date: 2025-12-13
-- Description: Supports multiple production runs of same item with different quantities/prices

-- Step 1: Add new columns to order_item_workflows table
ALTER TABLE order_item_workflows 
ADD COLUMN IF NOT EXISTS quantity INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS work_type VARCHAR(50) NOT NULL DEFAULT 'WITH_MATERIAL',
ADD COLUMN IF NOT EXISTS unit_price DECIMAL(10,2),
ADD COLUMN IF NOT EXISTS material_cost_per_unit DECIMAL(10,2),
ADD COLUMN IF NOT EXISTS job_work_cost_per_unit DECIMAL(10,2),
ADD COLUMN IF NOT EXISTS special_instructions VARCHAR(1000);

-- Step 2: Migrate data from order_items to order_item_workflows
-- For each OrderItem, copy its quantity/prices to ALL its OrderItemWorkflows
UPDATE order_item_workflows oiw
SET 
  quantity = COALESCE((SELECT quantity FROM order_items WHERE id = oiw.order_item_id), 0),
  work_type = COALESCE((SELECT work_type FROM order_items WHERE id = oiw.order_item_id), 'WITH_MATERIAL'),
  unit_price = (SELECT unit_price FROM order_items WHERE id = oiw.order_item_id),
  material_cost_per_unit = (SELECT material_cost_per_unit FROM order_items WHERE id = oiw.order_item_id),
  job_work_cost_per_unit = (SELECT job_work_cost_per_unit FROM order_items WHERE id = oiw.order_item_id),
  special_instructions = (SELECT special_instructions FROM order_items WHERE id = oiw.order_item_id)
WHERE EXISTS (SELECT 1 FROM order_items WHERE id = oiw.order_item_id);

-- Step 3: Verify migration (this will fail if any workflows have zero quantity)
DO $$
DECLARE
  zero_quantity_count INTEGER;
BEGIN
  SELECT COUNT(*) INTO zero_quantity_count
  FROM order_item_workflows
  WHERE quantity = 0 OR quantity IS NULL;
  
  IF zero_quantity_count > 0 THEN
    RAISE NOTICE 'WARNING: % order_item_workflows have zero or null quantity!', zero_quantity_count;
  END IF;
END $$;

-- Step 4: Drop old columns from order_items table
ALTER TABLE order_items 
DROP COLUMN IF EXISTS quantity,
DROP COLUMN IF EXISTS work_type,
DROP COLUMN IF EXISTS unit_price,
DROP COLUMN IF EXISTS material_cost_per_unit,
DROP COLUMN IF EXISTS job_work_cost_per_unit,
DROP COLUMN IF EXISTS special_instructions;

-- Step 5: Add comment for documentation
COMMENT ON COLUMN order_item_workflows.quantity IS 'Quantity for this specific workflow execution';
COMMENT ON COLUMN order_item_workflows.work_type IS 'Work type for this workflow execution (JOB_WORK_ONLY or WITH_MATERIAL)';
COMMENT ON COLUMN order_item_workflows.unit_price IS 'Total unit price (material + job work) for this workflow';
COMMENT ON COLUMN order_item_workflows.material_cost_per_unit IS 'Material cost per unit (for WITH_MATERIAL work type)';
COMMENT ON COLUMN order_item_workflows.job_work_cost_per_unit IS 'Job work/processing cost per unit';
COMMENT ON COLUMN order_item_workflows.special_instructions IS 'Special instructions for this workflow execution';
