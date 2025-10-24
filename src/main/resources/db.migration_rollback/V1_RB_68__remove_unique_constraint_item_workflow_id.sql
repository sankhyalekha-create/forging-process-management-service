-- Rollback Script: V1_RB_68__remove_unique_constraint_item_workflow_id.sql
-- Description: Rollback changes to restore unique constraint on item_workflow_id
-- Author: System Generated
-- Date: 2024-10-19
-- Version: V1_RB_67
--
-- WARNING: This rollback will FAIL if there are multiple OrderItemWorkflows
-- associated with the same ItemWorkflow. Manual data cleanup required before rollback.

-- Drop the non-unique index
DROP INDEX IF EXISTS idx_order_item_workflow_item_workflow_id;

-- Recreate the unique index on item_workflow_id
CREATE UNIQUE INDEX idx_order_item_workflow_item_workflow_id ON order_item_workflows(item_workflow_id);

-- Add comment
COMMENT ON INDEX idx_order_item_workflow_item_workflow_id IS 'Unique index to enforce OneToOne relationship between OrderItemWorkflow and ItemWorkflow';

COMMIT;

