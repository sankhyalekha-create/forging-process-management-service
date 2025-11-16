BEGIN;

-- Rollback script for V1_74__create_eway_bill_table.sql
-- This script safely removes the eway_bill table and its sequence

-- Drop indexes first (if they exist)
DROP INDEX IF EXISTS idx_eway_bill_date;
DROP INDEX IF EXISTS idx_eway_bill_number;
DROP INDEX IF EXISTS idx_eway_bill_deleted;
DROP INDEX IF EXISTS idx_eway_bill_validity;
DROP INDEX IF EXISTS idx_eway_bill_tenant_status;
DROP INDEX IF EXISTS idx_eway_bill_challan;
DROP INDEX IF EXISTS idx_eway_bill_invoice;
DROP INDEX IF EXISTS idx_eway_bill_dispatch_batch;

-- Drop the eway_bill table (foreign key constraints will be dropped automatically)
DROP TABLE IF EXISTS eway_bill CASCADE;

-- Drop the sequence
DROP SEQUENCE IF EXISTS eway_bill_sequence;

COMMIT;
