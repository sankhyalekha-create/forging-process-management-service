-- Rollback: Complete Vendor Management System
-- Version: V1_RB_44
-- Description: Rollback complete vendor management with dispatch, receive, and workflow integration
-- Note: Updated to reflect simplified structure (removed quantity fields since receiving is always in pieces)
-- Note: This rollback includes the new tenant_rejects_count and total_tenant_rejects_count columns

BEGIN;

-- ======================================================================
-- PART 1: Drop vendor_dispatch_heat table and related objects (dependent on processed_item_vendor_dispatch_batch)
-- ======================================================================

-- Drop indexes first
DROP INDEX IF EXISTS idx_vendor_dispatch_heat_heat_id;
DROP INDEX IF EXISTS idx_vendor_dispatch_heat_processed_item_vendor_dispatch_batch_id;

-- Drop foreign key constraints
ALTER TABLE vendor_dispatch_heat 
DROP CONSTRAINT IF EXISTS fk_vendor_dispatch_heat_heat;

ALTER TABLE vendor_dispatch_heat 
DROP CONSTRAINT IF EXISTS fk_vendor_dispatch_heat_processed_item_vendor_dispatch_batch;

-- Drop check constraints
ALTER TABLE vendor_dispatch_heat
DROP CONSTRAINT IF EXISTS chk_vendor_dispatch_heat_quantity_consumption;

-- Drop the vendor_dispatch_heat table
DROP TABLE IF EXISTS vendor_dispatch_heat;

-- ======================================================================
-- PART 2: Drop vendor_receive_batch table and related objects
-- ======================================================================

-- Drop indexes first
DROP INDEX IF EXISTS idx_vendor_receive_quality_check;
DROP INDEX IF EXISTS idx_vendor_receive_batch_status;
DROP INDEX IF EXISTS idx_vendor_receive_batch_dispatch_batch_id;
DROP INDEX IF EXISTS idx_vendor_receive_batch_vendor_id;
DROP INDEX IF EXISTS idx_vendor_receive_batch_number;

-- Drop foreign key constraints
ALTER TABLE vendor_receive_batch 
DROP CONSTRAINT IF EXISTS fk_vendor_receive_batch_shipping_entity;

ALTER TABLE vendor_receive_batch 
DROP CONSTRAINT IF EXISTS fk_vendor_receive_batch_billing_entity;

ALTER TABLE vendor_receive_batch 
DROP CONSTRAINT IF EXISTS fk_vendor_receive_batch_dispatch_batch;

ALTER TABLE vendor_receive_batch 
DROP CONSTRAINT IF EXISTS fk_vendor_receive_batch_vendor;

ALTER TABLE vendor_receive_batch 
DROP CONSTRAINT IF EXISTS fk_vendor_receive_batch_tenant;

-- Drop unique constraints
ALTER TABLE vendor_receive_batch 
DROP CONSTRAINT IF EXISTS unique_vendor_receive_batch_number_tenant_deleted;

-- Drop the vendor_receive_batch table
DROP TABLE IF EXISTS vendor_receive_batch;

-- ======================================================================
-- PART 3: Drop processed_item_vendor_dispatch_batch table and related objects
-- ======================================================================

-- Drop indexes first (will cascade with table but being explicit)
DROP INDEX IF EXISTS idx_processed_item_vendor_dispatch_batch_deleted;
DROP INDEX IF EXISTS idx_processed_item_vendor_dispatch_batch_item_status;
DROP INDEX IF EXISTS idx_processed_item_vendor_dispatch_batch_workflow_identifier;
DROP INDEX IF EXISTS idx_processed_item_vendor_dispatch_batch_item_workflow_id;
DROP INDEX IF EXISTS idx_processed_item_vendor_dispatch_batch_item_id;
DROP INDEX IF EXISTS idx_processed_item_vendor_dispatch_batch_vendor_dispatch_batch_id;

-- Drop foreign key constraints (will cascade with table but being explicit)
ALTER TABLE processed_item_vendor_dispatch_batch 
DROP CONSTRAINT IF EXISTS fk_processed_item_vendor_dispatch_batch_item_workflow;

ALTER TABLE processed_item_vendor_dispatch_batch 
DROP CONSTRAINT IF EXISTS fk_processed_item_vendor_dispatch_batch_item;

ALTER TABLE processed_item_vendor_dispatch_batch 
DROP CONSTRAINT IF EXISTS fk_processed_item_vendor_dispatch_batch_vendor_dispatch_batch;

-- Drop unique constraint
ALTER TABLE processed_item_vendor_dispatch_batch 
DROP CONSTRAINT IF EXISTS unique_processed_item_vendor_dispatch_batch_vendor_dispatch_batch;

-- Drop the processed_item_vendor_dispatch_batch table
DROP TABLE IF EXISTS processed_item_vendor_dispatch_batch;

-- ======================================================================
-- PART 4: Drop vendor_dispatch_batch table and related objects
-- ======================================================================

-- Drop vendor_dispatch_batch_processes table first (dependent on vendor_dispatch_batch)
DROP INDEX IF EXISTS idx_vendor_dispatch_batch_processes_vendor_dispatch_batch_id;

-- Drop foreign key constraint
ALTER TABLE vendor_dispatch_batch_processes 
DROP CONSTRAINT IF EXISTS fk_vendor_dispatch_batch_processes_vendor_dispatch_batch;

-- Drop the vendor_dispatch_batch_processes table
DROP TABLE IF EXISTS vendor_dispatch_batch_processes;

-- Drop indexes for vendor_dispatch_batch
DROP INDEX IF EXISTS idx_vendor_dispatch_batch_status;
DROP INDEX IF EXISTS idx_vendor_dispatch_batch_vendor_id;
DROP INDEX IF EXISTS idx_vendor_dispatch_batch_number;

