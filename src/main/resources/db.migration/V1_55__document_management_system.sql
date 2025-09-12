-- Migration: Document Management System for FOPMAS
-- Version: V1_55
-- Description: Add complete document management with entity linking, storage quotas, and audit trail

BEGIN;

-- ======================================================================
-- PART 1: Create Sequences
-- ======================================================================

CREATE SEQUENCE document_sequence START 1 INCREMENT BY 1;
CREATE SEQUENCE document_metadata_sequence START 1 INCREMENT BY 1;
CREATE SEQUENCE document_link_sequence START 1 INCREMENT BY 1;
CREATE SEQUENCE tenant_storage_quota_sequence START 1 INCREMENT BY 1;

-- ======================================================================
-- PART 2: Core Document Table
-- ======================================================================

CREATE TABLE document (
    id BIGINT PRIMARY KEY DEFAULT nextval('document_sequence'),
    tenant_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    compressed_file_path VARCHAR(500),
    mime_type VARCHAR(100) NOT NULL,
    file_extension VARCHAR(10) NOT NULL,
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes >= 0),
    compressed_size_bytes BIGINT CHECK (compressed_size_bytes >= 0),
    file_hash VARCHAR(64) NOT NULL,
    document_category VARCHAR(50) CHECK (document_category IN ('INVOICE', 'CERTIFICATE', 'PROCESS', 'SPECIFICATION', 'REPORT', 'OTHER')),
    document_type VARCHAR(50) CHECK (document_type IN ('PDF', 'IMAGE', 'EXCEL', 'WORD', 'OTHER')),
    description TEXT,
    tags VARCHAR(1000),
    upload_source VARCHAR(50) DEFAULT 'WEB_UI',
    is_compressed BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT DEFAULT 1,
    
    CONSTRAINT fk_document_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

-- Comments for document table
COMMENT ON TABLE document IS 'Core document storage table for FOPMAS document management';
COMMENT ON COLUMN document.file_name IS 'Unique UUID-based filename for storage';
COMMENT ON COLUMN document.original_file_name IS 'Original filename as uploaded by user';
COMMENT ON COLUMN document.file_path IS 'Full file system path: /var/fopmas/documents/tenants/{tenant_id}/{category}/{YYYY-MM}/{uuid}.{ext}';
COMMENT ON COLUMN document.compressed_file_path IS 'Path to compressed version if available';
COMMENT ON COLUMN document.file_hash IS 'SHA-256 hash for file integrity verification';
COMMENT ON COLUMN document.document_category IS 'Simple document category: INVOICE, CERTIFICATE, PROCESS, SPECIFICATION, REPORT, OTHER';
COMMENT ON COLUMN document.document_type IS 'Auto-detected document type based on MIME type';
COMMENT ON COLUMN document.description IS 'Document title and description (combined for simplicity)';
COMMENT ON COLUMN document.tags IS 'Comma-separated tags for search and organization';

-- ======================================================================
-- PART 3: Document Metadata Table (Optional custom metadata)
-- ======================================================================

CREATE TABLE document_metadata (
    id BIGINT PRIMARY KEY DEFAULT nextval('document_metadata_sequence'),
    document_id BIGINT NOT NULL,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value TEXT,
    metadata_type VARCHAR(20) DEFAULT 'STRING' CHECK (metadata_type IN ('STRING', 'NUMBER', 'DATE', 'BOOLEAN', 'JSON', 'URL')),
    deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_document_metadata_document FOREIGN KEY (document_id) REFERENCES document(id),
    CONSTRAINT unique_document_metadata_key UNIQUE (document_id, metadata_key, deleted)
);

COMMENT ON TABLE document_metadata IS 'Optional custom metadata for documents';
COMMENT ON COLUMN document_metadata.metadata_key IS 'Metadata field name (e.g., batchNumber, operatorId)';
COMMENT ON COLUMN document_metadata.metadata_value IS 'Metadata field value stored as text';
COMMENT ON COLUMN document_metadata.metadata_type IS 'Type hint for proper value parsing';

-- ======================================================================
-- PART 4: Document Link Table (Entity Association)
-- ======================================================================

