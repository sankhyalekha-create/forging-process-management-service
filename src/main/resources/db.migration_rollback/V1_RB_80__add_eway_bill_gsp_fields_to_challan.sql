-- Rollback Script: Remove E-Way Bill GSP API fields from Delivery Challan table
-- Version: V1_RB_80
-- Description: Rollback for V1_80__add_eway_bill_gsp_fields_to_challan.sql

BEGIN;

-- Remove E-Way Bill GSP API fields from delivery_challan table
ALTER TABLE delivery_challan DROP COLUMN IF EXISTS eway_bill_alert_message;
ALTER TABLE delivery_challan DROP COLUMN IF EXISTS eway_bill_details_json;
ALTER TABLE delivery_challan DROP COLUMN IF EXISTS eway_bill_supply_type;
ALTER TABLE delivery_challan DROP COLUMN IF EXISTS eway_bill_sub_supply_type;
ALTER TABLE delivery_challan DROP COLUMN IF EXISTS eway_bill_doc_type;
ALTER TABLE delivery_challan DROP COLUMN IF EXISTS eway_bill_transaction_type;

COMMIT;
