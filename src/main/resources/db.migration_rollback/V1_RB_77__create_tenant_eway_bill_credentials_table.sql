-- Rollback: Drop E-Way Bill GSP integration tables
-- Version: V1_RB_77
-- Date: 2025-11-20
-- Description: Rollback script to remove gsp_configuration and tenant_eway_bill_credentials tables

-- ============================================================================
-- Part 1: Drop tenant_eway_bill_credentials table
-- ============================================================================

-- Drop indexes for tenant_eway_bill_credentials
DROP INDEX IF EXISTS idx_tenant_ewb_cred_token_expiry;
DROP INDEX IF EXISTS idx_tenant_ewb_cred_active;
DROP INDEX IF EXISTS idx_tenant_ewb_cred_gstin;
DROP INDEX IF EXISTS idx_tenant_ewb_cred_gsp_config;
DROP INDEX IF EXISTS idx_tenant_ewb_cred_tenant_id;

-- Drop the tenant_eway_bill_credentials table (CASCADE will remove foreign key references)
DROP TABLE IF EXISTS tenant_eway_bill_credentials CASCADE;

-- ============================================================================
-- Part 2: Drop gsp_configuration table
-- ============================================================================

-- Drop indexes for gsp_configuration
DROP INDEX IF EXISTS idx_gsp_config_production;
DROP INDEX IF EXISTS idx_gsp_config_active;
DROP INDEX IF EXISTS idx_gsp_config_provider;
DROP INDEX IF EXISTS idx_gsp_config_name;

-- Drop the gsp_configuration table
DROP TABLE IF EXISTS gsp_configuration CASCADE;

-- ============================================================================
-- Log rollback completion
-- ============================================================================

DO $$
BEGIN
    RAISE NOTICE 'Rollback V1_77: E-Way Bill GSP integration tables dropped successfully';
    RAISE NOTICE '  - tenant_eway_bill_credentials table dropped';
    RAISE NOTICE '  - gsp_configuration table dropped';
    RAISE NOTICE '  - All indexes and constraints removed';
END $$;
