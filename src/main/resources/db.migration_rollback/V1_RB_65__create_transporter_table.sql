-- Rollback Migration: Remove Transporter Table and Document Link Support
-- Version: V1_64 Rollback
-- Description: 
--   Rollback script to undo V1_64 migration changes in reverse order:
--   1. Removes TRANSPORTER from document_link CHECK constraint
--   2. Drops transporter table and all associated objects
-- Date: 2025-10-24
-- WARNING: This script will permanently delete all transporter data and 
--          transporter document links. Use with caution!

BEGIN;

-- ============================================================================
-- PART 1: Remove TRANSPORTER Entity Type from Document Link
-- ============================================================================

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

-- IMPORTANT: Delete any existing TRANSPORTER document links BEFORE adding new constraint
-- This prevents constraint violation errors
DO $$
DECLARE
    deleted_count INTEGER;
BEGIN
    -- Delete TRANSPORTER document links
    DELETE FROM document_link WHERE entity_type = 'TRANSPORTER';
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'Removed % TRANSPORTER document link(s)', deleted_count;
END $$;

-- Now restore the CHECK constraint WITHOUT TRANSPORTER (back to pre-V1_64 state)
ALTER TABLE document_link ADD CONSTRAINT document_link_entity_type_check 
    CHECK (entity_type IN (
        'RAW_MATERIAL', 'ITEM', 'PRODUCT', 'ORDER', 'ITEM_WORKFLOW',
        'FORGE', 'MACHINING_BATCH', 'HEAT_TREATMENT_BATCH', 'INSPECTION_BATCH', 'DISPATCH_BATCH',
        'FORGING_LINE', 'MACHINE', 'MACHINE_SET', 'FURNACE', 'INSPECTION_EQUIPMENT', 'OPERATOR',
        'VENDOR_DISPATCH_BATCH', 'VENDOR_RECEIVE_BATCH', 'BUYER', 'SUPPLIER',
        'TENANT', 'OTHER'
    ));

-- Add a comment to document the rollback
COMMENT ON CONSTRAINT document_link_entity_type_check ON document_link IS 
    'Ensures entity_type contains only valid entity types. TRANSPORTER removed via V1_64 rollback.';

-- ============================================================================
-- PART 2: Drop Transporter Table
-- ============================================================================

-- Drop partial unique indexes
DROP INDEX IF EXISTS unique_transporter_id_number_tenant_active;
DROP INDEX IF EXISTS unique_transporter_gstin_tenant_active;
DROP INDEX IF EXISTS unique_transporter_name_tenant_active;

-- Drop performance indexes
DROP INDEX IF EXISTS idx_transporter_deleted;
DROP INDEX IF EXISTS idx_transporter_tenant_id;
DROP INDEX IF EXISTS idx_transporter_id_number;
DROP INDEX IF EXISTS idx_transporter_gstin;
DROP INDEX IF EXISTS idx_transporter_name;

-- Drop transporter table (CASCADE will drop any dependent objects)
DROP TABLE IF EXISTS transporter CASCADE;

-- Remove migration record from flyway_schema_history
-- Note: Only run this if you want to completely remove the migration record
-- DELETE FROM flyway_schema_history WHERE version = '1.64';

COMMIT;

-- ============================================================================
-- Post-Rollback Verification Queries
-- ============================================================================
-- Run these to verify the rollback was successful:
--
-- 1. Verify TRANSPORTER is not in the document_link constraint:
--    SELECT pg_get_constraintdef(c.oid) 
--    FROM pg_constraint c
--    JOIN pg_class t ON c.conrelid = t.oid
--    WHERE t.relname = 'document_link' AND c.conname = 'document_link_entity_type_check';
--    (Should NOT contain 'TRANSPORTER')
--
-- 2. Verify transporter table is dropped:
--    SELECT tablename FROM pg_tables WHERE tablename = 'transporter';
--    (Should return 0 rows)
--
-- 3. Verify indexes are dropped:
--    SELECT indexname FROM pg_indexes WHERE tablename = 'transporter';
--    (Should return 0 rows)
--
-- 4. Check if any TRANSPORTER document links still exist:
--    SELECT COUNT(*) FROM document_link WHERE entity_type = 'TRANSPORTER';
--    (Should be 0 or error "relation does not exist")
--
-- 5. Check for dependent objects (should be none):
--    SELECT * FROM information_schema.table_constraints 
--    WHERE constraint_name LIKE '%transporter%';
--    (Should return 0 rows)
