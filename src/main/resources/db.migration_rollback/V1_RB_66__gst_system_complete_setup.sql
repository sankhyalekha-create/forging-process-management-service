-- Rollback Migration: Complete GST System Setup
-- Version: V1_RB_61
-- Description: Rollback all GST system changes including entities, enhanced buyer/vendor fields, 
--              and dispatch batch enhancements

BEGIN;

-- ======================================================================
-- PART 1: Drop GST Core Tables (in reverse dependency order)
-- ======================================================================

-- Drop GST configuration table
DROP TABLE IF EXISTS gst_configuration CASCADE;
DROP SEQUENCE IF EXISTS gst_configuration_sequence;

-- Drop E-Way Bill table
DROP TABLE IF EXISTS eway_bill CASCADE;
DROP SEQUENCE IF EXISTS eway_bill_sequence;

-- Drop Invoice table
DROP TABLE IF EXISTS invoice CASCADE;
DROP SEQUENCE IF EXISTS invoice_sequence;

-- Drop Delivery Challan table
DROP TABLE IF EXISTS delivery_challan CASCADE;
DROP SEQUENCE IF EXISTS delivery_challan_sequence;

-- ======================================================================
-- PART 2: Remove Dispatch Batch GST Enhancements
-- ======================================================================

-- Drop indexes first
DROP INDEX IF EXISTS idx_dispatch_batch_requires_eway_bill;
DROP INDEX IF EXISTS idx_dispatch_batch_hsn_code;
DROP INDEX IF EXISTS idx_dispatch_batch_gstin;

-- Drop constraints
ALTER TABLE dispatch_batch DROP CONSTRAINT IF EXISTS chk_dispatch_transportation_distance;
ALTER TABLE dispatch_batch DROP CONSTRAINT IF EXISTS chk_dispatch_transportation_mode;

-- Remove GST-related columns from dispatch_batch
ALTER TABLE dispatch_batch 
DROP COLUMN IF EXISTS eway_bill_threshold_met,
DROP COLUMN IF EXISTS requires_eway_bill,
DROP COLUMN IF EXISTS transportation_distance,
DROP COLUMN IF EXISTS transportation_mode,
DROP COLUMN IF EXISTS total_amount,
DROP COLUMN IF EXISTS total_tax_amount,
DROP COLUMN IF EXISTS igst_amount,
DROP COLUMN IF EXISTS sgst_amount,
DROP COLUMN IF EXISTS cgst_amount,
DROP COLUMN IF EXISTS taxable_value,
DROP COLUMN IF EXISTS hsn_code,
DROP COLUMN IF EXISTS gstin;

-- ======================================================================
-- PART 3: Remove Buyer/Vendor Validation Constraints and Indexes
-- ======================================================================
-- Note: Buyer/Vendor state_code and pincode fields are from V1_61, not V1_65
--       Only removing constraints and indexes added by V1_65

-- Drop indexes
DROP INDEX IF EXISTS idx_vendor_entity_state_code;
DROP INDEX IF EXISTS idx_vendor_state_code;
DROP INDEX IF EXISTS idx_buyer_entity_state_code;
DROP INDEX IF EXISTS idx_buyer_state_code;

-- Drop constraints
ALTER TABLE vendor_entity DROP CONSTRAINT IF EXISTS chk_vendor_entity_state_code_format;
ALTER TABLE vendor_entity DROP CONSTRAINT IF EXISTS chk_vendor_entity_pincode_format;
ALTER TABLE vendor DROP CONSTRAINT IF EXISTS chk_vendor_state_code_format;
ALTER TABLE vendor DROP CONSTRAINT IF EXISTS chk_vendor_pincode_format;
ALTER TABLE buyer_entity DROP CONSTRAINT IF EXISTS chk_buyer_entity_state_code_format;
ALTER TABLE buyer_entity DROP CONSTRAINT IF EXISTS chk_buyer_entity_pincode_format;
ALTER TABLE buyer DROP CONSTRAINT IF EXISTS chk_buyer_state_code_format;
ALTER TABLE buyer DROP CONSTRAINT IF EXISTS chk_buyer_pincode_format;

-- Note: NOT dropping state_code and pincode columns as they were created in V1_61

COMMIT;

-- ======================================================================
-- ROLLBACK SUMMARY
-- ======================================================================
-- Complete GST System Rollback includes:
-- 
-- 1. Removed GST Core Entities:
--    - gst_configuration table and sequence
--    - eway_bill table and sequence
--    - invoice table and sequence
--    - delivery_challan table and sequence
--
-- 2. Reverted Buyer/Vendor Enhancements:
--    - Removed validation constraints added by V1_65
--    - Removed indexes added by V1_65
--    - Note: state_code and pincode columns remain (created in V1_61)
--
-- 3. Reverted Dispatch Batch:
--    - Removed all GST-related fields
--    - Removed transportation and E-Way Bill fields
--    - Removed related constraints and indexes
--
-- 4. Cleanup:
--    - All foreign key relationships properly handled
--    - All indexes and constraints from V1_65 removed
--    - All sequences dropped
--
-- System reverted to pre-V1_65 state with buyer/vendor fields from V1_61 intact.
-- ======================================================================
