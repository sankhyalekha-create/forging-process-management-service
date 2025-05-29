-- Rollback script for V1_29__add_performance_indexes_for_new_queries.sql
-- This script removes all the performance indexes created for the new SQL queries

-- Drop index for MachiningBatch create_at column
DROP INDEX IF EXISTS idx_machining_batch_create_at;

-- Drop index for complex availability queries
DROP INDEX IF EXISTS idx_daily_machining_batch_overlap_detection;

-- Drop index for MachineOperator and time range queries
DROP INDEX IF EXISTS idx_daily_machining_batch_operator_start_time;

-- Drop index for native query getTotalMachiningHours optimization
DROP INDEX IF EXISTS idx_daily_machining_batch_operator_time_native;

-- Drop partial index for MachiningBatch non-completed status
DROP INDEX IF EXISTS idx_machining_batch_non_completed;

-- Drop index for DailyMachiningBatch machining_batch_id joins
DROP INDEX IF EXISTS idx_daily_machining_batch_machining_batch_deleted;

-- Drop index for MachineSet created_at ordering
DROP INDEX IF EXISTS idx_machine_set_created_deleted;

-- Drop index for Machine tenant relationship
DROP INDEX IF EXISTS idx_machine_tenant_deleted;

-- Drop index for MachiningBatch status and created_at
DROP INDEX IF EXISTS idx_machining_batch_status_created;

-- Drop index for MachiningBatch with DailyMachiningBatch joins
DROP INDEX IF EXISTS idx_daily_machining_batch_machine_set_machining_batch;

-- Drop index for MachiningBatch tenant queries
DROP INDEX IF EXISTS idx_machining_batch_tenant_created;

-- Drop composite index for DailyMachiningBatch time range queries
DROP INDEX IF EXISTS idx_daily_machining_batch_time_range_deleted;

-- Drop index for DailyMachiningBatch machine set overlap queries
DROP INDEX IF EXISTS idx_daily_machining_batch_machine_set_time_range;

-- Drop index for DailyMachiningBatch machine operator overlap queries
DROP INDEX IF EXISTS idx_daily_machining_batch_operator_time_range; 