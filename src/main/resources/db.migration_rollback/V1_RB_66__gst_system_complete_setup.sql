BEGIN;


DROP TABLE IF EXISTS gst_configuration CASCADE;
DROP SEQUENCE IF EXISTS gst_configuration_sequence;

DROP TABLE IF EXISTS eway_bill CASCADE;
DROP SEQUENCE IF EXISTS eway_bill_sequence;

DROP TABLE IF EXISTS invoice_line_item CASCADE;
DROP SEQUENCE IF EXISTS invoice_line_item_sequence;

DROP TABLE IF EXISTS invoice CASCADE;
DROP SEQUENCE IF EXISTS invoice_sequence;

DROP TABLE IF EXISTS delivery_challan CASCADE;
DROP SEQUENCE IF EXISTS delivery_challan_sequence;


DROP INDEX IF EXISTS idx_dispatch_batch_requires_eway_bill;
DROP INDEX IF EXISTS idx_dispatch_batch_hsn_code;
DROP INDEX IF EXISTS idx_dispatch_batch_gstin;

ALTER TABLE dispatch_batch DROP CONSTRAINT IF EXISTS chk_dispatch_transportation_distance;
ALTER TABLE dispatch_batch DROP CONSTRAINT IF EXISTS chk_dispatch_transportation_mode;

ALTER TABLE dispatch_batch
DROP COLUMN IF EXISTS eway_bill_threshold_met,
DROP COLUMN IF EXISTS requires_eway_bill,
DROP COLUMN IF EXISTS transportation_distance,
DROP COLUMN IF EXISTS transportation_mode,
DROP COLUMN IF EXISTS total_amount,
DROP COLUMN IF EXISTS total_tax_amount,
DROP COLUMN IF EXISTS igst_amount,
DROP COLUMN IF EXISTS sgst_amount,
DROP COLUMN IF EXISTS cgst_amount,
DROP COLUMN IF EXISTS taxable_value,
DROP COLUMN IF EXISTS hsn_code,
DROP COLUMN IF EXISTS gstin;


DROP INDEX IF EXISTS idx_vendor_entity_state_code;
DROP INDEX IF EXISTS idx_vendor_state_code;
DROP INDEX IF EXISTS idx_buyer_entity_state_code;
DROP INDEX IF EXISTS idx_buyer_state_code;

ALTER TABLE vendor_entity DROP CONSTRAINT IF EXISTS chk_vendor_entity_state_code_format;
ALTER TABLE vendor_entity DROP CONSTRAINT IF EXISTS chk_vendor_entity_pincode_format;
ALTER TABLE vendor DROP CONSTRAINT IF EXISTS chk_vendor_state_code_format;
ALTER TABLE vendor DROP CONSTRAINT IF EXISTS chk_vendor_pincode_format;
ALTER TABLE buyer_entity DROP CONSTRAINT IF EXISTS chk_buyer_entity_state_code_format;
ALTER TABLE buyer_entity DROP CONSTRAINT IF EXISTS chk_buyer_entity_pincode_format;
ALTER TABLE buyer DROP CONSTRAINT IF EXISTS chk_buyer_state_code_format;
ALTER TABLE buyer DROP CONSTRAINT IF EXISTS chk_buyer_pincode_format;

COMMIT;