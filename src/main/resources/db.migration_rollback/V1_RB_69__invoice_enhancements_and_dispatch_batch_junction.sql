-- Rollback: Invoice enhancements and multi-dispatch batch support
-- Rollback operations in reverse order (Part 6 → Part 1)

-- ===================================
-- Part 6: Restore Original DispatchBatch Order Column Names
-- ===================================

-- Rename order_po_number back to purchase_order_number
ALTER TABLE dispatch_batch
    RENAME COLUMN order_po_number TO purchase_order_number;

-- Rename order_date back to purchase_order_date_time
ALTER TABLE dispatch_batch
    RENAME COLUMN order_date TO purchase_order_date_time;

-- Restore original comments
COMMENT ON COLUMN dispatch_batch.purchase_order_number IS 'Purchase Order Number for the dispatched batch';
COMMENT ON COLUMN dispatch_batch.purchase_order_date_time IS 'Purchase Order Date Time for the dispatched batch';

-- ===================================
-- Part 5: Restore dispatch_batch_id to Invoice
-- ===================================

-- Add back the dispatch_batch_id column
ALTER TABLE invoice
ADD COLUMN IF NOT EXISTS dispatch_batch_id BIGINT;

-- Recreate the foreign key constraint
ALTER TABLE invoice
ADD CONSTRAINT fk_invoice_dispatch_batch 
  FOREIGN KEY (dispatch_batch_id) REFERENCES dispatch_batch(id);

-- ===================================
-- Part 4: Remove Supplier Detail Fields
-- ===================================

-- Drop supplier detail columns from invoice table
ALTER TABLE invoice
DROP COLUMN IF EXISTS supplier_state_code,
DROP COLUMN IF EXISTS supplier_address,
DROP COLUMN IF EXISTS supplier_name,
DROP COLUMN IF EXISTS supplier_gstin;

-- ===================================
-- Part 3: Remove Terms and Conditions and Bank Details Fields
-- ===================================

-- Drop amount in words column from invoice table
ALTER TABLE invoice
DROP COLUMN IF EXISTS amount_in_words;

-- Drop bank detail columns from invoice table
ALTER TABLE invoice
DROP COLUMN IF EXISTS ifsc_code,
DROP COLUMN IF EXISTS account_number,
DROP COLUMN IF EXISTS bank_name;

-- Drop terms and conditions column
ALTER TABLE invoice
DROP COLUMN IF EXISTS terms_and_conditions;

-- ===================================
-- Part 2: Drop Invoice-DispatchBatch Junction Table
-- ===================================

-- Drop indexes
DROP INDEX IF EXISTS idx_invoice_dispatch_unique;
DROP INDEX IF EXISTS idx_invoice_dispatch_tenant;
DROP INDEX IF EXISTS idx_invoice_dispatch_deleted;
DROP INDEX IF EXISTS idx_invoice_dispatch_batch;
DROP INDEX IF EXISTS idx_invoice_dispatch_invoice;

-- Drop table
DROP TABLE IF EXISTS invoice_dispatch_batch;

-- Drop sequence
DROP SEQUENCE IF EXISTS invoice_dispatch_batch_sequence;

-- ===================================
-- Part 1: Remove Invoice Approval Field
-- ===================================

ALTER TABLE invoice
DROP COLUMN IF EXISTS approved_by;

