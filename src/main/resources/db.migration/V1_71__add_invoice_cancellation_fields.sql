-- ===================================
-- Invoice Cancellation Support
-- Version: V1_71
-- Description: Adds cancellation tracking fields to invoice table
-- ===================================

-- Add cancellation tracking fields
ALTER TABLE invoice 
ADD COLUMN cancelled_by VARCHAR(100),
ADD COLUMN cancellation_reason VARCHAR(500),
ADD COLUMN cancellation_date TIMESTAMP;

-- Add index for cancellation queries
CREATE INDEX idx_invoice_cancellation_date ON invoice(cancellation_date);

-- Add comments for documentation
COMMENT ON COLUMN invoice.cancelled_by IS 'Name of person who cancelled the invoice';
COMMENT ON COLUMN invoice.cancellation_reason IS 'Reason for invoice cancellation';
COMMENT ON COLUMN invoice.cancellation_date IS 'Date and time when invoice was cancelled';

