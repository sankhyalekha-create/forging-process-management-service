BEGIN;

-- ==========================================
-- E-Way Bill Lightweight Implementation
-- ==========================================
-- This migration implements offline E-Way Bill support by:
-- 1. Dropping the old heavyweight eway_bill table
-- 2. Adding minimal tracking fields to invoice and delivery_challan tables
-- 
-- Rationale: Store only essential fields (number, date, validity) instead of duplicating
-- all invoice/challan data. E-Way Bill JSON is generated on-demand from existing data.
-- ==========================================

-- Part 1: Drop old eway_bill table and related objects
-- ==========================================

-- Drop indexes from old eway_bill table
DROP INDEX IF EXISTS idx_eway_bill_dispatch_batch;
DROP INDEX IF EXISTS idx_eway_bill_invoice;
DROP INDEX IF EXISTS idx_eway_bill_challan;
DROP INDEX IF EXISTS idx_eway_bill_tenant_status;
DROP INDEX IF EXISTS idx_eway_bill_validity;
DROP INDEX IF EXISTS idx_eway_bill_deleted;
DROP INDEX IF EXISTS idx_eway_bill_number;
DROP INDEX IF EXISTS idx_eway_bill_date;

-- Drop the old eway_bill table
DROP TABLE IF EXISTS eway_bill;

-- Drop the sequence
DROP SEQUENCE IF EXISTS eway_bill_sequence;

-- Part 2: DROP E-Way Bill null fields from delivery_challan
-- ==========================================

ALTER TABLE delivery_challan
DROP COLUMN IF EXISTS eway_bill_number;

-- Part 2: Add E-Way Bill tracking fields to invoice table
-- ==========================================

ALTER TABLE invoice
ADD COLUMN eway_bill_number VARCHAR(12),
ADD COLUMN eway_bill_date TIMESTAMP,
ADD COLUMN eway_bill_valid_until TIMESTAMP;

-- Add index for querying invoices by E-Way Bill number
CREATE INDEX idx_invoice_eway_bill_number ON invoice(eway_bill_number) 
WHERE eway_bill_number IS NOT NULL;

-- Add comments for documentation
COMMENT ON COLUMN invoice.eway_bill_number IS 'E-Way Bill number (12 digits) generated from GST portal';
COMMENT ON COLUMN invoice.eway_bill_date IS 'Date and time when E-Way Bill was generated';
COMMENT ON COLUMN invoice.eway_bill_valid_until IS 'Validity expiry date/time of E-Way Bill';


-- Part 3: Add E-Way Bill tracking fields to delivery_challan table
-- ==========================================

ALTER TABLE delivery_challan
ADD COLUMN eway_bill_number VARCHAR(12),
ADD COLUMN eway_bill_date TIMESTAMP,
ADD COLUMN eway_bill_valid_until TIMESTAMP;

-- Add index for querying challans by E-Way Bill number
CREATE INDEX idx_challan_eway_bill_number ON delivery_challan(eway_bill_number) 
WHERE eway_bill_number IS NOT NULL;

-- Add comments for documentation
COMMENT ON COLUMN delivery_challan.eway_bill_number IS 'E-Way Bill number (12 digits) generated from GST portal';
COMMENT ON COLUMN delivery_challan.eway_bill_date IS 'Date and time when E-Way Bill was generated';
COMMENT ON COLUMN delivery_challan.eway_bill_valid_until IS 'Validity expiry date/time of E-Way Bill';

COMMIT;
