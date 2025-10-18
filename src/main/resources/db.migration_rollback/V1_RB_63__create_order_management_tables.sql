-- Rollback Script: V1_RB_63__create_order_management_tables.sql
-- Description: Complete rollback for Order Management system including tenant settings
-- Author: System Generated
-- Date: 2024-10-04
-- Version: Consolidated rollback for development phase (includes V1_RB_63 and V1_RB_64)
-- WARNING: This script will permanently delete all order management data!

-- Begin transaction for rollback safety
BEGIN;

-- Log rollback initiation
DO $$ 
BEGIN
    RAISE NOTICE 'Starting Order Management system rollback...';
END $$;

-- Drop indexes first (order matters for dependencies)
DROP INDEX IF EXISTS idx_order_item_workflow_priority;
DROP INDEX IF EXISTS idx_order_item_workflow_item_workflow_id;
DROP INDEX IF EXISTS idx_order_item_workflow_order_item_id;

DROP INDEX IF EXISTS idx_order_item_item_id;
DROP INDEX IF EXISTS idx_order_item_order_id;

DROP INDEX IF EXISTS idx_orders_inventory_shortage;
DROP INDEX IF EXISTS idx_order_expected_completion;
DROP INDEX IF EXISTS idx_order_priority_tenant_id;
DROP INDEX IF EXISTS idx_order_buyer_id;
DROP INDEX IF EXISTS idx_order_date_tenant_id;
DROP INDEX IF EXISTS idx_order_status_tenant_id;
DROP INDEX IF EXISTS idx_order_tenant_id;
DROP INDEX IF EXISTS idx_order_po_number_tenant_id;

DROP INDEX IF EXISTS idx_tenant_order_settings_tenant;

-- Drop tables in reverse dependency order
DROP TABLE IF EXISTS order_item_workflows CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS tenant_order_settings CASCADE;

-- Drop sequences
DROP SEQUENCE IF EXISTS order_item_workflow_sequence;
DROP SEQUENCE IF EXISTS order_item_sequence;
DROP SEQUENCE IF EXISTS order_sequence;
DROP SEQUENCE IF EXISTS tenant_order_settings_sequence;

-- Verify cleanup and provide detailed feedback
DO $$ 
DECLARE 
    table_count INTEGER;
    sequence_count INTEGER;
    index_count INTEGER;
    constraint_count INTEGER;
BEGIN
    -- Check if tables still exist
    SELECT COUNT(*) INTO table_count 
    FROM information_schema.tables 
    WHERE table_name IN ('orders', 'order_items', 'order_item_workflows', 'tenant_order_settings') 
    AND table_schema = 'public';
    
    -- Check if sequences still exist
    SELECT COUNT(*) INTO sequence_count 
    FROM information_schema.sequences 
    WHERE sequence_name IN ('order_sequence', 'order_item_sequence', 'order_item_workflow_sequence', 'tenant_order_settings_sequence')
    AND sequence_schema = 'public';
    
    -- Check if indexes still exist
    SELECT COUNT(*) INTO index_count 
    FROM pg_indexes 
    WHERE (indexname LIKE 'idx_order%' OR indexname LIKE 'idx_tenant_order%')
    AND schemaname = 'public';
    
    -- Check if constraints still exist
    SELECT COUNT(*) INTO constraint_count
    FROM information_schema.table_constraints
    WHERE constraint_name LIKE '%order%'
    AND table_schema = 'public';
    
    -- Log detailed results
    RAISE NOTICE 'Rollback verification completed:';
    RAISE NOTICE '  - Tables remaining: %', table_count;
    RAISE NOTICE '  - Sequences remaining: %', sequence_count;
    RAISE NOTICE '  - Indexes remaining: %', index_count;
    RAISE NOTICE '  - Constraints remaining: %', constraint_count;
    
    -- Raise warning if cleanup incomplete
    IF table_count > 0 OR sequence_count > 0 OR index_count > 0 THEN
        RAISE WARNING 'Rollback may be incomplete. Manual cleanup may be required.';
        RAISE NOTICE 'Run the manual cleanup commands if needed (see script comments).';
    ELSE
        RAISE NOTICE 'Order Management system rollback completed successfully!';
        RAISE NOTICE 'All tables, sequences, indexes, and constraints have been removed.';
    END IF;
END $$;

-- Commit the rollback transaction
COMMIT;

-- Additional cleanup commands (run manually if needed)
-- These are commented out for safety but can be used for manual cleanup

/*
-- MANUAL CLEANUP COMMANDS (uncomment if automatic cleanup fails)

-- Force drop with CASCADE if there are dependency issues
DROP TABLE IF EXISTS order_item_workflows CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS tenant_order_settings CASCADE;

-- Clean up any remaining constraints
ALTER TABLE IF EXISTS item_workflow DROP CONSTRAINT IF EXISTS fk_item_workflow_order_item_workflows;

-- Clean up any remaining triggers (if any were created)
DROP TRIGGER IF EXISTS tr_orders_updated_at ON orders;
DROP TRIGGER IF EXISTS tr_order_items_updated_at ON order_items;
DROP TRIGGER IF EXISTS tr_order_item_workflows_updated_at ON order_item_workflows;
DROP TRIGGER IF EXISTS tr_tenant_order_settings_updated_at ON tenant_order_settings;

-- Clean up any remaining functions (if any were created)
DROP FUNCTION IF EXISTS update_order_timestamps();

-- Clean up any remaining views
DROP VIEW IF EXISTS v_order_summary;
DROP VIEW IF EXISTS v_order_workflow_status;

-- Force cleanup of sequences
DROP SEQUENCE IF EXISTS order_sequence CASCADE;
DROP SEQUENCE IF EXISTS order_item_sequence CASCADE;
DROP SEQUENCE IF EXISTS order_item_workflow_sequence CASCADE;
DROP SEQUENCE IF EXISTS tenant_order_settings_sequence CASCADE;
*/

-- ROLLBACK DOCUMENTATION
-- This consolidated rollback script removes:
-- 1. All order management tables (orders, order_items, order_item_workflows)
-- 2. Tenant order settings table (tenant_order_settings)
-- 3. All related indexes and constraints
-- 4. All sequences used for ID generation
-- 5. All data stored in these tables (IRREVERSIBLE)
-- 6. All comments and documentation
--
-- Features of this rollback:
-- - Comprehensive verification with detailed logging
-- - Safe transaction-based rollback
-- - Manual cleanup commands available if needed
-- - Complete removal of all Order Management system components
--
-- To restore the Order Management system after rollback:
<<<<<<<< HEAD:src/main/resources/db.migration_rollback/V1_RB_63__create_order_management_tables.sql
-- 1. Re-run V1_63__create_order_management_tables.sql
========
-- 1. Re-run V1_64__create_order_management_tables.sql
>>>>>>>> 2e903e8 (BillingSettings & initial Accounting support):src/main/resources/db.migration_rollback/V1_RB_64__create_order_management_tables.sql
-- 2. Restore data from backups if needed
-- 3. Verify all foreign key relationships are intact
-- 4. Test order creation and workflow functionality
--
-- Development Phase Benefits:
-- - Single consolidated rollback script
-- - No dependency on multiple migration versions
-- - Clean slate for development iterations
-- - Comprehensive cleanup verification
