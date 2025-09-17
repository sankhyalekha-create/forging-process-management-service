-- Migration: Fix Document Category Constraint
-- Version: V1_56
-- Description: Add IDENTITY to the document_category check constraint

BEGIN;

-- Drop the existing constraint
ALTER TABLE document DROP CONSTRAINT IF EXISTS document_document_category_check;

-- Add the updated constraint that includes IDENTITY
ALTER TABLE document ADD CONSTRAINT document_document_category_check 
    CHECK (document_category IN ('INVOICE', 'CERTIFICATE', 'PROCESS', 'SPECIFICATION', 'REPORT', 'IDENTITY', 'OTHER'));

-- Update the comment to reflect the change
COMMENT ON COLUMN document.document_category IS 'Document category: INVOICE, CERTIFICATE, PROCESS, SPECIFICATION, REPORT, IDENTITY, OTHER';

COMMIT;

-- ======================================================================
-- MIGRATION SUMMARY
-- ======================================================================
-- Changes:
-- 1. Updated document_category check constraint to include 'IDENTITY'
-- 2. Updated column comment to reflect the new allowed values
--
-- This fixes the constraint violation when uploading documents with
-- IDENTITY category, which is a valid enum value in DocumentCategory.java
-- ======================================================================
