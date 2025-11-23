-- Migration Script: Add E-Way Bill fields to Invoice table
-- Version: V1_60
-- Description: Adds E-Way Bill form fields and response fields for tracking E-Way Bill generation

-- Add E-Way Bill alert field
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS eway_bill_alert_message VARCHAR(500);

-- Add E-Way Bill details JSON field (stores complete GetEwayBill API response)
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS eway_bill_details_json TEXT;
COMMENT ON COLUMN invoice.eway_bill_details_json IS 'Complete E-Way Bill details JSON from GetEwayBill API - cached for printing';

-- Add E-Way Bill form fields (captured during generation)
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS eway_bill_supply_type VARCHAR(1);
COMMENT ON COLUMN invoice.eway_bill_supply_type IS 'Supply Type: O = Outward, I = Inward';

ALTER TABLE invoice ADD COLUMN IF NOT EXISTS eway_bill_sub_supply_type VARCHAR(2);
COMMENT ON COLUMN invoice.eway_bill_sub_supply_type IS 'Sub Supply Type: 1=Supply, 2=Import, 3=Export, 4=Job Work, etc.';

ALTER TABLE invoice ADD COLUMN IF NOT EXISTS eway_bill_doc_type VARCHAR(3);
COMMENT ON COLUMN invoice.eway_bill_doc_type IS 'Document Type: INV, BIL, BOE, CHL, OTH';

ALTER TABLE invoice ADD COLUMN IF NOT EXISTS eway_bill_transaction_type VARCHAR(1);
COMMENT ON COLUMN invoice.eway_bill_transaction_type IS 'Transaction Type: 1=Regular, 2=Bill To-Ship To, 3=Bill From-Dispatch From, 4=Combination';

-- Update existing comment for eway_bill_number to reflect GSP API generation
COMMENT ON COLUMN invoice.eway_bill_number IS 'E-Way Bill Number (12 digits) - Generated via GSP API';

-- Create index for E-Way Bill queries
CREATE INDEX IF NOT EXISTS idx_invoice_eway_bill_number ON invoice(eway_bill_number) WHERE eway_bill_number IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_invoice_eway_bill_valid_until ON invoice(eway_bill_valid_until) WHERE eway_bill_valid_until IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_invoice_eway_bill_alert_message ON invoice(eway_bill_alert_message) WHERE eway_bill_alert_message IS NOT NULL;
