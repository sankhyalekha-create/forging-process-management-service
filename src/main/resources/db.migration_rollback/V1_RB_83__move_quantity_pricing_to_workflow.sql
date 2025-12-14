-- ROLLBACK Script: Restore quantity and pricing fields to order_items
-- Version: 1.0.1 ROLLBACK
-- Date: 2025-12-13
-- WARNING: This will restore data from FIRST workflow of each OrderItem
-- Use only if migration needs to be reverted within 24 hours

-- Step 1: Add columns back to order_items
ALTER TABLE order_items 
ADD COLUMN IF NOT EXISTS quantity INTEGER,
ADD COLUMN IF NOT EXISTS work_type VARCHAR(50),
ADD COLUMN IF NOT EXISTS unit_price DECIMAL(10,2),
ADD COLUMN IF NOT EXISTS material_cost_per_unit DECIMAL(10,2),
ADD COLUMN IF NOT EXISTS job_work_cost_per_unit DECIMAL(10,2),
ADD COLUMN IF NOT EXISTS special_instructions VARCHAR(1000);

-- Step 2: Copy data from FIRST workflow of each OrderItem back to OrderItem
-- Note: If an OrderItem has multiple workflows with different values, only the first is preserved
UPDATE order_items oi
SET 
  quantity = first_workflow.quantity,
  work_type = first_workflow.work_type,
  unit_price = first_workflow.unit_price,
  material_cost_per_unit = first_workflow.material_cost_per_unit,
  job_work_cost_per_unit = first_workflow.job_work_cost_per_unit,
  special_instructions = first_workflow.special_instructions
FROM (
  SELECT DISTINCT ON (order_item_id) 
    order_item_id,
    quantity,
    work_type,
    unit_price,
    material_cost_per_unit,
    job_work_cost_per_unit,
    special_instructions
  FROM order_item_workflows
  ORDER BY order_item_id, id
) AS first_workflow
WHERE oi.id = first_workflow.order_item_id;

-- Step 3: Set defaults for items without workflows
UPDATE order_items
SET 
  quantity = COALESCE(quantity, 0),
  work_type = COALESCE(work_type, 'WITH_MATERIAL')
WHERE quantity IS NULL OR work_type IS NULL;

-- Step 4: Drop columns from order_item_workflows
ALTER TABLE order_item_workflows
DROP COLUMN IF EXISTS quantity,
DROP COLUMN IF EXISTS work_type,
DROP COLUMN IF EXISTS unit_price,
DROP COLUMN IF EXISTS material_cost_per_unit,
DROP COLUMN IF EXISTS job_work_cost_per_unit,
DROP COLUMN IF EXISTS special_instructions;

-- Step 5: Log rollback
DO $$
BEGIN
  RAISE NOTICE 'ROLLBACK COMPLETED: Quantity and pricing fields restored to order_items table';
  RAISE NOTICE 'WARNING: If OrderItems had multiple workflows, only data from first workflow was preserved';
END $$;
