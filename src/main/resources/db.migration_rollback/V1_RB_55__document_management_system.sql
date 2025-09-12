-- Rollback: Document Management System for FOPMAS
-- Version: V1_RB_55
-- Description: Complete rollback of document management system

BEGIN;

-- ======================================================================
-- PART 1: Drop Triggers and Functions
-- ======================================================================

-- Drop triggers
DROP TRIGGER IF EXISTS trigger_document_updated_at ON document;
DROP TRIGGER IF EXISTS trigger_document_metadata_updated_at ON document_metadata;
DROP TRIGGER IF EXISTS trigger_tenant_storage_quota_updated_at ON tenant_storage_quota;

-- Drop functions
DROP FUNCTION IF EXISTS update_document_updated_at();

-- ======================================================================
-- PART 2: Drop Indexes
-- ======================================================================

-- Document table indexes
DROP INDEX IF EXISTS idx_document_tenant_id;
DROP INDEX IF EXISTS idx_document_file_hash;
DROP INDEX IF EXISTS idx_document_category;
DROP INDEX IF EXISTS idx_document_type;
DROP INDEX IF EXISTS idx_document_created_at;
DROP INDEX IF EXISTS idx_document_file_extension;
DROP INDEX IF EXISTS idx_document_search;

-- Document metadata indexes
DROP INDEX IF EXISTS idx_document_metadata_document_id;
DROP INDEX IF EXISTS idx_document_metadata_key;
DROP INDEX IF EXISTS idx_document_metadata_key_value;

-- Document link indexes
DROP INDEX IF EXISTS idx_document_link_document_id;
DROP INDEX IF EXISTS idx_document_link_entity;
DROP INDEX IF EXISTS idx_document_link_entity_type;
DROP INDEX IF EXISTS idx_document_link_tenant_entity;

-- Tenant storage quota indexes
DROP INDEX IF EXISTS idx_tenant_storage_quota_tenant_id;
DROP INDEX IF EXISTS idx_tenant_storage_quota_usage;

-- No document access log indexes to drop

-- ======================================================================
-- PART 3: Drop Tables (in reverse dependency order)
-- ======================================================================

-- Drop linking tables (reference document)
DROP TABLE IF EXISTS document_link;
DROP TABLE IF EXISTS document_metadata;

-- Drop storage quota table (references tenant only)
DROP TABLE IF EXISTS tenant_storage_quota;

-- Drop core document table last
DROP TABLE IF EXISTS document;

-- ======================================================================
-- PART 4: Drop Sequences
-- ======================================================================

DROP SEQUENCE IF EXISTS tenant_storage_quota_sequence;
DROP SEQUENCE IF EXISTS document_link_sequence;
DROP SEQUENCE IF EXISTS document_metadata_sequence;
DROP SEQUENCE IF EXISTS document_sequence;

COMMIT;

-- ======================================================================
-- ROLLBACK SUMMARY
-- ======================================================================
-- Removed:
-- 1. All document management tables (document, document_metadata, document_link, tenant_storage_quota)
-- 2. All indexes and performance optimizations
-- 3. All triggers and functions
-- 4. All sequences and constraints
-- 5. All document storage quotas for tenants
--
-- Note: Physical files in /var/fopmas/documents/ directory are NOT deleted
-- by this rollback script and should be manually cleaned if needed.
-- ======================================================================
