-- =====================================================
-- Rollback: Complete Removal of Delivery Challan System
-- Consolidated Rollback: V1_RB_72, V1_RB_73, V1_RB_74
-- Description: Removes all delivery challan tables, vendor challan support,
--              and restores tables to pre-challan state
-- Date: 2025 (Development Phase - Consolidated)
-- =====================================================

-- =====================================================
-- 1. Remove Challan Fields from Dispatch Batch Table
-- =====================================================

ALTER TABLE dispatch_batch
DROP COLUMN IF EXISTS challan_number,
DROP COLUMN IF EXISTS challan_date_time;

-- Verify challan fields removed from dispatch_batch
DO $$
DECLARE
  challan_number_exists BOOLEAN;
  challan_date_time_exists BOOLEAN;
BEGIN
  SELECT EXISTS (
    SELECT 1 FROM information_schema.columns 
    WHERE table_name = 'dispatch_batch' AND column_name = 'challan_number'
  ) INTO challan_number_exists;
  
  SELECT EXISTS (
    SELECT 1 FROM information_schema.columns 
    WHERE table_name = 'dispatch_batch' AND column_name = 'challan_date_time'
  ) INTO challan_date_time_exists;
  
  IF NOT challan_number_exists AND NOT challan_date_time_exists THEN
    RAISE NOTICE '✓ Challan fields removed from dispatch_batch';
  ELSE
    RAISE EXCEPTION 'ERROR: Dispatch batch challan fields rollback failed! Still exists: challan_number=%, challan_date_time=%',
                    challan_number_exists, challan_date_time_exists;
  END IF;
END $$;

-- =====================================================
-- 2. Remove Challan Fields from Vendor Dispatch Batch Table
-- =====================================================

-- Drop foreign key constraint
ALTER TABLE vendor_dispatch_batch 
DROP CONSTRAINT IF EXISTS fk_vendor_dispatch_batch_delivery_challan;

-- Drop indexes
DROP INDEX IF EXISTS idx_vendor_dispatch_batch_delivery_challan;
DROP INDEX IF EXISTS idx_vendor_dispatch_batch_challan_number;

-- Drop columns
ALTER TABLE vendor_dispatch_batch 
DROP COLUMN IF EXISTS delivery_challan_id,
DROP COLUMN IF EXISTS challan_date_time,
DROP COLUMN IF EXISTS challan_number;

-- =====================================================
-- 3. Drop Tenant Vendor Challan Settings Table
-- =====================================================

DROP INDEX IF EXISTS idx_vendor_challan_settings_tenant;
DROP INDEX IF EXISTS idx_vendor_challan_settings_active;
DROP TABLE IF EXISTS tenant_vendor_challan_settings CASCADE;
DROP SEQUENCE IF EXISTS vendor_challan_settings_sequence;

-- =====================================================
-- 4. Drop Challan Vendor Line Item Table
-- =====================================================

DROP INDEX IF EXISTS idx_challan_vendor_line_item_challan;
DROP INDEX IF EXISTS idx_challan_vendor_line_item_vendor_dispatch_batch;
DROP INDEX IF EXISTS idx_challan_vendor_line_item_hsn;
DROP INDEX IF EXISTS idx_challan_vendor_line_item_tenant;
DROP INDEX IF EXISTS idx_challan_vendor_line_item_deleted;
DROP INDEX IF EXISTS idx_challan_vendor_line_item_workflow;
DROP INDEX IF EXISTS idx_challan_vendor_line_processed_item;
DROP TABLE IF EXISTS challan_vendor_line_item CASCADE;
DROP SEQUENCE IF EXISTS challan_vendor_line_item_sequence;

-- =====================================================
-- 5. Drop Challan Vendor Dispatch Batch Junction Table
-- =====================================================

DROP INDEX IF EXISTS idx_challan_vendor_dispatch_batch_challan;
DROP INDEX IF EXISTS idx_challan_vendor_dispatch_batch_vendor_dispatch;
DROP INDEX IF EXISTS idx_challan_vendor_dispatch_batch_tenant;
DROP INDEX IF EXISTS idx_challan_vendor_dispatch_batch_deleted;
DROP TABLE IF EXISTS challan_vendor_dispatch_batch CASCADE;
DROP SEQUENCE IF EXISTS challan_vendor_dispatch_batch_sequence;

