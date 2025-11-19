BEGIN;

-- ==========================================
-- Combined Migration: Invoice Enhancements, City Fields, and Line Item Details
-- ==========================================
-- This migration includes:
-- 1. Add Buyer/Vendor Entity Fields to Invoice and Remove Deprecated Recipient Fields
-- 2. Add LR (Lorry Receipt) fields for E-Way Bill compliance
-- 3. Add city field to tenant, buyer, vendor, buyer_entity, and vendor_entity tables
-- 4. Add Finished Good and RM Product details to invoice_line_item table
-- ==========================================

-- ==========================================
-- PART 1: Invoice Table - Buyer/Vendor Entities
-- ==========================================

-- Step 1: Drop old recipient-related constraints and indexes
-- ==========================================

-- Drop the single recipient check constraint
ALTER TABLE invoice
DROP CONSTRAINT IF EXISTS chk_invoice_single_recipient;

-- Drop old recipient indexes
DROP INDEX IF EXISTS idx_invoice_recipient_buyer;
DROP INDEX IF EXISTS idx_invoice_recipient_vendor;

-- Drop old recipient foreign key constraints
ALTER TABLE invoice
DROP CONSTRAINT IF EXISTS fk_invoice_recipient_buyer_entity,
DROP CONSTRAINT IF EXISTS fk_invoice_recipient_vendor_entity;

-- Step 2: Remove old recipient columns
-- ==========================================

ALTER TABLE invoice
DROP COLUMN IF EXISTS recipient_buyer_entity_id,
DROP COLUMN IF EXISTS recipient_vendor_entity_id;

-- Step 3: Add new buyer and vendor references
-- ==========================================

ALTER TABLE invoice
ADD COLUMN buyer_id BIGINT,
ADD COLUMN vendor_id BIGINT;

-- Step 4: Add buyer billing and shipping entity references
-- ==========================================

ALTER TABLE invoice
ADD COLUMN buyer_billing_entity_id BIGINT,
ADD COLUMN buyer_shipping_entity_id BIGINT;

-- Step 5: Add vendor billing and shipping entity references
-- ==========================================

ALTER TABLE invoice
ADD COLUMN vendor_billing_entity_id BIGINT,
ADD COLUMN vendor_shipping_entity_id BIGINT;

-- Step 6: Add foreign key constraints
-- ==========================================
ALTER TABLE invoice
ADD CONSTRAINT fk_invoice_buyer FOREIGN KEY (buyer_id) REFERENCES buyer(id),
ADD CONSTRAINT fk_invoice_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(id),
ADD CONSTRAINT fk_invoice_buyer_billing_entity FOREIGN KEY (buyer_billing_entity_id) REFERENCES buyer_entity(id),
ADD CONSTRAINT fk_invoice_buyer_shipping_entity FOREIGN KEY (buyer_shipping_entity_id) REFERENCES buyer_entity(id),
ADD CONSTRAINT fk_invoice_vendor_billing_entity FOREIGN KEY (vendor_billing_entity_id) REFERENCES vendor_entity(id),
ADD CONSTRAINT fk_invoice_vendor_shipping_entity FOREIGN KEY (vendor_shipping_entity_id) REFERENCES vendor_entity(id);

-- Step 7: Add indexes for better query performance
-- ==========================================
CREATE INDEX idx_invoice_buyer ON invoice(buyer_id);
CREATE INDEX idx_invoice_vendor ON invoice(vendor_id);
CREATE INDEX idx_invoice_buyer_billing_entity ON invoice(buyer_billing_entity_id);
CREATE INDEX idx_invoice_buyer_shipping_entity ON invoice(buyer_shipping_entity_id);
CREATE INDEX idx_invoice_vendor_billing_entity ON invoice(vendor_billing_entity_id);
CREATE INDEX idx_invoice_vendor_shipping_entity ON invoice(vendor_shipping_entity_id);

-- Step 8: Add comments for documentation
-- ==========================================
COMMENT ON COLUMN invoice.buyer_id IS 'Reference to the main buyer (customer) for this invoice';
COMMENT ON COLUMN invoice.vendor_id IS 'Reference to the main vendor for this invoice';
COMMENT ON COLUMN invoice.buyer_billing_entity_id IS 'Specific buyer billing address entity for this invoice';
COMMENT ON COLUMN invoice.buyer_shipping_entity_id IS 'Specific buyer shipping address entity for this invoice';
COMMENT ON COLUMN invoice.vendor_billing_entity_id IS 'Specific vendor billing address entity for this invoice';
COMMENT ON COLUMN invoice.vendor_shipping_entity_id IS 'Specific vendor shipping address entity for this invoice';

-- ==========================================
-- PART 2: Invoice Table - LR Fields for E-Way Bill
-- ==========================================

ALTER TABLE invoice
ADD COLUMN transport_document_number VARCHAR(50),
ADD COLUMN transport_document_date DATE,
ADD COLUMN remarks VARCHAR(500);

COMMENT ON COLUMN invoice.transport_document_number IS 'Transport document number (LR Number for ROAD, RR for RAIL, AWB for AIR, BOL for SHIP)';
COMMENT ON COLUMN invoice.transport_document_date IS 'Transport document date (LR Date, RR Date, etc.)';
COMMENT ON COLUMN invoice.remarks IS 'Additional remarks or special notes for the invoice';

-- ==========================================
-- PART 3: Add City Field to Entity Tables
-- ==========================================

-- Add city field to tenant table
ALTER TABLE tenant ADD COLUMN city VARCHAR(100);

-- Add city field to buyer table
ALTER TABLE buyer ADD COLUMN city VARCHAR(100);

-- Add city field to vendor table
ALTER TABLE vendor ADD COLUMN city VARCHAR(100);

-- Add city field to buyer_entity table
ALTER TABLE buyer_entity ADD COLUMN city VARCHAR(100);

-- Add city field to vendor_entity table
ALTER TABLE vendor_entity ADD COLUMN city VARCHAR(100);

-- ==========================================
-- PART 4: Invoice Line Item - Finished Good and RM Product Details
-- ==========================================

ALTER TABLE invoice_line_item
ADD COLUMN finished_good_name VARCHAR(500),
ADD COLUMN finished_good_code VARCHAR(100),
ADD COLUMN rm_product_names VARCHAR(500),
ADD COLUMN rm_product_codes VARCHAR(100),
ADD COLUMN rm_invoice_numbers VARCHAR(100),
ADD COLUMN rm_heat_numbers VARCHAR(100),
ADD COLUMN heat_tracebility_numbers VARCHAR(100);

COMMENT ON COLUMN invoice_line_item.finished_good_name IS 'Name of the finished good item';
COMMENT ON COLUMN invoice_line_item.finished_good_code IS 'Code of the finished good item';
COMMENT ON COLUMN invoice_line_item.rm_product_names IS 'Name of the raw material product used';
COMMENT ON COLUMN invoice_line_item.rm_product_codes IS 'Code of the raw material product used';
COMMENT ON COLUMN invoice_line_item.rm_invoice_numbers IS 'Invoice number of the raw material product';
COMMENT ON COLUMN invoice_line_item.rm_heat_numbers IS 'Heat number of the raw material product';
COMMENT ON COLUMN invoice_line_item.heat_tracebility_numbers IS 'Heat tracebility number of the raw material product';

COMMIT;
