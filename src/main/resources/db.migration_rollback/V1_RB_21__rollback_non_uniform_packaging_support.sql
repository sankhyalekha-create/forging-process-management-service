-- Drop indexes
DROP INDEX IF EXISTS idx_dispatch_package_deleted;
DROP INDEX IF EXISTS idx_dispatch_package_dispatch_batch;

-- Drop the dispatch_package table
DROP TABLE IF EXISTS dispatch_package;

-- Drop the sequence
DROP SEQUENCE IF EXISTS dispatch_package_sequence;

-- Remove the use_uniform_packaging column from dispatch_batch table
ALTER TABLE dispatch_batch
DROP COLUMN IF EXISTS use_uniform_packaging; 