-- =====================================================
-- 6. Drop Delivery Challan Indexes
-- =====================================================

DROP INDEX IF EXISTS uk_challan_number_tenant;
DROP INDEX IF EXISTS idx_delivery_challan_tenant_status;
DROP INDEX IF EXISTS idx_delivery_challan_challan_date_time;
DROP INDEX IF EXISTS idx_delivery_challan_buyer;
DROP INDEX IF EXISTS idx_delivery_challan_vendor;
DROP INDEX IF EXISTS idx_delivery_challan_billing_buyer_entity;
DROP INDEX IF EXISTS idx_delivery_challan_shipping_buyer_entity;
DROP INDEX IF EXISTS idx_delivery_challan_billing_vendor_entity;
DROP INDEX IF EXISTS idx_delivery_challan_shipping_vendor_entity;
DROP INDEX IF EXISTS idx_delivery_challan_deleted;
DROP INDEX IF EXISTS idx_delivery_challan_vehicle;
DROP INDEX IF EXISTS idx_delivery_challan_transporter_legacy;
DROP INDEX IF EXISTS idx_delivery_challan_order;
DROP INDEX IF EXISTS idx_delivery_challan_work_type;
DROP INDEX IF EXISTS idx_delivery_challan_is_vendor_challan;
DROP INDEX IF EXISTS idx_delivery_challan_tenant_is_vendor;
DROP INDEX IF EXISTS idx_delivery_challan_estimated_value;
DROP INDEX IF EXISTS idx_delivery_challan_total_quantity;
DROP INDEX IF EXISTS idx_delivery_challan_cancelled_at;
DROP INDEX IF EXISTS idx_delivery_challan_cancellation;
DROP INDEX IF EXISTS idx_delivery_challan_converted_to_invoice;
DROP INDEX IF EXISTS idx_delivery_challan_expected_delivery;
DROP INDEX IF EXISTS idx_delivery_challan_actual_delivery;
DROP INDEX IF EXISTS idx_delivery_challan_transportation_mode;
DROP INDEX IF EXISTS idx_delivery_challan_document_path;

-- =====================================================
-- 7. Drop Challan Line Item Table
-- =====================================================

DROP INDEX IF EXISTS idx_challan_line_item_challan;
DROP INDEX IF EXISTS idx_challan_line_item_hsn;
DROP INDEX IF EXISTS idx_challan_line_item_tenant;
DROP INDEX IF EXISTS idx_challan_line_item_deleted;
DROP INDEX IF EXISTS idx_challan_line_item_workflow;
DROP INDEX IF EXISTS idx_challan_line_item_processed_item;
DROP TABLE IF EXISTS challan_line_item CASCADE;
DROP SEQUENCE IF EXISTS challan_line_item_sequence;

-- =====================================================
-- 8. Drop Challan Dispatch Batch Junction Table
-- =====================================================

DROP INDEX IF EXISTS idx_challan_dispatch_batch_challan;
DROP INDEX IF EXISTS idx_challan_dispatch_batch_dispatch;
DROP INDEX IF EXISTS idx_challan_dispatch_batch_tenant;
DROP INDEX IF EXISTS idx_challan_dispatch_batch_deleted;
DROP TABLE IF EXISTS challan_dispatch_batch CASCADE;
DROP SEQUENCE IF EXISTS challan_dispatch_batch_sequence;

-- =====================================================
-- 9. Drop Delivery Challan Table
-- =====================================================

DROP TABLE IF EXISTS delivery_challan CASCADE;
DROP SEQUENCE IF EXISTS delivery_challan_sequence;

-- =====================================================
-- 10. Remove Challan Prefix from Tenant Challan Settings
-- =====================================================

ALTER TABLE tenant_challan_settings
DROP COLUMN IF EXISTS challan_prefix;

-- =====================================================
-- 11. Verification
-- =====================================================

-- Verify delivery_challan table removed
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'delivery_challan') THEN
    RAISE NOTICE '✓ Table delivery_challan removed successfully';
  ELSE
    RAISE EXCEPTION 'ERROR: Table delivery_challan still exists';
  END IF;
