-- Migration: Update bank_account_number field length to 18 characters
-- Purpose: Comply with GST E-Invoice requirement (max 18 characters for bank account number)
-- Affects: transporter, tenant_invoice_settings, invoice, delivery_challan tables
-- Date: 2025-11-24

-- ======================================================================
-- PART 1: Update bank_account_number field length
-- ======================================================================

-- Update transporter table
ALTER TABLE transporter 
ALTER COLUMN bank_account_number TYPE VARCHAR(18);

-- Update tenant_invoice_settings table
ALTER TABLE tenant_invoice_settings 
ALTER COLUMN account_number TYPE VARCHAR(18);

-- Update invoice table
ALTER TABLE invoice 
ALTER COLUMN account_number TYPE VARCHAR(18);

-- Update delivery_challan table
ALTER TABLE delivery_challan 
ALTER COLUMN account_number TYPE VARCHAR(18);

-- ======================================================================
-- PART 2: Add INVOICE and CHALLAN entity types to document_link table
-- ======================================================================

-- Find and drop the existing CHECK constraint on entity_type column
DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    -- Find the CHECK constraint on entity_type column
    SELECT conname INTO constraint_name
    FROM pg_constraint c
    JOIN pg_class t ON c.conrelid = t.oid
    JOIN pg_namespace n ON t.relnamespace = n.oid
    WHERE n.nspname = 'public' 
      AND t.relname = 'document_link' 
      AND c.contype = 'c'
      AND pg_get_constraintdef(c.oid) LIKE '%entity_type%';
    
    -- Drop the existing constraint if found
    IF constraint_name IS NOT NULL THEN
        EXECUTE 'ALTER TABLE document_link DROP CONSTRAINT ' || constraint_name;
        RAISE NOTICE 'Dropped existing constraint: %', constraint_name;
    END IF;
END $$;

-- Add the new CHECK constraint that includes INVOICE and CHALLAN
ALTER TABLE document_link ADD CONSTRAINT document_link_entity_type_check 
    CHECK (entity_type IN (
        'RAW_MATERIAL', 'ITEM', 'PRODUCT',
        'ORDER', 'ITEM_WORKFLOW',
        'FORGE', 'MACHINING_BATCH', 'HEAT_TREATMENT_BATCH', 'INSPECTION_BATCH', 'DISPATCH_BATCH',
        'FORGING_LINE', 'MACHINE', 'MACHINE_SET', 'FURNACE', 'INSPECTION_EQUIPMENT', 'OPERATOR',
        'VENDOR_DISPATCH_BATCH', 'VENDOR_RECEIVE_BATCH', 'BUYER', 'SUPPLIER', 'TRANSPORTER',
        'INVOICE', 'CHALLAN',
        'TENANT', 'OTHER'
    ));

-- Add a comment to document the change
COMMENT ON CONSTRAINT document_link_entity_type_check ON document_link IS 
    'Ensures entity_type contains only valid entity types. Updated in V1_81 to include INVOICE and CHALLAN for GST document PDF caching support';

-- ======================================================================
-- MIGRATION SUMMARY
-- ======================================================================
-- Changes Made:
-- 1. Updated bank_account_number field length to 18 characters (GST E-Invoice compliance)
-- 2. Added INVOICE and CHALLAN entity types to document_link table
-- 3. Added ORDER, ITEM_WORKFLOW, and TRANSPORTER entity types (sync with DocumentLink.java)
--
-- Purpose:
-- - INVOICE and CHALLAN entity types enable PDF caching for E-Way Bills and E-Invoices
-- - Reduces redundant GSP API calls by storing generated PDFs
-- - Improves performance and user experience
-- ======================================================================
