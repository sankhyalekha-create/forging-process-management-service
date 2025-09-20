-- Migration: Add MACHINE_SET entity type support to document_link table
-- Version: V1_59
-- Description: Adds MACHINE_SET to the allowed entity_type values in document_link table CHECK constraint

-- Find and drop the existing CHECK constraint on entity_type column
-- PostgreSQL auto-generates constraint names, so we need to find it dynamically
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

-- Add the new CHECK constraint that includes MACHINE_SET
ALTER TABLE document_link ADD CONSTRAINT document_link_entity_type_check 
    CHECK (entity_type IN (
        'RAW_MATERIAL', 'ITEM', 'PRODUCT',
        'FORGE', 'MACHINING_BATCH', 'HEAT_TREATMENT_BATCH', 'INSPECTION_BATCH', 'DISPATCH_BATCH',
        'FORGING_LINE', 'MACHINE', 'MACHINE_SET', 'FURNACE', 'INSPECTION_EQUIPMENT', 'OPERATOR',
        'VENDOR_DISPATCH_BATCH', 'VENDOR_RECEIVE_BATCH', 'BUYER', 'SUPPLIER',
        'TENANT', 'OTHER'
    ));

-- Add a comment to document the change
COMMENT ON CONSTRAINT document_link_entity_type_check ON document_link IS 
    'Ensures entity_type contains only valid entity types including MACHINE_SET added in V1_59';
