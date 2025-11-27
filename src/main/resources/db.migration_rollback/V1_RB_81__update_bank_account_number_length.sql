-- Rollback Migration: V1_81
-- Purpose: Revert bank_account_number field length and document_link entity_type constraint changes
-- Date: 2025-11-24

-- ======================================================================
-- PART 1: Rollback bank_account_number field length changes
-- ======================================================================

-- Update transporter table
ALTER TABLE transporter 
ALTER COLUMN bank_account_number TYPE VARCHAR(20);

-- Update tenant_invoice_settings table
ALTER TABLE tenant_invoice_settings 
ALTER COLUMN account_number TYPE VARCHAR(20);

-- Update invoice table
ALTER TABLE invoice 
ALTER COLUMN account_number TYPE VARCHAR(20);

-- Update delivery_challan table
ALTER TABLE delivery_challan 
ALTER COLUMN account_number TYPE VARCHAR(20);

-- ======================================================================
-- PART 2: Rollback document_link entity_type constraint changes
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

-- Restore the previous CHECK constraint (without INVOICE, CHALLAN, ORDER, ITEM_WORKFLOW, TRANSPORTER)
ALTER TABLE document_link ADD CONSTRAINT document_link_entity_type_check 
    CHECK (entity_type IN (
        'RAW_MATERIAL', 'ITEM', 'PRODUCT',
        'FORGE', 'MACHINING_BATCH', 'HEAT_TREATMENT_BATCH', 'INSPECTION_BATCH', 'DISPATCH_BATCH',
        'FORGING_LINE', 'MACHINE', 'MACHINE_SET', 'FURNACE', 'INSPECTION_EQUIPMENT', 'OPERATOR',
        'VENDOR_DISPATCH_BATCH', 'VENDOR_RECEIVE_BATCH', 'BUYER', 'SUPPLIER',
        'TENANT', 'OTHER'
    ));

-- Restore the previous comment
COMMENT ON CONSTRAINT document_link_entity_type_check ON document_link IS 
    'Ensures entity_type contains only valid entity types including MACHINE_SET added in V1_59';

-- ======================================================================
-- ROLLBACK SUMMARY
-- ======================================================================
-- Changes Reverted:
-- 1. bank_account_number field length reverted from VARCHAR(18) to VARCHAR(20)
-- 2. Removed INVOICE, CHALLAN, ORDER, ITEM_WORKFLOW, and TRANSPORTER from document_link entity_type constraint
-- 3. Restored constraint to V1_59 state
--
-- Note: This rollback should only be executed if V1_81 migration needs to be undone.
-- Any documents linked to INVOICE or CHALLAN entities will remain but cannot be linked to new ones.
-- ======================================================================
