-- Add original_machining_batch_number column to machining_batch table
ALTER TABLE machining_batch
ADD COLUMN original_machining_batch_number VARCHAR(255);

-- Update existing records to have the same value for original number as their current batch number
-- This ensures consistency for any previously deleted batches
UPDATE machining_batch
SET original_machining_batch_number = machining_batch_number
WHERE deleted = true;

-- Create an index on original_machining_batch_number and tenant_id to improve query performance
-- This will speed up lookups when checking if a batch number was previously used
CREATE INDEX idx_machining_batch_original_number_tenant
ON machining_batch (original_machining_batch_number, tenant_id, deleted);

-- Create a filtered index for active (non-deleted) batch numbers to enforce uniqueness
-- This is more efficient than the constraint and helps with the existsByMachiningBatchNumberAndTenantIdAndDeletedFalse query
CREATE INDEX idx_machining_batch_active_number_tenant
ON machining_batch (machining_batch_number, tenant_id) 
WHERE deleted = false;

-- Add comment explaining the purpose of this column
COMMENT ON COLUMN machining_batch.original_machining_batch_number IS 'Stores the original machining batch number before deletion, allowing tracking of previously used batch numbers'; 