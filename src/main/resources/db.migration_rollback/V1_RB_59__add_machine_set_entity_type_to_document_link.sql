-- Rollback Migration: Remove MACHINE_SET entity type support from document_link table
-- Version: V1_RB_59
-- Description: Removes MACHINE_SET from the allowed entity_type values in document_link table CHECK constraint
-- Rollback for: V1_59__add_machine_set_entity_type_to_document_link.sql

-- Find and drop the current CHECK constraint on entity_type column
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
        RAISE NOTICE 'Dropped constraint with MACHINE_SET: %', constraint_name;
    END IF;
END $$;

-- Restore the original CHECK constraint WITHOUT MACHINE_SET
-- This matches the original constraint from V1_55__document_management_system.sql
ALTER TABLE document_link ADD CONSTRAINT document_link_entity_type_check 
    CHECK (entity_type IN (
        'RAW_MATERIAL', 'ITEM', 'PRODUCT',
        'FORGE', 'MACHINING_BATCH', 'HEAT_TREATMENT_BATCH', 'INSPECTION_BATCH', 'DISPATCH_BATCH',
        'FORGING_LINE', 'MACHINE', 'FURNACE', 'INSPECTION_EQUIPMENT', 'OPERATOR',
        'VENDOR_DISPATCH_BATCH', 'VENDOR_RECEIVE_BATCH', 'BUYER', 'SUPPLIER',
        'TENANT', 'OTHER'
    ));

-- Add a comment to document the rollback
COMMENT ON CONSTRAINT document_link_entity_type_check ON document_link IS 
    'Ensures entity_type contains only valid entity types - MACHINE_SET removed in rollback V1_RB_59';

-- Note: Any existing document_link records with entity_type = 'MACHINE_SET' 
-- will need to be handled manually before running this rollback
-- You may want to run this query first to check:
-- SELECT COUNT(*) FROM document_link WHERE entity_type = 'MACHINE_SET';
