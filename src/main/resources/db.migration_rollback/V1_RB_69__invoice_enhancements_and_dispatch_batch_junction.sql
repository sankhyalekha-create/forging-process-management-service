-- Rollback: Invoice enhancements and multi-dispatch batch support
-- Rollback operations in reverse order (Part 7 → Part 1)

-- ===================================
-- Part 7: Restore dispatch_batch_id to DeliveryChallan
-- ===================================

-- Add back the dispatch_batch_id column
ALTER TABLE delivery_challan
ADD COLUMN IF NOT EXISTS dispatch_batch_id BIGINT;

-- Recreate the foreign key constraint
ALTER TABLE delivery_challan
ADD CONSTRAINT fk_delivery_challan_dispatch_batch 
  FOREIGN KEY (dispatch_batch_id) REFERENCES dispatch_batch(id);

-- ===================================
-- Part 6: Drop DeliveryChallan-DispatchBatch Junction Table
-- ===================================

-- Drop indexes
DROP INDEX IF EXISTS idx_delivery_challan_dispatch_unique;
DROP INDEX IF EXISTS idx_delivery_challan_dispatch_tenant;
DROP INDEX IF EXISTS idx_delivery_challan_dispatch_deleted;
DROP INDEX IF EXISTS idx_delivery_challan_dispatch_batch;
DROP INDEX IF EXISTS idx_delivery_challan_dispatch_challan;

-- Drop table
DROP TABLE IF EXISTS delivery_challan_dispatch_batch;

-- Drop sequence
DROP SEQUENCE IF EXISTS delivery_challan_dispatch_batch_sequence;

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

