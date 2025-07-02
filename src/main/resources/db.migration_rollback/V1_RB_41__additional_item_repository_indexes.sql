-- Rollback script for V1_41__additional_item_repository_indexes.sql
-- This script removes all the additional indexes created for ItemRepository query optimization

-- Drop composite index for item_code search queries
DROP INDEX IF EXISTS idx_item_code_lower;

-- Drop composite index for workflow queries with tenant filtering
DROP INDEX IF EXISTS idx_item_tenant_deleted_workflow_created;

-- Drop index for workflow_template foreign key joins
DROP INDEX IF EXISTS idx_workflow_step_template_operation_deleted;

-- Drop composite index for item_workflow_step status and operation filtering
DROP INDEX IF EXISTS idx_item_workflow_step_workflow_operation_status;

-- Drop index for item_workflow tenant filtering through item relationship
DROP INDEX IF EXISTS idx_item_workflow_item_deleted_status;

-- Drop composite index for workflow_template queries
DROP INDEX IF EXISTS idx_workflow_template_tenant_deleted;

-- Drop performance index for item ordering in workflow queries
DROP INDEX IF EXISTS idx_item_deleted_created_tenant;

-- Drop index for efficient EXISTS queries on workflow steps
DROP INDEX IF EXISTS idx_workflow_step_operation_template_deleted;

-- Verification
DO $$
BEGIN
    RAISE NOTICE 'ItemRepository optimization indexes dropped successfully';
    RAISE NOTICE 'All performance indexes for the following have been removed:';
    RAISE NOTICE '- Case-insensitive item_code searches';
    RAISE NOTICE '- Complex workflow queries with tenant filtering';
    RAISE NOTICE '- EXISTS/NOT EXISTS subqueries for workflow step filtering';
    RAISE NOTICE '- Workflow status and completion state queries';
END $$; 