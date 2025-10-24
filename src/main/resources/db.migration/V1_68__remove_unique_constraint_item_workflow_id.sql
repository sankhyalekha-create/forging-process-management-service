-- Migration Script: V1_68__remove_unique_constraint_item_workflow_id.sql
-- Description: Remove unique constraint on item_workflow_id to allow multiple OrderItemWorkflows per ItemWorkflow
-- Author: System Generated
-- Date: 2024-10-19
-- Version: V1_67
--
-- Background:
-- Changed relationship from OneToOne to ManyToOne between OrderItemWorkflow and ItemWorkflow.
-- This allows a single ItemWorkflow to be associated with multiple OrderItemWorkflows,
-- enabling better flexibility in order-workflow associations.

-- Drop the existing unique index on item_workflow_id
DROP INDEX IF EXISTS idx_order_item_workflow_item_workflow_id;

-- Create a non-unique index on item_workflow_id for query performance
CREATE INDEX idx_order_item_workflow_item_workflow_id ON order_item_workflows(item_workflow_id);

-- Add comment to document the change
COMMENT ON INDEX idx_order_item_workflow_item_workflow_id IS 'Non-unique index to support ManyToOne relationship - allows multiple OrderItemWorkflows per ItemWorkflow';

COMMIT;

