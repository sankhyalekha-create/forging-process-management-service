-- Rollback Migration: Remove Tenant Billing Settings Tables with Tax Support
-- Description: Complete rollback script for V1_67__add_tenant_billing_settings_with_tax_support.sql
--              Removes all tenant billing settings infrastructure including tax support

BEGIN;

-- ======================================================================
-- PART 1: Drop Indexes
-- ======================================================================

-- Drop performance indexes for challan settings
DROP INDEX IF EXISTS idx_challan_settings_active;
DROP INDEX IF EXISTS idx_challan_settings_tenant;

-- Drop performance indexes for invoice settings
DROP INDEX IF EXISTS idx_invoice_settings_active;
DROP INDEX IF EXISTS idx_invoice_settings_tenant;

-- ======================================================================
-- PART 2: Drop Tables (in reverse dependency order)
-- ======================================================================

-- Drop tenant challan settings table (with all constraints and data)
DROP TABLE IF EXISTS tenant_challan_settings CASCADE;

-- Drop tenant invoice settings table (with all constraints and data)
-- This includes all job work and material tax fields
DROP TABLE IF EXISTS tenant_invoice_settings CASCADE;

-- ======================================================================
-- PART 3: Drop Sequences
-- ======================================================================

-- Drop sequences for ID generation
DROP SEQUENCE IF EXISTS challan_settings_sequence;
DROP SEQUENCE IF EXISTS invoice_settings_sequence;

COMMIT;

-- ======================================================================
-- ROLLBACK SUMMARY
-- ======================================================================
-- Completely removed all tenant billing settings infrastructure:
-- 
-- 1. Dropped Tables:
--    - tenant_challan_settings (with all tax and configuration settings)
--    - tenant_invoice_settings (with job work and material tax support)
--
-- 2. Removed Job Work Tax Fields:
--    - job_work_hsn_sac_code
--    - job_work_cgst_rate
--    - job_work_sgst_rate
--    - job_work_igst_rate
--    - job_work_terms_and_conditions
--
-- 3. Removed Material Tax Fields:
--    - material_hsn_sac_code
--    - material_cgst_rate
--    - material_sgst_rate
--    - material_igst_rate
--    - material_terms_and_conditions
--
-- 4. Dropped All Constraints:
--    - chk_job_work_hsn_sac_format
--    - chk_job_work_cgst_rate, chk_job_work_sgst_rate, chk_job_work_igst_rate
--    - chk_material_hsn_sac_format
--    - chk_material_cgst_rate, chk_material_sgst_rate, chk_material_igst_rate
--    - chk_hsn_sac_format (challan)
--    - chk_cgst_rate, chk_sgst_rate, chk_igst_rate (challan)
--    - unique_active_invoice_settings
--    - unique_active_challan_settings
--    - All sequence and format validation constraints
--
-- 5. Dropped Indexes:
--    - idx_challan_settings_active
--    - idx_challan_settings_tenant
--    - idx_invoice_settings_active
--    - idx_invoice_settings_tenant
--
-- 6. Dropped Sequences:
--    - challan_settings_sequence
--    - invoice_settings_sequence
--
-- System completely reverted to state before tenant billing settings feature.
-- All tenant-specific invoice and challan configurations removed.
-- All tax calculation support for job work and material invoices removed.
-- ======================================================================
