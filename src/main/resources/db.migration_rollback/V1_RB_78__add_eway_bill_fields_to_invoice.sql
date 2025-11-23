-- Rollback Script for V1_78: Remove E-Way Bill fields from Invoice table
-- Description: Removes E-Way Bill form fields and alert message field added in V1_78

-- Drop indexes first
DROP INDEX IF EXISTS idx_invoice_eway_bill_alert_message;
DROP INDEX IF EXISTS idx_invoice_eway_bill_valid_until;
DROP INDEX IF EXISTS idx_invoice_eway_bill_number;

-- Drop E-Way Bill form fields
ALTER TABLE invoice DROP COLUMN IF EXISTS eway_bill_transaction_type;
ALTER TABLE invoice DROP COLUMN IF EXISTS eway_bill_doc_type;
ALTER TABLE invoice DROP COLUMN IF EXISTS eway_bill_sub_supply_type;
ALTER TABLE invoice DROP COLUMN IF EXISTS eway_bill_supply_type;

-- Drop E-Way Bill details JSON field
ALTER TABLE invoice DROP COLUMN IF EXISTS eway_bill_details_json;

-- Drop alert message field
ALTER TABLE invoice DROP COLUMN IF EXISTS eway_bill_alert_message;

-- Revert comment on eway_bill_number to original
COMMENT ON COLUMN invoice.eway_bill_number IS 'E-Way Bill Number (12 digits) - Stored after manual generation on GST portal';
