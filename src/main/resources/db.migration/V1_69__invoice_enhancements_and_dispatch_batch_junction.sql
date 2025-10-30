-- Migration: Invoice enhancements and multi-dispatch batch support
-- Part 1: Add approval tracking field to invoice table (improved invoice workflow: DRAFT → GENERATED)
-- Part 2: Create invoice_dispatch_batch junction table to support many-to-many relationship
-- Part 3: Add terms and conditions field to invoice table (persisted for legal compliance)
-- Part 4: Add supplier detail fields to invoice table (persisted for data integrity)
-- Date: 2025-10-28

-- ===================================
-- Part 1: Invoice Approval Field
-- ===================================

ALTER TABLE invoice 
ADD COLUMN IF NOT EXISTS approved_by VARCHAR(100);

COMMENT ON COLUMN invoice.approved_by IS 'Username/email of person who approved the invoice (moved from DRAFT to GENERATED status)';

-- ===================================
-- Part 2: Invoice-DispatchBatch Junction Table
-- ===================================

-- Create sequence for invoice_dispatch_batch
CREATE SEQUENCE IF NOT EXISTS invoice_dispatch_batch_sequence START WITH 1 INCREMENT BY 1;

-- Create invoice_dispatch_batch junction table
CREATE TABLE IF NOT EXISTS invoice_dispatch_batch (
  id BIGINT PRIMARY KEY DEFAULT nextval('invoice_dispatch_batch_sequence'),
  invoice_id BIGINT NOT NULL,
  dispatch_batch_id BIGINT NOT NULL,
  sequence_order INTEGER,
  tenant_id BIGINT NOT NULL,
  created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP WITHOUT TIME ZONE,
  deleted BOOLEAN DEFAULT FALSE,
  
  -- Foreign key constraints
  CONSTRAINT fk_invoice_dispatch_batch_invoice 
    FOREIGN KEY (invoice_id) REFERENCES invoice(id) ON DELETE CASCADE,
  
  CONSTRAINT fk_invoice_dispatch_batch_dispatch_batch 
    FOREIGN KEY (dispatch_batch_id) REFERENCES dispatch_batch(id) ON DELETE CASCADE,
  
  CONSTRAINT fk_invoice_dispatch_batch_tenant 
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_invoice_dispatch_invoice 
  ON invoice_dispatch_batch(invoice_id) WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_invoice_dispatch_batch 
  ON invoice_dispatch_batch(dispatch_batch_id) WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_invoice_dispatch_deleted 
  ON invoice_dispatch_batch(deleted);

CREATE INDEX IF NOT EXISTS idx_invoice_dispatch_tenant 
  ON invoice_dispatch_batch(tenant_id) WHERE deleted = FALSE;

-- Unique constraint: prevent duplicate invoice-dispatch batch associations
CREATE UNIQUE INDEX IF NOT EXISTS idx_invoice_dispatch_unique 
  ON invoice_dispatch_batch(invoice_id, dispatch_batch_id) 
  WHERE deleted = FALSE;

-- Comments for documentation
COMMENT ON TABLE invoice_dispatch_batch IS 
  'Junction table for many-to-many relationship between invoices and dispatch batches. Allows one invoice to reference multiple dispatch batches.';

COMMENT ON COLUMN invoice_dispatch_batch.sequence_order IS 
  'Display order of dispatch batches in the invoice listing.';

-- ===================================
-- Part 3: Terms and Conditions Field
-- ===================================

ALTER TABLE invoice
ADD COLUMN IF NOT EXISTS terms_and_conditions VARCHAR(2000);

COMMENT ON COLUMN invoice.terms_and_conditions IS 'Terms and conditions applicable at the time of invoice generation (persisted for legal compliance and audit trail)';

-- Add bank detail columns to invoice table
ALTER TABLE invoice
ADD COLUMN IF NOT EXISTS bank_name VARCHAR(100),
ADD COLUMN IF NOT EXISTS account_number VARCHAR(20),
ADD COLUMN IF NOT EXISTS ifsc_code VARCHAR(11);

-- Add comments for documentation
COMMENT ON COLUMN invoice.bank_name IS 'Bank name at the time of invoice generation (from TenantInvoiceSettings)';
COMMENT ON COLUMN invoice.account_number IS 'Account number at the time of invoice generation (from TenantInvoiceSettings)';
COMMENT ON COLUMN invoice.ifsc_code IS 'IFSC code at the time of invoice generation (from TenantInvoiceSettings)';

-- Add amount in words column to invoice table
ALTER TABLE invoice
ADD COLUMN IF NOT EXISTS amount_in_words VARCHAR(500);

COMMENT ON COLUMN invoice.amount_in_words IS 'Total invoice amount in words (e.g., "Seven Hundred Eight Rupees Only") for legal/display purposes';

-- ===================================
-- Part 4: Supplier Detail Fields
-- ===================================

-- Add supplier detail columns to invoice table
ALTER TABLE invoice
ADD COLUMN IF NOT EXISTS supplier_gstin VARCHAR(15),
ADD COLUMN IF NOT EXISTS supplier_name VARCHAR(200),
ADD COLUMN IF NOT EXISTS supplier_address VARCHAR(500),
ADD COLUMN IF NOT EXISTS supplier_state_code VARCHAR(2);

-- Add comments for documentation
COMMENT ON COLUMN invoice.supplier_gstin IS 'Supplier GSTIN at the time of invoice generation (from Tenant/GSTConfiguration)';
COMMENT ON COLUMN invoice.supplier_name IS 'Supplier name at the time of invoice generation (from Tenant)';
COMMENT ON COLUMN invoice.supplier_address IS 'Supplier address at the time of invoice generation (from Tenant)';
COMMENT ON COLUMN invoice.supplier_state_code IS 'Supplier state code at the time of invoice generation (from Tenant/GSTConfiguration)';

-- ===================================
-- Part 5: Remove Redundant dispatch_batch_id from Invoice
-- ===================================

-- Drop the old foreign key constraint
ALTER TABLE invoice
DROP CONSTRAINT IF EXISTS fk_invoice_dispatch_batch;

-- Drop the old dispatch_batch_id column (now using junction table)
ALTER TABLE invoice
DROP COLUMN IF EXISTS dispatch_batch_id;

COMMENT ON TABLE invoice IS 'Invoice table now uses invoice_dispatch_batch junction table for many-to-many relationship with dispatch_batch';

-- ===================================
-- Part 6: DeliveryChallan-DispatchBatch Junction Table
-- ===================================

-- Create sequence for delivery_challan_dispatch_batch
CREATE SEQUENCE IF NOT EXISTS delivery_challan_dispatch_batch_sequence START WITH 1 INCREMENT BY 1;

-- Create delivery_challan_dispatch_batch junction table
CREATE TABLE IF NOT EXISTS delivery_challan_dispatch_batch (
  id BIGINT PRIMARY KEY DEFAULT nextval('delivery_challan_dispatch_batch_sequence'),
  delivery_challan_id BIGINT NOT NULL,
  dispatch_batch_id BIGINT NOT NULL,
  sequence_order INTEGER,
  tenant_id BIGINT NOT NULL,
  created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP WITHOUT TIME ZONE,
  deleted BOOLEAN DEFAULT FALSE,
  
  -- Foreign key constraints
  CONSTRAINT fk_delivery_challan_dispatch_batch_challan 
    FOREIGN KEY (delivery_challan_id) REFERENCES delivery_challan(id) ON DELETE CASCADE,
  
  CONSTRAINT fk_delivery_challan_dispatch_batch_dispatch 
    FOREIGN KEY (dispatch_batch_id) REFERENCES dispatch_batch(id) ON DELETE CASCADE,
  
  CONSTRAINT fk_delivery_challan_dispatch_batch_tenant 
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_delivery_challan_dispatch_challan 
  ON delivery_challan_dispatch_batch(delivery_challan_id) WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_delivery_challan_dispatch_batch 
  ON delivery_challan_dispatch_batch(dispatch_batch_id) WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_delivery_challan_dispatch_deleted 
  ON delivery_challan_dispatch_batch(deleted);

CREATE INDEX IF NOT EXISTS idx_delivery_challan_dispatch_tenant 
  ON delivery_challan_dispatch_batch(tenant_id) WHERE deleted = FALSE;

-- Unique constraint: prevent duplicate delivery challan-dispatch batch associations
CREATE UNIQUE INDEX IF NOT EXISTS idx_delivery_challan_dispatch_unique 
  ON delivery_challan_dispatch_batch(delivery_challan_id, dispatch_batch_id) 
  WHERE deleted = FALSE;

-- Comments for documentation
COMMENT ON TABLE delivery_challan_dispatch_batch IS 
  'Junction table for many-to-many relationship between delivery challans and dispatch batches. Allows one delivery challan to reference multiple dispatch batches.';

COMMENT ON COLUMN delivery_challan_dispatch_batch.sequence_order IS 
  'Display order of dispatch batches in the delivery challan listing.';

-- ===================================
-- Part 7: Remove Redundant dispatch_batch_id from DeliveryChallan
-- ===================================

-- Drop the old foreign key constraint
ALTER TABLE delivery_challan
DROP CONSTRAINT IF EXISTS fk_delivery_challan_dispatch_batch;

-- Drop the old dispatch_batch_id column (now using junction table)
ALTER TABLE delivery_challan
DROP COLUMN IF EXISTS dispatch_batch_id;

COMMENT ON TABLE delivery_challan IS 'Delivery challan table now uses delivery_challan_dispatch_batch junction table for many-to-many relationship with dispatch_batch';

-- ===================================
-- Part 8: Rename DispatchBatch Order Columns for Clarity
-- ===================================

-- Rename purchase_order_number to order_po_number
ALTER TABLE dispatch_batch
    RENAME COLUMN purchase_order_number TO order_po_number;

-- Rename purchase_order_date_time to order_date
ALTER TABLE dispatch_batch
    RENAME COLUMN purchase_order_date_time TO order_date;

-- Add comments for clarity
COMMENT ON COLUMN dispatch_batch.order_po_number IS 'Order Purchase Order Number - populated from associated Order or entered manually';
COMMENT ON COLUMN dispatch_batch.order_date IS 'Order Date - populated from associated Order or entered manually';

