-- Performance indexes for new SQL queries introduced in recent changes
-- This migration adds indexes to optimize the new query patterns introduced
-- after moving machine_set relationship to daily_machining_batch table

-- Index for DailyMachiningBatch machine operator overlap queries
-- Optimizes existsOverlappingBatchForOperator and findOverlappingBatchesForOperator
CREATE INDEX idx_daily_machining_batch_operator_time_range 
ON daily_machining_batch(machine_operator_id, deleted, start_date_time, end_date_time)
WHERE deleted = false;

-- Index for DailyMachiningBatch machine set overlap queries  
-- Optimizes existsOverlappingBatchForMachineSet and findOverlappingBatchesForMachineSet
CREATE INDEX idx_daily_machining_batch_machine_set_time_range 
ON daily_machining_batch(machine_set_id, deleted, start_date_time, end_date_time)
WHERE deleted = false;

-- Composite index for DailyMachiningBatch time range queries
-- Optimizes queries that filter by time ranges across start_date_time and end_date_time
CREATE INDEX idx_daily_machining_batch_time_range_deleted 
ON daily_machining_batch(start_date_time, end_date_time, deleted)
WHERE deleted = false;

-- Index for MachiningBatch tenant queries
-- Optimizes findByTenantIdAndDeletedFalseOrderByCreatedAtDesc
CREATE INDEX idx_machining_batch_tenant_created 
ON machining_batch(tenant_id, deleted, created_at DESC)
WHERE deleted = false;

-- Index for MachiningBatch with DailyMachiningBatch joins
-- Optimizes the updated findAppliedMachiningBatchOnMachineSet query
CREATE INDEX idx_daily_machining_batch_machine_set_machining_batch 
ON daily_machining_batch(machine_set_id, machining_batch_id, deleted)
WHERE deleted = false;

-- Index for MachiningBatch status and created_at for complex queries
-- Optimizes queries that filter by status and order by created_at  
CREATE INDEX idx_machining_batch_status_created 
ON machining_batch(machining_batch_status, deleted, created_at DESC)
WHERE deleted = false;

-- Composite index for Machine tenant relationship used in machine set availability queries
-- Optimizes findAvailableMachineSetsByTenantIdAndTimeRange
CREATE INDEX idx_machine_tenant_deleted 
ON machine(tenant_id, deleted)
WHERE deleted = false;

-- Index for MachineSet created_at for ordering in availability queries
-- Optimizes ORDER BY ms.createdAt DESC in findAvailableMachineSetsByTenantIdAndTimeRange
CREATE INDEX idx_machine_set_created_deleted 
ON machine_set(created_at DESC, deleted)
WHERE deleted = false;

-- Index for DailyMachiningBatch machining_batch_id for join operations
-- Optimizes various JOIN operations between machining_batch and daily_machining_batch
CREATE INDEX idx_daily_machining_batch_machining_batch_deleted 
ON daily_machining_batch(machining_batch_id, deleted)
WHERE deleted = false;

-- Performance optimization for native query getTotalMachiningHours
-- Optimizes the native SQL query for calculating machining hours
CREATE INDEX idx_daily_machining_batch_operator_time_native 
ON daily_machining_batch(machine_operator_id, deleted, start_date_time, end_date_time)
WHERE deleted = false;

-- Index for MachineOperator and time range queries
-- Optimizes findByMachineOperatorAndStartDateTimeBetween
CREATE INDEX idx_daily_machining_batch_operator_start_time 
ON daily_machining_batch(machine_operator_id, start_date_time, deleted)
WHERE deleted = false;

-- Additional index for complex availability queries involving multiple table joins
-- Optimizes the subquery in findAvailableMachineSetsByTenantIdAndTimeRange
CREATE INDEX idx_daily_machining_batch_overlap_detection 
ON daily_machining_batch(machine_set_id, start_date_time, end_date_time, deleted)
WHERE deleted = false;

-- Index for MachiningBatch create_at column (renamed from apply_at)
-- Optimizes time-based validations in business logic
CREATE INDEX idx_machining_batch_create_at 
ON machining_batch(create_at, deleted)
WHERE deleted = false; 