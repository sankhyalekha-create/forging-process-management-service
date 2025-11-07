-- Rollback Migration V1_RB_72: Complete Removal of Delivery Challan System
-- Description: Remove delivery_challan, challan_line_item, and challan_dispatch_batch tables
-- Date: November 4, 2025

-- =====================================================
-- 1. Remove Challan Fields from Dispatch Batch Table
-- =====================================================

-- Remove challan_number and challan_date_time columns from dispatch_batch table
ALTER TABLE dispatch_batch
DROP COLUMN IF EXISTS challan_number,
DROP COLUMN IF EXISTS challan_date_time;

-- Verify challan fields removed from dispatch_batch
DO $$
DECLARE
  challan_number_exists BOOLEAN;
  challan_date_time_exists BOOLEAN;
BEGIN
  -- Check if dispatch_batch.challan_number column still exists
  SELECT EXISTS (
    SELECT 1 
    FROM information_schema.columns 
    WHERE table_name = 'dispatch_batch' 
    AND column_name = 'challan_number'
  ) INTO challan_number_exists;
  
  -- Check if dispatch_batch.challan_date_time column still exists
  SELECT EXISTS (
    SELECT 1 
    FROM information_schema.columns 
    WHERE table_name = 'dispatch_batch' 
    AND column_name = 'challan_date_time'
  ) INTO challan_date_time_exists;
  
  -- Raise notice with verification results
  IF NOT challan_number_exists AND NOT challan_date_time_exists THEN
    RAISE NOTICE 'SUCCESS: Challan fields removed from dispatch_batch';
    RAISE NOTICE '  ✓ dispatch_batch.challan_number column removed';
    RAISE NOTICE '  ✓ dispatch_batch.challan_date_time column removed';
  ELSE
    RAISE EXCEPTION 'ERROR: Dispatch batch challan fields rollback failed! Still exists: challan_number=%, challan_date_time=%',
                    challan_number_exists, challan_date_time_exists;
  END IF;
END $$;

-- =====================================================
-- 2. Drop WorkType Index and Columns
-- =====================================================
DROP INDEX IF EXISTS idx_delivery_challan_work_type;

-- Drop work_type column from challan_line_item table
ALTER TABLE challan_line_item
DROP COLUMN IF EXISTS work_type;

-- Drop work_type column from delivery_challan table
ALTER TABLE delivery_challan
DROP COLUMN IF EXISTS work_type;

-- =====================================================
-- 3. Drop Unique Index
-- =====================================================
DROP INDEX IF EXISTS uk_challan_number_tenant;

-- =====================================================
-- 4. Drop Other Indexes (for clean rollback)
-- =====================================================

-- Drop delivery_challan indexes
DROP INDEX IF EXISTS idx_delivery_challan_tenant_status;
DROP INDEX IF EXISTS idx_delivery_challan_challan_date_time;
DROP INDEX IF EXISTS idx_delivery_challan_consignee_buyer;
DROP INDEX IF EXISTS idx_delivery_challan_consignee_vendor;
DROP INDEX IF EXISTS idx_delivery_challan_deleted;
DROP INDEX IF EXISTS idx_delivery_challan_vehicle;
DROP INDEX IF EXISTS idx_delivery_challan_transporter;
DROP INDEX IF EXISTS idx_delivery_challan_order;

-- Drop challan_line_item indexes
DROP INDEX IF EXISTS idx_challan_line_item_challan;
DROP INDEX IF EXISTS idx_challan_line_item_hsn;
DROP INDEX IF EXISTS idx_challan_line_item_tenant;
DROP INDEX IF EXISTS idx_challan_line_item_deleted;
DROP INDEX IF EXISTS idx_challan_line_item_workflow;
DROP INDEX IF EXISTS idx_challan_line_item_processed_item;

-- Drop challan_dispatch_batch indexes
DROP INDEX IF EXISTS idx_challan_dispatch_batch_challan;
DROP INDEX IF EXISTS idx_challan_dispatch_batch_dispatch;
DROP INDEX IF EXISTS idx_challan_dispatch_batch_tenant;
DROP INDEX IF EXISTS idx_challan_dispatch_batch_deleted;

-- =====================================================
-- 5. Drop Junction Table
-- =====================================================
DROP TABLE IF EXISTS challan_dispatch_batch CASCADE;

-- =====================================================
-- 6. Drop Challan Line Item Table
-- =====================================================
DROP TABLE IF EXISTS challan_line_item CASCADE;

-- =====================================================
-- 7. Drop Delivery Challan Table (Main Table)
-- =====================================================
DROP TABLE IF EXISTS delivery_challan CASCADE;

-- =====================================================
-- 8. Drop Sequences
-- =====================================================
DROP SEQUENCE IF EXISTS challan_dispatch_batch_sequence;
DROP SEQUENCE IF EXISTS challan_line_item_sequence;
DROP SEQUENCE IF EXISTS delivery_challan_sequence;

-- =====================================================
-- 9. Remove Display Fields from Delivery Challan (if any data exists)
-- =====================================================
-- Note: Since delivery_challan table will be dropped, this is optional
-- but included for completeness if someone wants to keep the table

-- DROP COLUMN statements (commented out as table will be dropped anyway)
-- ALTER TABLE delivery_challan DROP COLUMN IF EXISTS consignor_gstin;
-- ALTER TABLE delivery_challan DROP COLUMN IF EXISTS consignor_name;
-- ALTER TABLE delivery_challan DROP COLUMN IF EXISTS consignor_address;
-- ALTER TABLE delivery_challan DROP COLUMN IF EXISTS consignor_state_code;
-- ALTER TABLE delivery_challan DROP COLUMN IF EXISTS terms_and_conditions;
-- ALTER TABLE delivery_challan DROP COLUMN IF EXISTS bank_name;
-- ALTER TABLE delivery_challan DROP COLUMN IF EXISTS account_number;
-- ALTER TABLE delivery_challan DROP COLUMN IF EXISTS ifsc_code;
-- ALTER TABLE delivery_challan DROP COLUMN IF EXISTS amount_in_words;

