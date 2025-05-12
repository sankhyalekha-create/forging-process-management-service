-- Add invoice and purchase order fields to dispatch_batch table
ALTER TABLE dispatch_batch
ADD COLUMN invoice_number VARCHAR(255),
ADD COLUMN invoice_date_time TIMESTAMP,
ADD COLUMN purchase_order_number VARCHAR(255),
ADD COLUMN purchase_order_date_time TIMESTAMP;

-- Create index on invoice_number for faster lookups
CREATE INDEX idx_invoice_number ON dispatch_batch(invoice_number);

-- Add unique constraint for invoice_number, tenant_id, and deleted
ALTER TABLE dispatch_batch
ADD CONSTRAINT uq_invoice_number_tenant_deleted UNIQUE (invoice_number, tenant_id, deleted);

-- Add a comment to the table
COMMENT ON COLUMN dispatch_batch.invoice_number IS 'Invoice number for the dispatched batch';
COMMENT ON COLUMN dispatch_batch.invoice_date_time IS 'Date and time of the invoice (must be >= dispatch_ready_at)';
COMMENT ON COLUMN dispatch_batch.purchase_order_number IS 'Purchase order number for the dispatched batch';
COMMENT ON COLUMN dispatch_batch.purchase_order_date_time IS 'Date and time of the purchase order'; 