-- Drop foreign key constraints
ALTER TABLE vendor_dispatch_batch 
DROP CONSTRAINT IF EXISTS fk_vendor_dispatch_batch_shipping_entity;

ALTER TABLE vendor_dispatch_batch 
DROP CONSTRAINT IF EXISTS fk_vendor_dispatch_batch_billing_entity;

ALTER TABLE vendor_dispatch_batch 
DROP CONSTRAINT IF EXISTS fk_vendor_dispatch_batch_vendor;

ALTER TABLE vendor_dispatch_batch 
DROP CONSTRAINT IF EXISTS fk_vendor_dispatch_batch_tenant;

-- Drop unique constraints
ALTER TABLE vendor_dispatch_batch 
DROP CONSTRAINT IF EXISTS unique_vendor_dispatch_batch_number_tenant_deleted;

-- Drop the vendor_dispatch_batch table
DROP TABLE IF EXISTS vendor_dispatch_batch;

-- ======================================================================
-- PART 5: Drop vendor_entity table and related objects
-- ======================================================================

-- Drop indexes first
DROP INDEX IF EXISTS idx_vendor_entity_pan_number;
DROP INDEX IF EXISTS idx_vendor_entity_vendor_id;
DROP INDEX IF EXISTS idx_vendor_entity_name;

-- Drop foreign key constraints
ALTER TABLE vendor_entity 
DROP CONSTRAINT IF EXISTS fk_vendor_entity_vendor;

-- Drop unique constraints
ALTER TABLE vendor_entity 
DROP CONSTRAINT IF EXISTS unique_entity_name_vendor_deleted;

-- Drop the vendor_entity table
DROP TABLE IF EXISTS vendor_entity;

-- ======================================================================
-- PART 6: Drop vendor table and related objects
-- ======================================================================

-- Drop indexes first
DROP INDEX IF EXISTS idx_vendor_pan_number;
DROP INDEX IF EXISTS idx_vendor_tenant_id;
DROP INDEX IF EXISTS idx_vendor_name;

-- Drop foreign key constraints
ALTER TABLE vendor 
DROP CONSTRAINT IF EXISTS fk_vendor_tenant;

-- Drop unique constraints
ALTER TABLE vendor 
DROP CONSTRAINT IF EXISTS unique_vendor_name_tenant_deleted;

-- Drop the vendor table
DROP TABLE IF EXISTS vendor;

-- ======================================================================
-- PART 7: Drop sequences
-- ======================================================================

-- Drop sequences
DROP SEQUENCE IF EXISTS vendor_receive_batch_sequence;
DROP SEQUENCE IF EXISTS vendor_dispatch_heat_sequence;
DROP SEQUENCE IF EXISTS processed_item_vendor_dispatch_batch_sequence;
DROP SEQUENCE IF EXISTS vendor_dispatch_batch_sequence;
DROP SEQUENCE IF EXISTS vendor_entity_sequence;
DROP SEQUENCE IF EXISTS vendor_sequence;

-- ======================================================================
-- PART 8: Validation and verification
-- ======================================================================

-- Verify that all tables were dropped successfully
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'vendor_dispatch_heat') THEN
        RAISE EXCEPTION 'Rollback failed: vendor_dispatch_heat table still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'vendor_receive_batch') THEN
        RAISE EXCEPTION 'Rollback failed: vendor_receive_batch table still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'processed_item_vendor_dispatch_batch') THEN
        RAISE EXCEPTION 'Rollback failed: processed_item_vendor_dispatch_batch table still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'vendor_dispatch_batch') THEN
        RAISE EXCEPTION 'Rollback failed: vendor_dispatch_batch table still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'vendor_dispatch_batch_processes') THEN
        RAISE EXCEPTION 'Rollback failed: vendor_dispatch_batch_processes table still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'vendor_entity') THEN
        RAISE EXCEPTION 'Rollback failed: vendor_entity table still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'vendor') THEN
        RAISE EXCEPTION 'Rollback failed: vendor table still exists';
    END IF;
END $$;

-- Verify that all sequences were dropped successfully
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.sequences WHERE sequence_name = 'vendor_receive_batch_sequence') THEN
        RAISE EXCEPTION 'Rollback failed: vendor_receive_batch_sequence still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.sequences WHERE sequence_name = 'vendor_dispatch_heat_sequence') THEN
        RAISE EXCEPTION 'Rollback failed: vendor_dispatch_heat_sequence still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.sequences WHERE sequence_name = 'processed_item_vendor_dispatch_batch_sequence') THEN
        RAISE EXCEPTION 'Rollback failed: processed_item_vendor_dispatch_batch_sequence still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.sequences WHERE sequence_name = 'vendor_dispatch_batch_sequence') THEN
        RAISE EXCEPTION 'Rollback failed: vendor_dispatch_batch_sequence still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.sequences WHERE sequence_name = 'vendor_entity_sequence') THEN
        RAISE EXCEPTION 'Rollback failed: vendor_entity_sequence still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.sequences WHERE sequence_name = 'vendor_sequence') THEN
        RAISE EXCEPTION 'Rollback failed: vendor_sequence still exists';
    END IF;
END $$;

-- ======================================================================
-- PART 9: Restore original workflow operation types
-- ======================================================================

-- Drop updated check constraints
ALTER TABLE workflow_step DROP CONSTRAINT IF EXISTS workflow_step_operation_type_check;
ALTER TABLE item_workflow_step DROP CONSTRAINT IF EXISTS item_workflow_step_operation_type_check;

COMMIT; 