-- =====================================================
-- 10. Remove Challan Prefix from Tenant Challan Settings
-- =====================================================
ALTER TABLE tenant_challan_settings
DROP COLUMN IF EXISTS challan_prefix;

-- =====================================================
-- 11. Verification
-- =====================================================

-- Verify WorkType columns and index dropped
DO $$
DECLARE
  delivery_challan_work_type_exists BOOLEAN;
  challan_line_item_work_type_exists BOOLEAN;
  delivery_challan_work_type_index_exists BOOLEAN;
BEGIN
  -- Check if delivery_challan.work_type column still exists
  SELECT EXISTS (
    SELECT 1 
    FROM information_schema.columns 
    WHERE table_name = 'delivery_challan' 
    AND column_name = 'work_type'
  ) INTO delivery_challan_work_type_exists;
  
  -- Check if challan_line_item.work_type column still exists
  SELECT EXISTS (
    SELECT 1 
    FROM information_schema.columns 
    WHERE table_name = 'challan_line_item' 
    AND column_name = 'work_type'
  ) INTO challan_line_item_work_type_exists;
  
  -- Check if index still exists
  SELECT EXISTS (
    SELECT 1 
    FROM pg_indexes 
    WHERE tablename = 'delivery_challan' 
    AND indexname = 'idx_delivery_challan_work_type'
  ) INTO delivery_challan_work_type_index_exists;
  
  -- Raise notice with verification results
  IF NOT delivery_challan_work_type_exists AND NOT challan_line_item_work_type_exists AND NOT delivery_challan_work_type_index_exists THEN
    RAISE NOTICE 'SUCCESS: WorkType columns and index dropped successfully';
    RAISE NOTICE '  ✓ delivery_challan.work_type column removed';
    RAISE NOTICE '  ✓ challan_line_item.work_type column removed';
    RAISE NOTICE '  ✓ idx_delivery_challan_work_type index dropped';
  ELSE
    RAISE EXCEPTION 'ERROR: WorkType rollback failed! Still exists: delivery_challan.work_type=%, challan_line_item.work_type=%, index=%',
                    delivery_challan_work_type_exists, challan_line_item_work_type_exists, delivery_challan_work_type_index_exists;
  END IF;
END $$;

-- Verify unique index dropped
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE indexname = 'uk_challan_number_tenant'
    ) THEN
        RAISE NOTICE 'SUCCESS: Unique index uk_challan_number_tenant dropped successfully';
    ELSE
        RAISE EXCEPTION 'ERROR: Index uk_challan_number_tenant still exists';
    END IF;
END $$;

-- Verify delivery_challan table dropped
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables 
                 WHERE table_name = 'delivery_challan') THEN
    RAISE NOTICE 'SUCCESS: Table delivery_challan dropped successfully';
  ELSE
    RAISE EXCEPTION 'ERROR: Table delivery_challan still exists after rollback';
  END IF;
END $$;

-- Verify challan_line_item table dropped
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables 
                 WHERE table_name = 'challan_line_item') THEN
    RAISE NOTICE 'SUCCESS: Table challan_line_item dropped successfully';
  ELSE
    RAISE EXCEPTION 'ERROR: Table challan_line_item still exists after rollback';
  END IF;
END $$;

-- Verify challan_dispatch_batch table dropped
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables 
                 WHERE table_name = 'challan_dispatch_batch') THEN
    RAISE NOTICE 'SUCCESS: Table challan_dispatch_batch dropped successfully';
  ELSE
    RAISE EXCEPTION 'ERROR: Table challan_dispatch_batch still exists after rollback';
  END IF;
END $$;

-- Verify sequences dropped
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.sequences 
                 WHERE sequence_name IN ('delivery_challan_sequence', 'challan_line_item_sequence', 'challan_dispatch_batch_sequence')) THEN
    RAISE NOTICE 'SUCCESS: All sequences dropped successfully';
  ELSE
    RAISE EXCEPTION 'ERROR: One or more sequences still exist';
  END IF;
END $$;

-- Verify challan_prefix column dropped
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                 WHERE table_name = 'tenant_challan_settings' 
                 AND column_name = 'challan_prefix') THEN
    RAISE NOTICE 'SUCCESS: Column challan_prefix dropped from tenant_challan_settings';
  ELSE
    RAISE EXCEPTION 'ERROR: Column challan_prefix still exists in tenant_challan_settings';
  END IF;
END $$;

-- Final rollback success message
DO $$
BEGIN
  RAISE NOTICE '========================================';
  RAISE NOTICE 'V1_RB_72 Rollback completed successfully!';
  RAISE NOTICE 'Removed: challan_number and challan_date_time from dispatch_batch';
  RAISE NOTICE 'Removed: delivery_challan, challan_line_item, challan_dispatch_batch';
  RAISE NOTICE 'Removed: challan_prefix from tenant_challan_settings';
  RAISE NOTICE 'Removed: All display fields (consignor, terms, bank details) with table drop';
  RAISE NOTICE 'Removed: Unique index uk_challan_number_tenant';
  RAISE NOTICE 'Removed: work_type columns and index from challan tables';
  RAISE NOTICE 'Challan Management System rolled back!';
  RAISE NOTICE '========================================';
END $$;
