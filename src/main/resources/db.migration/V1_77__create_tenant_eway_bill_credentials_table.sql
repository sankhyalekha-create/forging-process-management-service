-- Migration: Create E-Way Bill GSP integration tables
-- Version: V1_77
-- Date: 2025-11-20
-- Description: Creates gsp_configuration (shared) and tenant_eway_bill_credentials (tenant-specific) tables

-- ============================================================================
-- Part 1: Create gsp_configuration table (Shared GSP settings)
-- ============================================================================

CREATE TABLE IF NOT EXISTS gsp_configuration (
    id BIGSERIAL PRIMARY KEY,
    
    -- Configuration Identity
    config_name VARCHAR(100) NOT NULL UNIQUE,
    gsp_provider VARCHAR(100) NOT NULL,
    integration_mode VARCHAR(20) NOT NULL DEFAULT 'GSP_API' CHECK (integration_mode IN ('OFFLINE', 'GSP_API', 'HYBRID')),
    
    -- Shared ASP Credentials (encrypted)
    asp_user_id VARCHAR(100) NOT NULL,
    asp_password VARCHAR(500) NOT NULL,
    
    -- Configuration Settings
    is_production BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Documentation
    description VARCHAR(500),
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for gsp_configuration
CREATE INDEX idx_gsp_config_name ON gsp_configuration(config_name);
CREATE INDEX idx_gsp_config_provider ON gsp_configuration(gsp_provider);
CREATE INDEX idx_gsp_config_active ON gsp_configuration(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_gsp_config_production ON gsp_configuration(is_production);

-- Insert default configurations
INSERT INTO gsp_configuration (
    config_name, 
    gsp_provider, 
    integration_mode,
    asp_user_id,
    asp_password,
    is_production,
    is_active,
    description
) VALUES 
(
    'TaxPro Sandbox',
    'TaxPro GSP',
    'GSP_API',
    '1790870825',
    'ENCRYPTED_PASSWORD_PLACEHOLDER',  -- Replace with encrypted password using EncryptionUtil
    FALSE,
    TRUE,
    'TaxPro GSP Sandbox environment for testing'
),
(
    'TaxPro Production',
    'TaxPro GSP',
    'GSP_API',
    'YOUR_PRODUCTION_ASP_ID',
    'ENCRYPTED_PRODUCTION_PASSWORD_PLACEHOLDER',
    TRUE,
    FALSE,  -- Inactive until proper credentials are configured
    'TaxPro GSP Production environment - update credentials before activating'
);

-- ============================================================================
-- Part 2: Create tenant_eway_bill_credentials table (Tenant-specific)
-- ============================================================================

CREATE TABLE IF NOT EXISTS tenant_eway_bill_credentials (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    
    -- Reference to shared GSP configuration
    gsp_config_id BIGINT NOT NULL,
    
    -- Tenant-specific E-Way Bill Portal Credentials (NIC Portal)
    ewb_gstin VARCHAR(15) NOT NULL,

    -- Authentication & Session Management
    auth_token VARCHAR(1000),
    sek VARCHAR(500),  -- Session Encryption Key
    token_expiry TIMESTAMP,
    
    -- Business Rules
    ewb_threshold NUMERIC(15, 2) DEFAULT 50000.00,
    is_active BOOLEAN DEFAULT FALSE,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Key Constraints
    CONSTRAINT fk_tenant_ewb_cred_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_tenant_ewb_cred_gsp_config 
        FOREIGN KEY (gsp_config_id) REFERENCES gsp_configuration(id) ON DELETE RESTRICT,
    
    -- Unique Constraint: One credential set per tenant
    CONSTRAINT uk_tenant_ewb_credentials_tenant UNIQUE (tenant_id)
);

-- Indexes for tenant_eway_bill_credentials
CREATE INDEX idx_tenant_ewb_cred_tenant_id ON tenant_eway_bill_credentials(tenant_id);
CREATE INDEX idx_tenant_ewb_cred_gsp_config ON tenant_eway_bill_credentials(gsp_config_id);
CREATE INDEX idx_tenant_ewb_cred_gstin ON tenant_eway_bill_credentials(ewb_gstin);
CREATE INDEX idx_tenant_ewb_cred_active ON tenant_eway_bill_credentials(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_tenant_ewb_cred_token_expiry ON tenant_eway_bill_credentials(token_expiry);

-- ============================================================================
-- Part 3: Table and column comments
-- ============================================================================

-- gsp_configuration comments
COMMENT ON TABLE gsp_configuration IS 'Shared GSP configuration for E-Way Bill integration across multiple tenants';
COMMENT ON COLUMN gsp_configuration.config_name IS 'Unique name for this GSP configuration (e.g., TaxPro Sandbox, TaxPro Production)';
COMMENT ON COLUMN gsp_configuration.gsp_provider IS 'GSP Provider name (e.g., TaxPro GSP)';
COMMENT ON COLUMN gsp_configuration.integration_mode IS 'Integration mode: OFFLINE, GSP_API, or HYBRID';
COMMENT ON COLUMN gsp_configuration.asp_user_id IS 'ASP User ID provided by GSP (shared across tenants)';
COMMENT ON COLUMN gsp_configuration.asp_password IS 'Encrypted ASP password (use EncryptionUtil to encrypt)';
COMMENT ON COLUMN gsp_configuration.is_production IS 'Whether this is a production or sandbox configuration';
COMMENT ON COLUMN gsp_configuration.is_active IS 'Whether this GSP configuration is active';

-- tenant_eway_bill_credentials comments
COMMENT ON TABLE tenant_eway_bill_credentials IS 'Tenant-specific E-Way Bill credentials linked to shared GSP configuration';
COMMENT ON COLUMN tenant_eway_bill_credentials.gsp_config_id IS 'Reference to shared GSP configuration containing ASP credentials';
COMMENT ON COLUMN tenant_eway_bill_credentials.ewb_gstin IS 'Tenant GSTIN registered on E-Way Bill portal';
COMMENT ON COLUMN tenant_eway_bill_credentials.auth_token IS 'Current authentication token (6-hour validity)';
COMMENT ON COLUMN tenant_eway_bill_credentials.sek IS 'Session Encryption Key for payload encryption';
COMMENT ON COLUMN tenant_eway_bill_credentials.token_expiry IS 'Timestamp when auth token expires';
COMMENT ON COLUMN tenant_eway_bill_credentials.ewb_threshold IS 'Invoice value threshold for mandatory E-Way Bill (default 50000)';
COMMENT ON COLUMN tenant_eway_bill_credentials.is_active IS 'Whether E-Way Bill integration is active for this tenant';