END $$;

-- Verify all junction tables removed
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables 
                 WHERE table_name IN ('challan_dispatch_batch', 'challan_vendor_dispatch_batch')) THEN
    RAISE NOTICE '✓ All junction tables removed successfully';
  ELSE
    RAISE EXCEPTION 'ERROR: Some junction tables still exist';
  END IF;
END $$;

-- Verify all line item tables removed
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables 
                 WHERE table_name IN ('challan_line_item', 'challan_vendor_line_item')) THEN
    RAISE NOTICE '✓ All line item tables removed successfully';
  ELSE
    RAISE EXCEPTION 'ERROR: Some line item tables still exist';
  END IF;
END $$;

-- Verify vendor challan settings removed
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'tenant_vendor_challan_settings') THEN
    RAISE NOTICE '✓ Tenant vendor challan settings removed successfully';
  ELSE
    RAISE EXCEPTION 'ERROR: tenant_vendor_challan_settings still exists';
  END IF;
END $$;

-- Verify all sequences removed
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.sequences 
                 WHERE sequence_name IN ('delivery_challan_sequence', 'challan_line_item_sequence',
                                         'challan_dispatch_batch_sequence', 'challan_vendor_dispatch_batch_sequence',
                                         'challan_vendor_line_item_sequence', 'vendor_challan_settings_sequence')) THEN
    RAISE NOTICE '✓ All sequences removed successfully';
  ELSE
    RAISE EXCEPTION 'ERROR: Some sequences still exist';
  END IF;
END $$;

-- Verify dispatch_batch columns removed
DO $$
DECLARE
  challan_number_exists BOOLEAN;
  challan_date_time_exists BOOLEAN;
BEGIN
  SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'dispatch_batch' AND column_name = 'challan_number') INTO challan_number_exists;
  SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'dispatch_batch' AND column_name = 'challan_date_time') INTO challan_date_time_exists;
  
  IF NOT challan_number_exists AND NOT challan_date_time_exists THEN
    RAISE NOTICE '✓ dispatch_batch challan columns removed';
  ELSE
    RAISE EXCEPTION 'ERROR: Some dispatch_batch challan columns still exist';
  END IF;
END $$;

-- Verify vendor_dispatch_batch columns removed
DO $$
DECLARE
  challan_number_exists BOOLEAN;
  challan_date_time_exists BOOLEAN;
  delivery_challan_id_exists BOOLEAN;
BEGIN
  SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'vendor_dispatch_batch' AND column_name = 'challan_number') INTO challan_number_exists;
  SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'vendor_dispatch_batch' AND column_name = 'challan_date_time') INTO challan_date_time_exists;
  SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'vendor_dispatch_batch' AND column_name = 'delivery_challan_id') INTO delivery_challan_id_exists;
  
  IF NOT challan_number_exists AND NOT challan_date_time_exists AND NOT delivery_challan_id_exists THEN
    RAISE NOTICE '✓ vendor_dispatch_batch challan columns removed';
  ELSE
    RAISE EXCEPTION 'ERROR: Some vendor_dispatch_batch challan columns still exist';
  END IF;
END $$;

-- =====================================================
-- 12. Final Success Message
-- =====================================================

DO $$
BEGIN
  RAISE NOTICE '========================================';
  RAISE NOTICE 'V1_RB_72 CONSOLIDATED Rollback completed successfully!';
  RAISE NOTICE 'Removed: delivery_challan table';
  RAISE NOTICE 'Removed: challan_line_item table';
  RAISE NOTICE 'Removed: challan_vendor_line_item table';
  RAISE NOTICE 'Removed: challan_dispatch_batch junction table';
  RAISE NOTICE 'Removed: challan_vendor_dispatch_batch junction table';
  RAISE NOTICE 'Removed: tenant_vendor_challan_settings table';
  RAISE NOTICE 'Removed: challan fields from dispatch_batch';
  RAISE NOTICE 'Removed: challan fields from vendor_dispatch_batch';
  RAISE NOTICE 'Removed: all sequences and indexes';
  RAISE NOTICE 'Database restored to pre-challan state';
  RAISE NOTICE '========================================';
END $$;
