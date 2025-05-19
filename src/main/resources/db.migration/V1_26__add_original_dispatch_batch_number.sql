-- Add original_dispatch_batch_number column to dispatch_batch table
ALTER TABLE dispatch_batch
ADD COLUMN original_dispatch_batch_number VARCHAR(255);

-- Update existing records to have the same value for original number as their current batch number
-- This ensures consistency for any previously deleted batches
UPDATE dispatch_batch
SET original_dispatch_batch_number = dispatch_batch_number
WHERE deleted = true;

-- Create an index on original_dispatch_batch_number and tenant_id to improve query performance
-- This will speed up lookups when checking if a batch number was previously used
CREATE INDEX idx_dispatch_batch_original_number_tenant
ON dispatch_batch (original_dispatch_batch_number, tenant_id, deleted);

-- Create a filtered index for active (non-deleted) batch numbers to enforce uniqueness
-- This is more efficient than the constraint and helps with the exists query
CREATE INDEX idx_dispatch_batch_active_number_tenant
ON dispatch_batch (dispatch_batch_number, tenant_id) 
WHERE deleted = false;

-- Add comment explaining the purpose of this column
COMMENT ON COLUMN dispatch_batch.original_dispatch_batch_number IS 'Stores the original dispatch batch number before deletion, allowing tracking of previously used batch numbers'; 