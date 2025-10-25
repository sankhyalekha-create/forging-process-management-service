-- Migration: Create Transporter Table and Add Document Link Support
-- Version: V1_64
-- Description: 
--   1. Creates the transporter table for managing transporter information
--      required for GST-compliant invoice generation and E-Way Bill processing
--   2. Adds TRANSPORTER entity type to document_link table CHECK constraint
--      to enable document attachment functionality
-- Date: 2025-10-24

BEGIN;

-- ============================================================================
-- PART 1: Create Transporter Table
-- ============================================================================

-- Create transporter table
CREATE TABLE IF NOT EXISTS transporter (
  -- Primary Key
  id BIGSERIAL PRIMARY KEY,
  
  -- Basic Information
  transporter_name VARCHAR(200) NOT NULL,
  
  -- GST and Tax Information
  gstin VARCHAR(15),
  transporter_id_number VARCHAR(15),
  pan_number VARCHAR(10),
  is_gst_registered BOOLEAN DEFAULT FALSE,
  
  -- Address Information
  address TEXT,
  state_code VARCHAR(2),
  pincode VARCHAR(6),
  
  -- Contact Information
  phone_number VARCHAR(15),
  alternate_phone_number VARCHAR(15),
  email VARCHAR(255),
  
  -- Banking Information
  bank_account_number VARCHAR(50),
  ifsc_code VARCHAR(11),
  bank_name VARCHAR(100),
  
  -- Additional Information
  notes TEXT,
  
  -- Audit Fields
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE,
  
  -- Multi-tenant Foreign Key
  tenant_id BIGINT NOT NULL,
  
  -- Constraints
  CONSTRAINT fk_transporter_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

-- Create indexes for performance optimization
CREATE INDEX IF NOT EXISTS idx_transporter_name 
  ON transporter(transporter_name);

CREATE INDEX IF NOT EXISTS idx_transporter_gstin 
  ON transporter(gstin) 
  WHERE gstin IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_transporter_id_number 
  ON transporter(transporter_id_number) 
  WHERE transporter_id_number IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_transporter_tenant_id 
  ON transporter(tenant_id);

CREATE INDEX IF NOT EXISTS idx_transporter_deleted 
  ON transporter(deleted);

-- Create partial unique index for active records
-- Ensures transporter name is unique per tenant for non-deleted records
CREATE UNIQUE INDEX IF NOT EXISTS unique_transporter_name_tenant_active 
  ON transporter(transporter_name, tenant_id) 
  WHERE deleted = FALSE;

-- Create partial unique index for GSTIN
-- Ensures GSTIN is unique per tenant for non-deleted records
CREATE UNIQUE INDEX IF NOT EXISTS unique_transporter_gstin_tenant_active 
  ON transporter(gstin, tenant_id) 
  WHERE deleted = FALSE AND gstin IS NOT NULL;

-- Create partial unique index for Transporter ID Number
-- Ensures Transporter ID is unique per tenant for non-deleted records
CREATE UNIQUE INDEX IF NOT EXISTS unique_transporter_id_number_tenant_active 
  ON transporter(transporter_id_number, tenant_id) 
  WHERE deleted = FALSE AND transporter_id_number IS NOT NULL;

-- Add comments for documentation
COMMENT ON TABLE transporter IS 'Stores transporter information for GST-compliant invoice generation and E-Way Bill processing';
COMMENT ON COLUMN transporter.transporter_name IS 'Legal/registered name of the transporter';
COMMENT ON COLUMN transporter.gstin IS 'GST Identification Number (15 digits) - Format: 22ABCDE1234F1Z5';
COMMENT ON COLUMN transporter.transporter_id_number IS 'Transporter ID for E-Way Bill (GSTIN or TIN) - Can be same as GSTIN for registered transporters';
COMMENT ON COLUMN transporter.pan_number IS 'PAN card number (10 characters) - Format: ABCDE1234F';
COMMENT ON COLUMN transporter.state_code IS 'State code as per GST (2 digits) - Examples: 09 for UP, 08 for Rajasthan';
COMMENT ON COLUMN transporter.pincode IS 'PIN code (6 digits)';
COMMENT ON COLUMN transporter.is_gst_registered IS 'Whether the transporter is registered under GST';
COMMENT ON COLUMN transporter.deleted IS 'Soft delete flag - TRUE if record is deleted';
COMMENT ON COLUMN transporter.deleted_at IS 'Timestamp when record was soft deleted';

-- ============================================================================
-- PART 2: Add TRANSPORTER Entity Type to Document Link
-- ============================================================================

-- Find and drop the existing CHECK constraint on entity_type column
-- PostgreSQL auto-generates constraint names, so we need to find it dynamically
DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    -- Find the CHECK constraint on entity_type column
    SELECT conname INTO constraint_name
    FROM pg_constraint c
    JOIN pg_class t ON c.conrelid = t.oid
    JOIN pg_namespace n ON t.relnamespace = n.oid
    WHERE n.nspname = 'public' 
      AND t.relname = 'document_link' 
      AND c.contype = 'c'
      AND pg_get_constraintdef(c.oid) LIKE '%entity_type%';
    
    -- Drop the existing constraint if found
    IF constraint_name IS NOT NULL THEN
        EXECUTE 'ALTER TABLE document_link DROP CONSTRAINT ' || constraint_name;
        RAISE NOTICE 'Dropped existing constraint: %', constraint_name;
    END IF;
END $$;

-- Add the new CHECK constraint that includes TRANSPORTER
ALTER TABLE document_link ADD CONSTRAINT document_link_entity_type_check 
    CHECK (entity_type IN (
        'RAW_MATERIAL', 'ITEM', 'PRODUCT', 'ORDER', 'ITEM_WORKFLOW',
        'FORGE', 'MACHINING_BATCH', 'HEAT_TREATMENT_BATCH', 'INSPECTION_BATCH', 'DISPATCH_BATCH',
        'FORGING_LINE', 'MACHINE', 'MACHINE_SET', 'FURNACE', 'INSPECTION_EQUIPMENT', 'OPERATOR',
        'VENDOR_DISPATCH_BATCH', 'VENDOR_RECEIVE_BATCH', 'BUYER', 'SUPPLIER', 'TRANSPORTER',
        'TENANT', 'OTHER'
    ));

-- Add a comment to document the change
COMMENT ON CONSTRAINT document_link_entity_type_check ON document_link IS 
    'Ensures entity_type contains only valid entity types including TRANSPORTER added in V1_64';

-- ============================================================================
-- Migration Summary
-- ============================================================================
-- This migration creates complete TRANSPORTER support:
--   ✅ Transporter table with all required fields
--   ✅ Indexes for performance optimization
--   ✅ Unique constraints for data integrity
--   ✅ Document management integration
--   ✅ TRANSPORTER entity type added to DocumentLink.EntityType enum (Java)
-- 
-- Use Cases:
--   - Attach PAN card, GST certificate, transport license to transporters
--   - Store contracts, insurance documents, bank details
--   - Maintain compliance documentation

COMMIT;