CREATE TABLE document_link (
    id BIGINT PRIMARY KEY DEFAULT nextval('document_link_sequence'),
    document_id BIGINT NOT NULL,
    entity_type VARCHAR(50) NOT NULL CHECK (entity_type IN (
        'RAW_MATERIAL', 'ITEM', 'PRODUCT',
        'FORGE', 'MACHINING_BATCH', 'HEAT_TREATMENT_BATCH', 'INSPECTION_BATCH', 'DISPATCH_BATCH',
        'FORGING_LINE', 'MACHINE', 'FURNACE', 'INSPECTION_EQUIPMENT', 'OPERATOR',
        'VENDOR_DISPATCH_BATCH', 'VENDOR_RECEIVE_BATCH', 'CUSTOMER', 'SUPPLIER',
        'TENANT', 'OTHER'
    )),
    entity_id BIGINT NOT NULL,
    link_type VARCHAR(50) DEFAULT 'ATTACHED' CHECK (link_type IN ('ATTACHED', 'REFERENCED', 'DERIVED', 'SUPERSEDED', 'RELATED')),
    relationship_context VARCHAR(200),
    deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_document_link_document FOREIGN KEY (document_id) REFERENCES document(id),
    CONSTRAINT unique_document_entity_link UNIQUE (document_id, entity_type, entity_id, deleted)
);

COMMENT ON TABLE document_link IS 'Links documents to FOPMAS business entities';
COMMENT ON COLUMN document_link.entity_type IS 'Type of FOPMAS entity (RAW_MATERIAL, ITEM, FORGE, VENDOR_DISPATCH_BATCH, OPERATOR, etc.)';
COMMENT ON COLUMN document_link.entity_id IS 'ID of the specific entity instance';
COMMENT ON COLUMN document_link.link_type IS 'Type of relationship between document and entity';
COMMENT ON COLUMN document_link.relationship_context IS 'Additional context about the relationship';

-- ======================================================================
-- PART 5: Tenant Storage Quota Table
-- ======================================================================

