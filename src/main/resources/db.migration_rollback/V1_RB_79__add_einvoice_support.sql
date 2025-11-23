-- Rollback Script for V1_79__add_einvoice_support.sql
-- This script reverses the E-Invoice support implementation

BEGIN;

-- ==========================================
-- Part 1: Remove E-Invoice fields from invoice table
-- ==========================================

-- Drop index for IRN lookup
DROP INDEX IF EXISTS idx_invoice_irn;

-- Remove E-Invoice fields
ALTER TABLE invoice DROP COLUMN IF EXISTS einvoice_details_json;
ALTER TABLE invoice DROP COLUMN IF EXISTS einvoice_alert_message;
ALTER TABLE invoice DROP COLUMN IF EXISTS einvoice_date;

-- ==========================================
-- Part 2: Drop tenant_einvoice_credentials table
-- ==========================================

-- Drop indexes
DROP INDEX IF EXISTS idx_tenant_einv_credentials_active;
DROP INDEX IF EXISTS idx_tenant_einv_credentials_gsp_config;
DROP INDEX IF EXISTS idx_tenant_einv_credentials_tenant;

-- Drop table
DROP TABLE IF EXISTS tenant_einvoice_credentials;

-- Drop sequence
DROP SEQUENCE IF EXISTS tenant_einvoice_credentials_sequence;

COMMIT;
