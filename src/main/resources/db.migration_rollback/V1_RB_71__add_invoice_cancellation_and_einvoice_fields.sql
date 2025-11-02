-- ===================================
-- Rollback: Invoice Cancellation, E-Invoice Support, Manual Invoice Support, and Work Type
-- Version: V1_RB_71
-- Description: Rollback script to remove cancellation, e-invoice, manual invoice, and work type fields from invoice table
-- Date: 2025-11-01 (Updated: 2025-11-02)
-- ===================================

-- ===================================
-- PART 1: Remove Work Type Fields and Indexes
-- ===================================

-- Drop work type index first
DROP INDEX IF EXISTS idx_invoice_work_type;

-- Remove work type field
ALTER TABLE invoice DROP COLUMN IF EXISTS work_type;


-- ===================================
-- PART 2: Remove Manual Invoice Fields and Indexes
-- ===================================

-- Drop manual invoice index first
DROP INDEX IF EXISTS idx_invoice_manual;

-- Remove manual invoice field
ALTER TABLE invoice DROP COLUMN IF EXISTS is_manual_invoice;


-- ===================================
-- PART 3: Remove E-Invoice Fields and Indexes
-- ===================================

-- Drop e-invoice indexes first
DROP INDEX IF EXISTS idx_invoice_ack_date;
DROP INDEX IF EXISTS idx_invoice_irn;

-- Remove E-Invoice fields
ALTER TABLE invoice DROP COLUMN IF EXISTS qr_code_data;
ALTER TABLE invoice DROP COLUMN IF EXISTS ack_date;
ALTER TABLE invoice DROP COLUMN IF EXISTS ack_no;
ALTER TABLE invoice DROP COLUMN IF EXISTS irn;


-- ===================================
-- PART 4: Remove Cancellation Fields and Indexes
-- ===================================

-- Drop cancellation index
DROP INDEX IF EXISTS idx_invoice_cancellation_date;

-- Remove cancellation fields
ALTER TABLE invoice 
DROP COLUMN IF EXISTS cancelled_by,
DROP COLUMN IF EXISTS cancellation_reason,
DROP COLUMN IF EXISTS cancellation_date;

