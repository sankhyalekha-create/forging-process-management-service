-- Rollback Migration: Fix Document Category Constraint
-- Version: V1_RB_56
-- Description: Rollback IDENTITY from the document_category check constraint

BEGIN;

-- Drop the updated constraint that includes IDENTITY
ALTER TABLE document DROP CONSTRAINT IF EXISTS document_document_category_check;

-- Add back the original constraint without IDENTITY
ALTER TABLE document ADD CONSTRAINT document_document_category_check 
    CHECK (document_category IN ('INVOICE', 'CERTIFICATE', 'PROCESS', 'SPECIFICATION', 'REPORT', 'OTHER'));

-- Revert the comment to the original state
COMMENT ON COLUMN document.document_category IS 'Simple document category: INVOICE, CERTIFICATE, PROCESS, SPECIFICATION, REPORT, OTHER';

COMMIT;

-- ======================================================================
-- ROLLBACK SUMMARY
-- ======================================================================
-- Changes Reverted:
-- 1. Removed 'IDENTITY' from document_category check constraint
-- 2. Reverted column comment to original values
--
-- This rollback restores the original constraint that only allows:
-- INVOICE, CERTIFICATE, PROCESS, SPECIFICATION, REPORT, OTHER
-- (excluding IDENTITY which was added in V1_56)
-- ======================================================================