CREATE TABLE tenant_storage_quota (
    id BIGINT PRIMARY KEY DEFAULT nextval('tenant_storage_quota_sequence'),
    tenant_id BIGINT NOT NULL UNIQUE,
    max_storage_bytes BIGINT DEFAULT 1073741824 CHECK (max_storage_bytes >= 0), -- 1GB default
    used_storage_bytes BIGINT DEFAULT 0 CHECK (used_storage_bytes >= 0),
    max_file_size_bytes BIGINT DEFAULT 10485760 CHECK (max_file_size_bytes >= 0), -- 10MB default
    max_files_per_entity INTEGER DEFAULT 100 CHECK (max_files_per_entity >= 1),
    quota_enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_tenant_storage_quota_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

COMMENT ON TABLE tenant_storage_quota IS 'Storage quotas and limits per tenant';
COMMENT ON COLUMN tenant_storage_quota.max_storage_bytes IS 'Maximum total storage allowed for tenant (1GB default)';
COMMENT ON COLUMN tenant_storage_quota.used_storage_bytes IS 'Current storage usage in bytes';
COMMENT ON COLUMN tenant_storage_quota.max_file_size_bytes IS 'Maximum file size per document (10MB default)';
COMMENT ON COLUMN tenant_storage_quota.max_files_per_entity IS 'Maximum number of files per business entity';

-- ======================================================================
-- PART 6: Performance Indexes
-- ======================================================================

-- Document table indexes
CREATE INDEX idx_document_tenant_id ON document (tenant_id) WHERE deleted = false;
CREATE INDEX idx_document_file_hash ON document (file_hash) WHERE deleted = false;
CREATE INDEX idx_document_category ON document (tenant_id, document_category) WHERE deleted = false;
CREATE INDEX idx_document_type ON document (tenant_id, document_type) WHERE deleted = false;
CREATE INDEX idx_document_created_at ON document (tenant_id, created_at DESC) WHERE deleted = false;
CREATE INDEX idx_document_file_extension ON document (tenant_id, file_extension) WHERE deleted = false;
CREATE INDEX idx_document_search ON document USING gin(to_tsvector('english', original_file_name || ' ' || COALESCE(description, '') || ' ' || COALESCE(tags, ''))) WHERE deleted = false;

-- Document metadata indexes
CREATE INDEX idx_document_metadata_document_id ON document_metadata (document_id) WHERE deleted = false;
CREATE INDEX idx_document_metadata_key ON document_metadata (metadata_key) WHERE deleted = false;
CREATE INDEX idx_document_metadata_key_value ON document_metadata (metadata_key, metadata_value) WHERE deleted = false;

-- Document link indexes
CREATE INDEX idx_document_link_document_id ON document_link (document_id) WHERE deleted = false;
CREATE INDEX idx_document_link_entity ON document_link (entity_type, entity_id) WHERE deleted = false;
CREATE INDEX idx_document_link_entity_type ON document_link (entity_type) WHERE deleted = false;
CREATE INDEX idx_document_link_tenant_entity ON document_link (entity_type, entity_id) 
    INCLUDE (document_id) WHERE deleted = false;

-- Tenant storage quota indexes
CREATE INDEX idx_tenant_storage_quota_tenant_id ON tenant_storage_quota (tenant_id);
CREATE INDEX idx_tenant_storage_quota_usage ON tenant_storage_quota (used_storage_bytes, max_storage_bytes) WHERE quota_enabled = true;

-- ======================================================================
-- PART 7: Create Default Storage Quotas for Existing Tenants
-- ======================================================================

-- Insert default storage quotas for all existing tenants
INSERT INTO tenant_storage_quota (tenant_id, max_storage_bytes, used_storage_bytes, max_file_size_bytes, max_files_per_entity, quota_enabled)
SELECT 
    t.id,
    1073741824,  -- 1GB default max storage
    0,           -- 0 bytes used initially
    10485760,    -- 10MB default max file size
    100,         -- 100 files per entity default
    true         -- quota enabled by default
FROM tenant t
WHERE t.deleted = false
ON CONFLICT (tenant_id) DO NOTHING;

-- ======================================================================
-- PART 8: Update Statistics and Triggers (Optional)
-- ======================================================================

-- Function to automatically update document updated_at timestamp
CREATE OR REPLACE FUNCTION update_document_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for document table
CREATE TRIGGER trigger_document_updated_at
    BEFORE UPDATE ON document
    FOR EACH ROW
    EXECUTE FUNCTION update_document_updated_at();

-- Trigger for document_metadata table
CREATE TRIGGER trigger_document_metadata_updated_at
    BEFORE UPDATE ON document_metadata
    FOR EACH ROW
    EXECUTE FUNCTION update_document_updated_at();

-- Trigger for tenant_storage_quota table
CREATE TRIGGER trigger_tenant_storage_quota_updated_at
    BEFORE UPDATE ON tenant_storage_quota
    FOR EACH ROW
    EXECUTE FUNCTION update_document_updated_at();

-- ======================================================================
-- PART 9: Grant Permissions (if using specific database user)
-- ======================================================================

-- Grant permissions on sequences
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO PUBLIC;

-- Grant permissions on tables
GRANT SELECT, INSERT, UPDATE, DELETE ON document TO PUBLIC;
GRANT SELECT, INSERT, UPDATE, DELETE ON document_metadata TO PUBLIC;
GRANT SELECT, INSERT, UPDATE, DELETE ON document_link TO PUBLIC;
GRANT SELECT, INSERT, UPDATE, DELETE ON tenant_storage_quota TO PUBLIC;

COMMIT;

-- ======================================================================
-- MIGRATION SUMMARY
-- ======================================================================
-- Tables Created:
-- 1. document - Core document storage with simplified categorization
-- 2. document_metadata - Optional custom metadata for documents
-- 3. document_link - Links documents to FOPMAS business entities
-- 4. tenant_storage_quota - Per-tenant storage quotas and limits (10MB max file, 1GB max storage)
--
-- Features:
-- - Entity-focused document attachment (Raw Material, Item, Operator, Vendor Dispatch Batch, etc.)
-- - 6 simple document categories (INVOICE, CERTIFICATE, PROCESS, SPECIFICATION, REPORT, OTHER)
-- - Auto-detected document types (PDF, IMAGE, EXCEL, WORD, OTHER)
-- - Storage quotas with 10MB max file size and 1GB max per tenant
-- - Category-based file organization: /tenants/{tenant_id}/{category}/{YYYY-MM}/
-- - Search optimization with full-text search support
-- - Soft deletes for data preservation
-- ======================================================================
