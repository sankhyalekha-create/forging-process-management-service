BEGIN;

-- ==========================================
-- Combined Rollback for V1_76 Migration
-- ==========================================
-- This rollback script reverses:
-- 1. Invoice Line Item - Finished Good and RM Product details
-- 2. City fields from entity tables
-- 3. LR fields from invoice table
-- 4. Buyer/Vendor entity fields from invoice table
-- 
-- WARNING: This will drop foreign key relationships, indexes, and data.
-- Ensure you have a backup before running this rollback.
-- ==========================================

-- ==========================================
-- PART 1: Rollback Invoice Line Item Changes
-- ==========================================

ALTER TABLE invoice_line_item
DROP COLUMN IF EXISTS finished_good_name,
DROP COLUMN IF EXISTS finished_good_code,
DROP COLUMN IF EXISTS rm_product_names,
DROP COLUMN IF EXISTS rm_product_codes,
DROP COLUMN IF EXISTS rm_invoice_numbers,
DROP COLUMN IF EXISTS rm_heat_numbers;
DROP COLUMN IF EXISTS heat_tracebility_numbers;

-- ==========================================
-- PART 2: Rollback City Fields from Entity Tables
-- ==========================================

-- Remove city field from vendor_entity table
ALTER TABLE vendor_entity DROP COLUMN IF EXISTS city;

-- Remove city field from buyer_entity table
ALTER TABLE buyer_entity DROP COLUMN IF EXISTS city;

-- Remove city field from vendor table
ALTER TABLE vendor DROP COLUMN IF EXISTS city;

-- Remove city field from buyer table
ALTER TABLE buyer DROP COLUMN IF EXISTS city;

-- Remove city field from tenant table
ALTER TABLE tenant DROP COLUMN IF EXISTS city;

-- ==========================================
-- PART 3: Rollback Invoice Table Changes
-- ==========================================

-- Step 1: Drop new indexes
-- ==========================================

DROP INDEX IF EXISTS idx_invoice_buyer;
DROP INDEX IF EXISTS idx_invoice_vendor;
DROP INDEX IF EXISTS idx_invoice_buyer_billing_entity;
DROP INDEX IF EXISTS idx_invoice_buyer_shipping_entity;
DROP INDEX IF EXISTS idx_invoice_vendor_billing_entity;
DROP INDEX IF EXISTS idx_invoice_vendor_shipping_entity;

-- Step 2: Drop new foreign key constraints
-- ==========================================

ALTER TABLE invoice
DROP CONSTRAINT IF EXISTS fk_invoice_buyer,
DROP CONSTRAINT IF EXISTS fk_invoice_vendor,
DROP CONSTRAINT IF EXISTS fk_invoice_buyer_billing_entity,
DROP CONSTRAINT IF EXISTS fk_invoice_buyer_shipping_entity,
DROP CONSTRAINT IF EXISTS fk_invoice_vendor_billing_entity,
DROP CONSTRAINT IF EXISTS fk_invoice_vendor_shipping_entity;

-- Step 3: Drop new columns (including LR fields)
-- ==========================================

ALTER TABLE invoice
DROP COLUMN IF EXISTS buyer_id,
DROP COLUMN IF EXISTS vendor_id,
DROP COLUMN IF EXISTS buyer_billing_entity_id,
DROP COLUMN IF EXISTS buyer_shipping_entity_id,
DROP COLUMN IF EXISTS vendor_billing_entity_id,
DROP COLUMN IF EXISTS vendor_shipping_entity_id,
DROP COLUMN IF EXISTS transport_document_number,
DROP COLUMN IF EXISTS transport_document_date,
DROP COLUMN IF EXISTS remarks;

-- Step 4: Restore old recipient columns
-- ==========================================

ALTER TABLE invoice
ADD COLUMN recipient_buyer_entity_id BIGINT,
ADD COLUMN recipient_vendor_entity_id BIGINT;

-- Step 5: Add foreign key constraints for old recipient columns
-- ==========================================

ALTER TABLE invoice
ADD CONSTRAINT fk_invoice_recipient_buyer_entity 
    FOREIGN KEY (recipient_buyer_entity_id) REFERENCES buyer_entity(id),
ADD CONSTRAINT fk_invoice_recipient_vendor_entity 
    FOREIGN KEY (recipient_vendor_entity_id) REFERENCES vendor_entity(id);

-- Step 6: Restore old recipient indexes
-- ==========================================

CREATE INDEX idx_invoice_recipient_buyer ON invoice(recipient_buyer_entity_id);
CREATE INDEX idx_invoice_recipient_vendor ON invoice(recipient_vendor_entity_id);

-- Step 7: Restore the single recipient check constraint
-- ==========================================

ALTER TABLE invoice
ADD CONSTRAINT chk_invoice_single_recipient CHECK (
    (recipient_buyer_entity_id IS NOT NULL AND recipient_vendor_entity_id IS NULL) OR
    (recipient_buyer_entity_id IS NULL AND recipient_vendor_entity_id IS NOT NULL)
);

COMMIT;
