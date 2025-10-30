-- ===================================
-- Invoice Cancellation Rollback
-- Version: V1_RB_71
-- Description: Rollback script for invoice cancellation fields
-- ===================================

-- Drop index
DROP INDEX IF EXISTS idx_invoice_cancellation_date;

-- Remove cancellation fields
ALTER TABLE invoice 
DROP COLUMN IF EXISTS cancelled_by,
DROP COLUMN IF EXISTS cancellation_reason,
DROP COLUMN IF EXISTS cancellation_date;

