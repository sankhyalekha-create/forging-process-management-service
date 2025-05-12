-- Drop the unique constraint first
ALTER TABLE dispatch_batch
DROP CONSTRAINT IF EXISTS uq_invoice_number_tenant_deleted;

-- Drop the index
DROP INDEX IF EXISTS idx_invoice_number;

-- Remove the comments
COMMENT ON COLUMN dispatch_batch.invoice_number IS NULL;
COMMENT ON COLUMN dispatch_batch.invoice_date_time IS NULL;
COMMENT ON COLUMN dispatch_batch.purchase_order_number IS NULL;
COMMENT ON COLUMN dispatch_batch.purchase_order_date_time IS NULL;

-- Drop the columns
ALTER TABLE dispatch_batch
DROP COLUMN IF EXISTS invoice_number,
DROP COLUMN IF EXISTS invoice_date_time,
DROP COLUMN IF EXISTS purchase_order_number,
DROP COLUMN IF EXISTS purchase_order_date_time; 