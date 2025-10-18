-- Migration: Add Tenant Billing Settings Tables with Complete Tax Support
-- Version: V1_61 (Merged from V1_61 + V1_62 + Job Work Tax Fields)
-- Description: Create tenant_invoice_settings and tenant_challan_settings tables
--              with comprehensive tax support for both Job Work and Material invoices

BEGIN;

-- ======================================================================
-- PART 1: Create tenant_invoice_settings table
-- ======================================================================

CREATE TABLE tenant_invoice_settings (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    
    -- Job Work Invoice Settings
    job_work_invoice_prefix VARCHAR(10) DEFAULT 'JW',
    job_work_current_sequence INTEGER DEFAULT 1,
    job_work_series_format VARCHAR(20) DEFAULT '{25-26}',
    job_work_start_from INTEGER DEFAULT 1,
    
    -- Job Work Tax Settings (NEW)
    job_work_hsn_sac_code VARCHAR(10),
    job_work_cgst_rate DECIMAL(5,2) DEFAULT 9.00,
    job_work_sgst_rate DECIMAL(5,2) DEFAULT 9.00,
    job_work_igst_rate DECIMAL(5,2) DEFAULT 18.00,
    
    -- Material Invoice Settings
    material_invoice_prefix VARCHAR(10),
    material_current_sequence INTEGER DEFAULT 1,
    material_series_format VARCHAR(20) DEFAULT '{25-26}',
    material_start_from INTEGER DEFAULT 1,
    
    -- Material Tax Settings
    material_hsn_sac_code VARCHAR(10),
    material_cgst_rate DECIMAL(5,2) DEFAULT 9.00,
    material_sgst_rate DECIMAL(5,2) DEFAULT 9.00,
    material_igst_rate DECIMAL(5,2) DEFAULT 18.00,
    
    -- Manual Invoice Settings
    manual_invoice_enabled BOOLEAN DEFAULT FALSE,
    manual_invoice_title VARCHAR(100) DEFAULT 'GST INVOICE',
    manual_start_from INTEGER DEFAULT 1,
    
    -- Integration Settings
    show_vehicle_number BOOLEAN DEFAULT TRUE,
    
    -- E-Way Bill & E-Invoice Settings
    activate_eway_bill BOOLEAN DEFAULT FALSE,
    activate_einvoice BOOLEAN DEFAULT FALSE,
    activate_tcs BOOLEAN DEFAULT FALSE,
    
    -- Transport & Bank Details
    transporter_details BOOLEAN DEFAULT TRUE,
    bank_details_same_as_jobwork BOOLEAN DEFAULT TRUE,
    bank_name VARCHAR(100),
    account_number VARCHAR(20),
    ifsc_code VARCHAR(11),
    
    -- Terms and Conditions
    terms_and_conditions TEXT,
    
    -- Status and Audit
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- ======================================================================
-- PART 2: Create tenant_challan_settings table
-- ======================================================================

CREATE TABLE tenant_challan_settings (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    
    -- Challan Number Configuration
    start_from INTEGER DEFAULT 1,
    current_sequence INTEGER DEFAULT 1,
    series_format VARCHAR(20) DEFAULT '2025-26',
    
    -- Tax Configuration
    hsn_sac_code VARCHAR(10),
    cgst_rate DECIMAL(5,2) DEFAULT 9.00,
    sgst_rate DECIMAL(5,2) DEFAULT 9.00,
    igst_rate DECIMAL(5,2) DEFAULT 18.00,
    activate_tcs BOOLEAN DEFAULT FALSE,
    
    -- Bank Details
    bank_details_same_as_jobwork BOOLEAN DEFAULT TRUE,
    bank_name VARCHAR(100),
    account_number VARCHAR(20),
    ifsc_code VARCHAR(11),
    
    -- Terms and Conditions
    terms_and_conditions TEXT,
    
    -- Status and Audit
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- ======================================================================
-- PART 3: Add Constraints
-- ======================================================================

-- Invoice Settings Constraints
ALTER TABLE tenant_invoice_settings 
ADD CONSTRAINT chk_job_work_sequence CHECK (job_work_current_sequence >= 1 AND job_work_current_sequence <= 999999),
ADD CONSTRAINT chk_material_sequence CHECK (material_current_sequence >= 1 AND material_current_sequence <= 999999),
ADD CONSTRAINT chk_job_work_start_from CHECK (job_work_start_from >= 1),
ADD CONSTRAINT chk_material_start_from CHECK (material_start_from >= 1),
ADD CONSTRAINT chk_manual_start_from CHECK (manual_start_from >= 1),
ADD CONSTRAINT chk_ifsc_format CHECK (ifsc_code IS NULL OR ifsc_code ~ '^[A-Z]{4}0[A-Z0-9]{6}$'),

-- Job Work Tax Constraints (NEW)
ADD CONSTRAINT chk_job_work_hsn_sac_format CHECK (job_work_hsn_sac_code IS NULL OR job_work_hsn_sac_code ~ '^[0-9]{6,8}$'),
ADD CONSTRAINT chk_job_work_cgst_rate CHECK (job_work_cgst_rate >= 0.00 AND job_work_cgst_rate <= 50.00),
ADD CONSTRAINT chk_job_work_sgst_rate CHECK (job_work_sgst_rate >= 0.00 AND job_work_sgst_rate <= 50.00),
ADD CONSTRAINT chk_job_work_igst_rate CHECK (job_work_igst_rate >= 0.00 AND job_work_igst_rate <= 50.00),

-- Material Tax Constraints
ADD CONSTRAINT chk_material_hsn_sac_format CHECK (material_hsn_sac_code IS NULL OR material_hsn_sac_code ~ '^[0-9]{6,8}$'),
ADD CONSTRAINT chk_material_cgst_rate CHECK (material_cgst_rate >= 0.00 AND material_cgst_rate <= 50.00),
ADD CONSTRAINT chk_material_sgst_rate CHECK (material_sgst_rate >= 0.00 AND material_sgst_rate <= 50.00),
ADD CONSTRAINT chk_material_igst_rate CHECK (material_igst_rate >= 0.00 AND material_igst_rate <= 50.00),

-- Only one active setting per tenant
ADD CONSTRAINT unique_active_invoice_settings 
    EXCLUDE (tenant_id WITH =) WHERE (is_active = true AND deleted = false);

-- Challan Settings Constraints
ALTER TABLE tenant_challan_settings 
ADD CONSTRAINT chk_start_from CHECK (start_from >= 1),
ADD CONSTRAINT chk_current_sequence CHECK (current_sequence >= 1 AND current_sequence <= 999999),
ADD CONSTRAINT chk_hsn_sac_format CHECK (hsn_sac_code ~ '^[0-9]{6,8}$'),
ADD CONSTRAINT chk_cgst_rate CHECK (cgst_rate >= 0.00 AND cgst_rate <= 50.00),
ADD CONSTRAINT chk_sgst_rate CHECK (sgst_rate >= 0.00 AND sgst_rate <= 50.00),
ADD CONSTRAINT chk_igst_rate CHECK (igst_rate >= 0.00 AND igst_rate <= 50.00),
ADD CONSTRAINT chk_ifsc_format_challan CHECK (ifsc_code IS NULL OR ifsc_code ~ '^[A-Z]{4}0[A-Z0-9]{6}$'),

-- Only one active setting per tenant
ADD CONSTRAINT unique_active_challan_settings 
    EXCLUDE (tenant_id WITH =) WHERE (is_active = true AND deleted = false);

-- ======================================================================
-- PART 4: Create Indexes
-- ======================================================================

-- Performance indexes for invoice settings
CREATE INDEX idx_invoice_settings_tenant ON tenant_invoice_settings(tenant_id);
CREATE INDEX idx_invoice_settings_active ON tenant_invoice_settings(tenant_id, is_active, deleted);

-- Performance indexes for challan settings
CREATE INDEX idx_challan_settings_tenant ON tenant_challan_settings(tenant_id);
CREATE INDEX idx_challan_settings_active ON tenant_challan_settings(tenant_id, is_active, deleted);

-- ======================================================================
-- PART 5: Create Sequences
-- ======================================================================

-- Create sequences for ID generation
CREATE SEQUENCE IF NOT EXISTS invoice_settings_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS challan_settings_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- ======================================================================
-- PART 6: Add Comments for Documentation
-- ======================================================================

-- Invoice Settings Comments
COMMENT ON TABLE tenant_invoice_settings IS 'Tenant-specific invoice configuration settings';
COMMENT ON COLUMN tenant_invoice_settings.job_work_hsn_sac_code IS 'HSN/SAC code for job work invoices (6-8 digits)';
COMMENT ON COLUMN tenant_invoice_settings.job_work_cgst_rate IS 'CGST rate for job work invoices (0-50%)';
COMMENT ON COLUMN tenant_invoice_settings.job_work_sgst_rate IS 'SGST rate for job work invoices (0-50%)';
COMMENT ON COLUMN tenant_invoice_settings.job_work_igst_rate IS 'IGST rate for job work invoices (0-50%)';
COMMENT ON COLUMN tenant_invoice_settings.material_hsn_sac_code IS 'HSN/SAC code for material invoices (6-8 digits)';
COMMENT ON COLUMN tenant_invoice_settings.material_cgst_rate IS 'CGST rate for material invoices (0-50%)';
COMMENT ON COLUMN tenant_invoice_settings.material_sgst_rate IS 'SGST rate for material invoices (0-50%)';
COMMENT ON COLUMN tenant_invoice_settings.material_igst_rate IS 'IGST rate for material invoices (0-50%)';

-- Challan Settings Comments
COMMENT ON TABLE tenant_challan_settings IS 'Tenant-specific challan configuration settings';
COMMENT ON COLUMN tenant_challan_settings.hsn_sac_code IS 'HSN/SAC code for challans (6-8 digits)';
COMMENT ON COLUMN tenant_challan_settings.cgst_rate IS 'CGST rate for challans (0-50%)';
COMMENT ON COLUMN tenant_challan_settings.sgst_rate IS 'SGST rate for challans (0-50%)';
COMMENT ON COLUMN tenant_challan_settings.igst_rate IS 'IGST rate for challans (0-50%)';

COMMIT;

-- ======================================================================
-- MIGRATION SUMMARY
-- ======================================================================
-- Created comprehensive tenant billing settings infrastructure:
-- 
-- 1. Tables Created:
--    - tenant_invoice_settings (with job work and material tax support)
--    - tenant_challan_settings (with complete tax configuration)
--
-- 2. New Job Work Tax Fields:
--    - job_work_hsn_sac_code: HSN/SAC code for job work invoices
--    - job_work_cgst_rate: CGST rate (default 9.00%)
--    - job_work_sgst_rate: SGST rate (default 9.00%)
--    - job_work_igst_rate: IGST rate (default 18.00%)
--
-- 3. Material Tax Fields:
--    - material_hsn_sac_code: HSN/SAC code for material invoices
--    - material_cgst_rate: CGST rate (default 9.00%)
--    - material_sgst_rate: SGST rate (default 9.00%)
--    - material_igst_rate: IGST rate (default 18.00%)
--
-- 4. Validation Constraints:
--    - HSN/SAC code format validation (6-8 digits)
--    - Tax rate range validation (0-50%)
--    - Sequence number validation
--    - IFSC code format validation
--    - Unique active settings per tenant
--
-- 5. Performance Optimizations:
--    - Composite indexes for efficient queries
--    - Sequences for ID generation
--    - Proper foreign key relationships
--
-- 6. Benefits:
--    - Complete tax settings management for both invoice types
--    - Consistent data validation across all tax fields
--    - Optimized for high-performance tax calculations
--    - Tenant isolation with proper constraints
-- ======================================================================
