-- ===================================
-- Payment Entity Rollback
-- Version: V1_RB_70
-- Description: Rollback script for payment table and invoice enhancements
-- ===================================

-- ===================================
-- Part 1: Remove Payment Tracking Columns from Invoice Table
-- ===================================

DROP INDEX IF EXISTS idx_invoice_tds_amount;
DROP INDEX IF EXISTS idx_invoice_total_paid_amount;

ALTER TABLE invoice
    DROP COLUMN IF EXISTS total_tds_amount_deducted;

ALTER TABLE invoice
    DROP COLUMN IF EXISTS total_paid_amount;

-- ===================================
-- Part 2: Drop Payment Table Indexes
-- ===================================

DROP INDEX IF EXISTS idx_payment_invoice;
DROP INDEX IF EXISTS idx_payment_date_time;
DROP INDEX IF EXISTS idx_payment_status;
DROP INDEX IF EXISTS idx_payment_tenant;
DROP INDEX IF EXISTS idx_payment_deleted;

-- ===================================
-- Part 3: Drop Payment Table
-- ===================================

DROP TABLE IF EXISTS payment CASCADE;

-- ===================================
-- Part 4: Drop Payment Sequence
-- ===================================

DROP SEQUENCE IF EXISTS payment_sequence;

-- ===================================
-- Part 5: Restore Original Invoice Status Comment
-- ===================================

COMMENT ON COLUMN invoice.status IS 'Invoice status: DRAFT, GENERATED, SENT, PAID, CANCELLED';

