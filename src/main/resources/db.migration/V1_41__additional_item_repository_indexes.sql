-- Migration: Additional indexes for ItemRepository query optimization
-- Version: V1_41
-- Description: Add composite indexes to optimize complex queries in ItemRepository

-- 1. Composite index for item_code search queries (similar to existing item_name index)
CREATE INDEX IF NOT EXISTS idx_item_code_lower 
ON item (lower(item_code)) 
WHERE deleted = false;

-- 2. Composite index for workflow queries with tenant filtering
CREATE INDEX IF NOT EXISTS idx_item_tenant_deleted_workflow_created 
ON item (tenant_id, deleted, created_at DESC)
WHERE deleted = false;

-- 3. Index for workflow_template foreign key joins (used in EXISTS subqueries)
CREATE INDEX IF NOT EXISTS idx_workflow_step_template_operation_deleted 
ON workflow_step (workflow_template_id, operation_type, deleted)
WHERE deleted = false;

-- 4. Composite index for item_workflow_step status and operation filtering
-- This is critical for the NOT EXISTS clause we added for completed operations
CREATE INDEX IF NOT EXISTS idx_item_workflow_step_workflow_operation_status 
ON item_workflow_step (item_workflow_id, operation_type, step_status)
WHERE deleted = false;

-- 5. Index for item_workflow tenant filtering through item relationship
-- This helps with the complex workflow queries that filter by tenant
CREATE INDEX IF NOT EXISTS idx_item_workflow_item_deleted_status 
ON item_workflow (item_id, deleted, workflow_status)
WHERE deleted = false;

-- 6. Composite index for workflow_template queries (tenant + active status)
CREATE INDEX IF NOT EXISTS idx_workflow_template_tenant_deleted 
ON workflow_template (tenant_id, deleted)
WHERE deleted = false;

-- 7. Performance index for item ordering in workflow queries
CREATE INDEX IF NOT EXISTS idx_item_deleted_created_tenant 
ON item (deleted, created_at DESC, tenant_id)
WHERE deleted = false;

-- 8. Index for efficient EXISTS queries on workflow steps
CREATE INDEX IF NOT EXISTS idx_workflow_step_operation_template_deleted 
ON workflow_step (operation_type, workflow_template_id, deleted)
WHERE deleted = false;

-- Add comments explaining the purpose of these indexes
COMMENT ON INDEX idx_item_code_lower IS 'Optimizes case-insensitive item_code search queries with LIKE operations';
COMMENT ON INDEX idx_item_tenant_deleted_workflow_created IS 'Optimizes tenant-filtered workflow queries with ordering';
COMMENT ON INDEX idx_workflow_step_template_operation_deleted IS 'Optimizes EXISTS subqueries for workflow step filtering by operation type';
COMMENT ON INDEX idx_item_workflow_step_workflow_operation_status IS 'Critical for NOT EXISTS queries filtering completed workflow steps';
COMMENT ON INDEX idx_item_workflow_item_deleted_status IS 'Optimizes item_workflow joins with status filtering';
COMMENT ON INDEX idx_workflow_template_tenant_deleted IS 'Optimizes workflow template queries by tenant';
COMMENT ON INDEX idx_item_deleted_created_tenant IS 'Optimizes item ordering in complex workflow queries';
COMMENT ON INDEX idx_workflow_step_operation_template_deleted IS 'Optimizes EXISTS queries by operation type and template';

-- Verification
DO $$
BEGIN
    RAISE NOTICE 'ItemRepository optimization indexes created successfully';
    RAISE NOTICE 'These indexes will significantly improve performance for:';
    RAISE NOTICE '- Case-insensitive item_code searches';
    RAISE NOTICE '- Complex workflow queries with tenant filtering';
    RAISE NOTICE '- EXISTS/NOT EXISTS subqueries for workflow step filtering';
    RAISE NOTICE '- Workflow status and completion state queries';
END $$; 