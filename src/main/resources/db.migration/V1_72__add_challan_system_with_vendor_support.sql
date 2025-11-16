-- =====================================================
-- Complete Delivery Challan System with Buyer/Vendor Support
-- Consolidated Migration: V1_72, V1_73, V1_74
-- Description: Creates delivery challan tables with separate buyer/vendor references,
--              billing/shipping entities, vendor challan support, and value tracking
-- Date: 2025 (Development Phase - Consolidated)
-- =====================================================

-- =====================================================
-- 1. Add challan-related columns to dispatch_batch table
-- =====================================================

-- Add challan_number column to dispatch_batch table
ALTER TABLE dispatch_batch
ADD COLUMN challan_number VARCHAR(255);

-- Add challan_date_time column to dispatch_batch table  
ALTER TABLE dispatch_batch
ADD COLUMN challan_date_time TIMESTAMP WITHOUT TIME ZONE;

-- Add comments for clarity
COMMENT ON COLUMN dispatch_batch.challan_number IS 'Challan number when dispatch batch is associated with a delivery challan';
COMMENT ON COLUMN dispatch_batch.challan_date_time IS 'Challan date and time when dispatch batch is associated with a delivery challan';

-- =====================================================
-- 2. Create Delivery Challan Table
-- =====================================================

-- Create sequence for delivery_challan
CREATE SEQUENCE IF NOT EXISTS delivery_challan_sequence START 1 INCREMENT BY 1;

-- Create delivery_challan table
CREATE TABLE IF NOT EXISTS delivery_challan (
  -- Primary Key
  id BIGINT PRIMARY KEY DEFAULT nextval('delivery_challan_sequence'),
  
  -- Foreign Keys
  tenant_id BIGINT NOT NULL REFERENCES tenant(id),
  order_id BIGINT REFERENCES orders(id),
  
  -- Challan Details
  challan_number VARCHAR(255) NOT NULL,
  challan_date_time TIMESTAMP NOT NULL,
  
  -- Transportation Details
  transportation_reason TEXT NOT NULL,
  transportation_mode VARCHAR(20) DEFAULT 'ROAD',
  expected_delivery_date DATE,
  actual_delivery_date DATE,
  transporter_name VARCHAR(200),
  transporter_id VARCHAR(15),
  vehicle_number VARCHAR(20),
  transportation_distance INTEGER,
  
  -- Invoice Conversion
  converted_to_invoice_id BIGINT REFERENCES invoice(id),
  
  -- Legacy transporter reference (kept for backward compatibility if needed)
  transporter_id_legacy BIGINT REFERENCES transporter(id),
  eway_bill_number VARCHAR(50),
  work_type VARCHAR(100),
  
  -- Main Buyer/Vendor References (flexible - can use either or both)
  buyer_id BIGINT REFERENCES buyer(id),
  vendor_id BIGINT REFERENCES vendor(id),
  
  -- Billing and Shipping Entity References for Buyer
  buyer_billing_entity_id BIGINT REFERENCES buyer_entity(id),
  buyer_shipping_entity_id BIGINT REFERENCES buyer_entity(id),
  
  -- Billing and Shipping Entity References for Vendor
  vendor_billing_entity_id BIGINT REFERENCES vendor_entity(id),
  vendor_shipping_entity_id BIGINT REFERENCES vendor_entity(id),
  
  -- Vendor Challan Flag
  is_vendor_challan BOOLEAN DEFAULT FALSE NOT NULL,
  
  -- Challan Classification
  challan_type VARCHAR(50) NOT NULL,
  other_challan_type_details TEXT,
  
  -- Financial Details
  total_quantity DECIMAL(15,3) DEFAULT 0 CHECK (total_quantity >= 0),
  total_taxable_value DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (total_taxable_value >= 0),
  total_cgst_amount DECIMAL(15,2) DEFAULT 0,
  total_sgst_amount DECIMAL(15,2) DEFAULT 0,
  total_igst_amount DECIMAL(15,2) DEFAULT 0,
  total_value DECIMAL(15,2) NOT NULL DEFAULT 0,
  
  -- Estimated Value (for draft/planning)
  estimated_value DECIMAL(15,2) CHECK (estimated_value >= 0),
  
  -- Consignor Details (Persisted at generation)
  consignor_gstin VARCHAR(15),
  consignor_name TEXT,
  consignor_address TEXT,
  consignor_state_code VARCHAR(2),
  
  -- Additional Details
  terms_and_conditions TEXT,
  bank_name VARCHAR(255),
  account_number VARCHAR(50),
  ifsc_code VARCHAR(11),
  amount_in_words TEXT,
  
  -- Status and Lifecycle
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  
  -- Document References
  document_path VARCHAR(500),
  
  -- Cancellation Support
  cancelled_at TIMESTAMP,
  cancelled_by VARCHAR(100),
  cancellation_reason TEXT,
  
  -- Standard Audit Fields
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE,
  deleted_at TIMESTAMP,
  created_by BIGINT,
  updated_by BIGINT
);

-- =====================================================
-- 3. Create Indexes for Delivery Challan
-- =====================================================
CREATE UNIQUE INDEX uk_challan_number_tenant ON delivery_challan (challan_number, tenant_id) WHERE deleted = false;
CREATE INDEX idx_delivery_challan_tenant_status ON delivery_challan(tenant_id, status, deleted);
CREATE INDEX idx_delivery_challan_challan_date_time ON delivery_challan(challan_date_time);
CREATE INDEX idx_delivery_challan_buyer ON delivery_challan(buyer_id);
CREATE INDEX idx_delivery_challan_vendor ON delivery_challan(vendor_id);
CREATE INDEX idx_delivery_challan_billing_buyer_entity ON delivery_challan(buyer_billing_entity_id);
CREATE INDEX idx_delivery_challan_shipping_buyer_entity ON delivery_challan(buyer_shipping_entity_id);
CREATE INDEX idx_delivery_challan_billing_vendor_entity ON delivery_challan(vendor_billing_entity_id);
CREATE INDEX idx_delivery_challan_shipping_vendor_entity ON delivery_challan(vendor_shipping_entity_id);
CREATE INDEX idx_delivery_challan_deleted ON delivery_challan(deleted);
CREATE INDEX idx_delivery_challan_vehicle ON delivery_challan(vehicle_number);
CREATE INDEX idx_delivery_challan_transporter_legacy ON delivery_challan(transporter_id_legacy);
CREATE INDEX idx_delivery_challan_order ON delivery_challan(order_id);
CREATE INDEX idx_delivery_challan_work_type ON delivery_challan(work_type) WHERE deleted = false;
CREATE INDEX idx_delivery_challan_is_vendor_challan ON delivery_challan(is_vendor_challan);
CREATE INDEX idx_delivery_challan_tenant_is_vendor ON delivery_challan(tenant_id, is_vendor_challan, deleted);
CREATE INDEX idx_delivery_challan_estimated_value ON delivery_challan(estimated_value) WHERE deleted = false;
CREATE INDEX idx_delivery_challan_total_quantity ON delivery_challan(total_quantity) WHERE deleted = false AND total_quantity > 0;
CREATE INDEX idx_delivery_challan_cancelled_at ON delivery_challan(cancelled_at) WHERE cancelled_at IS NOT NULL;
CREATE INDEX idx_delivery_challan_cancellation ON delivery_challan(tenant_id, cancelled_at, deleted);
CREATE INDEX idx_delivery_challan_converted_to_invoice ON delivery_challan(converted_to_invoice_id);
CREATE INDEX idx_delivery_challan_expected_delivery ON delivery_challan(expected_delivery_date) WHERE deleted = false;
CREATE INDEX idx_delivery_challan_actual_delivery ON delivery_challan(actual_delivery_date) WHERE deleted = false;
CREATE INDEX idx_delivery_challan_transportation_mode ON delivery_challan(transportation_mode);
CREATE INDEX idx_delivery_challan_document_path ON delivery_challan(document_path) WHERE deleted = false AND document_path IS NOT NULL;

-- =====================================================
-- 4. Add Comments to Delivery Challan Table
-- =====================================================
COMMENT ON TABLE delivery_challan IS 'Delivery challan for goods movement with GST compliance (job work, branch transfer, samples, etc.)';
COMMENT ON COLUMN delivery_challan.challan_number IS 'Unique challan number (e.g., CHN/2025-26/001)';
COMMENT ON COLUMN delivery_challan.challan_type IS 'Type of challan - determines GST applicability';
COMMENT ON COLUMN delivery_challan.other_challan_type_details IS 'Details provided when challan type is OTHER';
COMMENT ON COLUMN delivery_challan.order_id IS 'Reference to the order for traceability';
COMMENT ON COLUMN delivery_challan.buyer_id IS 'Reference to main buyer (for buyer challans)';
COMMENT ON COLUMN delivery_challan.vendor_id IS 'Reference to main vendor (for vendor challans)';
COMMENT ON COLUMN delivery_challan.buyer_billing_entity_id IS 'Reference to BuyerEntity used as billing entity';
COMMENT ON COLUMN delivery_challan.buyer_shipping_entity_id IS 'Reference to BuyerEntity used as shipping entity';
COMMENT ON COLUMN delivery_challan.vendor_billing_entity_id IS 'Reference to VendorEntity used as billing entity for vendor challans';
COMMENT ON COLUMN delivery_challan.vendor_shipping_entity_id IS 'Reference to VendorEntity used as shipping entity for vendor challans';
COMMENT ON COLUMN delivery_challan.is_vendor_challan IS 'Flag to distinguish vendor challans (true - uses TenantVendorChallanSettings) from dispatch batch challans (false - uses TenantChallanSettings)';
COMMENT ON COLUMN delivery_challan.transportation_reason IS 'Reason for transportation (mandatory for GST compliance)';
COMMENT ON COLUMN delivery_challan.transportation_mode IS 'Mode of transportation (ROAD, RAIL, AIR, SHIP)';
COMMENT ON COLUMN delivery_challan.expected_delivery_date IS 'Expected delivery date';
COMMENT ON COLUMN delivery_challan.actual_delivery_date IS 'Actual delivery date (when goods are delivered)';
COMMENT ON COLUMN delivery_challan.transporter_name IS 'Name of the transporter';
COMMENT ON COLUMN delivery_challan.transporter_id IS 'GSTIN of the transporter';
COMMENT ON COLUMN delivery_challan.vehicle_number IS 'Vehicle registration number';
COMMENT ON COLUMN delivery_challan.transportation_distance IS 'Distance in kilometers';
COMMENT ON COLUMN delivery_challan.converted_to_invoice_id IS 'Reference to invoice if challan was converted';
COMMENT ON COLUMN delivery_challan.total_quantity IS 'Total quantity of all items in the challan (sum of line item quantities)';
COMMENT ON COLUMN delivery_challan.total_taxable_value IS 'Total declared value of goods (required for all challans)';
COMMENT ON COLUMN delivery_challan.total_cgst_amount IS 'Total CGST amount';
COMMENT ON COLUMN delivery_challan.total_sgst_amount IS 'Total SGST amount';
COMMENT ON COLUMN delivery_challan.total_igst_amount IS 'Total IGST amount';
COMMENT ON COLUMN delivery_challan.estimated_value IS 'Estimated value for draft/planning (not used in final calculations)';
COMMENT ON COLUMN delivery_challan.status IS 'Lifecycle status of the challan';
COMMENT ON COLUMN delivery_challan.document_path IS 'Path to the generated PDF document or file storage location';
COMMENT ON COLUMN delivery_challan.consignor_gstin IS 'Consignor GSTIN persisted at challan generation';
COMMENT ON COLUMN delivery_challan.consignor_name IS 'Consignor name persisted at challan generation';
COMMENT ON COLUMN delivery_challan.consignor_address IS 'Consignor address persisted at challan generation';
COMMENT ON COLUMN delivery_challan.consignor_state_code IS 'Consignor state code persisted at challan generation';
COMMENT ON COLUMN delivery_challan.terms_and_conditions IS 'Terms and conditions text';
COMMENT ON COLUMN delivery_challan.bank_name IS 'Bank name for reference';
COMMENT ON COLUMN delivery_challan.account_number IS 'Account number for reference';
COMMENT ON COLUMN delivery_challan.ifsc_code IS 'IFSC code for reference';
COMMENT ON COLUMN delivery_challan.amount_in_words IS 'Total amount in words for display';
COMMENT ON COLUMN delivery_challan.cancelled_at IS 'Timestamp when challan was cancelled';
COMMENT ON COLUMN delivery_challan.cancelled_by IS 'Username or identifier of person who cancelled the challan';
COMMENT ON COLUMN delivery_challan.cancellation_reason IS 'Reason for challan cancellation';
COMMENT ON COLUMN delivery_challan.work_type IS 'Work type (JOB_WORK_ONLY or WITH_MATERIAL) - determines which HSN/SAC and tax rates to use';

-- =====================================================
-- 5. Create Challan Line Item Table (for Buyer Challans)
-- =====================================================
CREATE SEQUENCE IF NOT EXISTS challan_line_item_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS challan_line_item (
  -- Primary Key
  id BIGINT PRIMARY KEY DEFAULT nextval('challan_line_item_sequence'),
  
  -- Foreign Keys
  delivery_challan_id BIGINT NOT NULL REFERENCES delivery_challan(id) ON DELETE CASCADE,
  tenant_id BIGINT NOT NULL REFERENCES tenant(id),
  
  -- Line Item Details
  line_number INTEGER NOT NULL,
  item_name TEXT NOT NULL,
  hsn_code VARCHAR(10) NOT NULL,
  work_type VARCHAR(100),
  
  -- Quantity and Units
  quantity DECIMAL(15,3) NOT NULL CHECK (quantity > 0),
  unit_of_measurement VARCHAR(10) NOT NULL,
  
  -- Pricing
  rate_per_unit DECIMAL(15,2),
  taxable_value DECIMAL(15,2) NOT NULL CHECK (taxable_value >= 0),
  
  -- Tax Details
  cgst_rate DECIMAL(5,2) DEFAULT 0,
  sgst_rate DECIMAL(5,2) DEFAULT 0,
  igst_rate DECIMAL(5,2) DEFAULT 0,
  cgst_amount DECIMAL(15,2) DEFAULT 0,
  sgst_amount DECIMAL(15,2) DEFAULT 0,
  igst_amount DECIMAL(15,2) DEFAULT 0,
  
  -- Total
  total_value DECIMAL(15,2) NOT NULL,
  
  -- Additional Details
  remarks TEXT,
  
  -- Traceability - Link back to source data
  item_workflow_id BIGINT,
  processed_item_dispatch_batch_id BIGINT,
  
  -- Standard Audit Fields
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE,
  deleted_at TIMESTAMP,
  
  -- Constraints
  CONSTRAINT uq_challan_line_number UNIQUE (delivery_challan_id, line_number),
  CONSTRAINT chk_challan_line_quantity_positive CHECK (quantity > 0),
  CONSTRAINT chk_challan_line_taxable_value_positive CHECK (taxable_value >= 0)
);

-- Create indexes for performance
CREATE INDEX idx_challan_line_item_challan ON challan_line_item(delivery_challan_id);
CREATE INDEX idx_challan_line_item_hsn ON challan_line_item(hsn_code);
CREATE INDEX idx_challan_line_item_tenant ON challan_line_item(tenant_id);
CREATE INDEX idx_challan_line_item_deleted ON challan_line_item(deleted);
CREATE INDEX idx_challan_line_item_workflow ON challan_line_item(item_workflow_id);
CREATE INDEX idx_challan_line_item_processed_item ON challan_line_item(processed_item_dispatch_batch_id);

-- Add comments for documentation
COMMENT ON TABLE challan_line_item IS 'Individual line items for delivery challans with dispatch batches (buyer challans)';
COMMENT ON COLUMN challan_line_item.line_number IS 'Sequential line number within the challan';
COMMENT ON COLUMN challan_line_item.item_name IS 'Name/description of the item';
COMMENT ON COLUMN challan_line_item.hsn_code IS 'HSN/SAC code for GST classification';
COMMENT ON COLUMN challan_line_item.taxable_value IS 'Taxable value before tax application';
COMMENT ON COLUMN challan_line_item.work_type IS 'Work type (JOB_WORK_ONLY or WITH_MATERIAL) for this line item - determines HSN/SAC and tax rates used';
COMMENT ON COLUMN challan_line_item.item_workflow_id IS 'Reference to ItemWorkflow for traceability and heat number retrieval';
COMMENT ON COLUMN challan_line_item.processed_item_dispatch_batch_id IS 'Reference to ProcessedItemDispatchBatch for traceability';

-- =====================================================
-- 6. Create Challan Dispatch Batch Junction Table
-- =====================================================
CREATE SEQUENCE IF NOT EXISTS challan_dispatch_batch_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS challan_dispatch_batch (
  -- Primary Key
  id BIGINT PRIMARY KEY DEFAULT nextval('challan_dispatch_batch_sequence'),
  
  -- Foreign Keys
  delivery_challan_id BIGINT NOT NULL REFERENCES delivery_challan(id) ON DELETE CASCADE,
  dispatch_batch_id BIGINT NOT NULL REFERENCES dispatch_batch(id) ON DELETE CASCADE,
  tenant_id BIGINT NOT NULL REFERENCES tenant(id),
  
  -- Standard Audit Fields
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE,
  
  -- Constraints
  CONSTRAINT uq_challan_dispatch_batch UNIQUE (delivery_challan_id, dispatch_batch_id)
);

-- Create indexes for performance
CREATE INDEX idx_challan_dispatch_batch_challan ON challan_dispatch_batch(delivery_challan_id);
CREATE INDEX idx_challan_dispatch_batch_dispatch ON challan_dispatch_batch(dispatch_batch_id);
CREATE INDEX idx_challan_dispatch_batch_tenant ON challan_dispatch_batch(tenant_id);
CREATE INDEX idx_challan_dispatch_batch_deleted ON challan_dispatch_batch(deleted);

-- Add comments for documentation
COMMENT ON TABLE challan_dispatch_batch IS 'Junction table for many-to-many relationship between challans and dispatch batches';
COMMENT ON COLUMN challan_dispatch_batch.delivery_challan_id IS 'Reference to the delivery challan';
COMMENT ON COLUMN challan_dispatch_batch.dispatch_batch_id IS 'Reference to the dispatch batch';

-- =====================================================
-- 7. Add challan related fields to vendor_dispatch_batch table
-- =====================================================

-- Add challan_number column
ALTER TABLE vendor_dispatch_batch 
ADD COLUMN challan_number VARCHAR(255);

-- Add challan_date_time column
ALTER TABLE vendor_dispatch_batch 
ADD COLUMN challan_date_time TIMESTAMP WITHOUT TIME ZONE;

-- Add delivery_challan_id column with foreign key reference
ALTER TABLE vendor_dispatch_batch 
ADD COLUMN delivery_challan_id BIGINT;

-- Add foreign key constraint for delivery_challan_id
ALTER TABLE vendor_dispatch_batch 
ADD CONSTRAINT fk_vendor_dispatch_batch_delivery_challan 
FOREIGN KEY (delivery_challan_id) 
REFERENCES delivery_challan (id);

-- Create index on delivery_challan_id for better query performance
CREATE INDEX idx_vendor_dispatch_batch_delivery_challan 
ON vendor_dispatch_batch (delivery_challan_id);

-- Create index on challan_number for better query performance
CREATE INDEX idx_vendor_dispatch_batch_challan_number 
ON vendor_dispatch_batch (challan_number);

-- =====================================================
-- 8. Create Challan Vendor Dispatch Batch Junction Table
-- =====================================================

CREATE SEQUENCE IF NOT EXISTS challan_vendor_dispatch_batch_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS challan_vendor_dispatch_batch (
  -- Primary Key
  id BIGINT PRIMARY KEY DEFAULT nextval('challan_vendor_dispatch_batch_sequence'),
  
  -- Foreign Keys
  delivery_challan_id BIGINT NOT NULL REFERENCES delivery_challan(id) ON DELETE CASCADE,
  vendor_dispatch_batch_id BIGINT NOT NULL REFERENCES vendor_dispatch_batch(id) ON DELETE CASCADE,
  tenant_id BIGINT NOT NULL REFERENCES tenant(id),
  
  -- Standard Audit Fields
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE,
  
  -- Constraints
  CONSTRAINT uq_challan_vendor_dispatch_batch UNIQUE (delivery_challan_id, vendor_dispatch_batch_id)
);

-- Create indexes for performance
CREATE INDEX idx_challan_vendor_dispatch_batch_challan ON challan_vendor_dispatch_batch(delivery_challan_id);
CREATE INDEX idx_challan_vendor_dispatch_batch_vendor_dispatch ON challan_vendor_dispatch_batch(vendor_dispatch_batch_id);
CREATE INDEX idx_challan_vendor_dispatch_batch_tenant ON challan_vendor_dispatch_batch(tenant_id);
CREATE INDEX idx_challan_vendor_dispatch_batch_deleted ON challan_vendor_dispatch_batch(deleted);

-- Add comments for documentation
COMMENT ON TABLE challan_vendor_dispatch_batch IS 'Junction table for many-to-many relationship between challans and vendor dispatch batches';
COMMENT ON COLUMN challan_vendor_dispatch_batch.delivery_challan_id IS 'Reference to the delivery challan';
COMMENT ON COLUMN challan_vendor_dispatch_batch.vendor_dispatch_batch_id IS 'Reference to the vendor dispatch batch';

-- =====================================================
-- 9. Create Challan Vendor Line Item Table
-- =====================================================

CREATE SEQUENCE IF NOT EXISTS challan_vendor_line_item_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS challan_vendor_line_item (
  -- Primary Key
  id BIGINT PRIMARY KEY DEFAULT nextval('challan_vendor_line_item_sequence'),
  
  -- Foreign Keys
  delivery_challan_id BIGINT NOT NULL REFERENCES delivery_challan(id) ON DELETE CASCADE,
  vendor_dispatch_batch_id BIGINT REFERENCES vendor_dispatch_batch(id),
  tenant_id BIGINT NOT NULL REFERENCES tenant(id),
  
  -- Line Item Details
  line_number INTEGER NOT NULL,
  item_name TEXT NOT NULL,
  hsn_code VARCHAR(10) NOT NULL,
  work_type VARCHAR(100),
  
  -- Quantity and Units
  quantity DECIMAL(15,3) NOT NULL CHECK (quantity > 0),
  unit_of_measurement VARCHAR(10) NOT NULL,
  
  -- Pricing
  rate_per_unit DECIMAL(15,2),
  taxable_value DECIMAL(15,2) NOT NULL CHECK (taxable_value >= 0),
  
  -- Tax Details
  cgst_rate DECIMAL(5,2) DEFAULT 0,
  sgst_rate DECIMAL(5,2) DEFAULT 0,
  igst_rate DECIMAL(5,2) DEFAULT 0,
  cgst_amount DECIMAL(15,2) DEFAULT 0,
  sgst_amount DECIMAL(15,2) DEFAULT 0,
  igst_amount DECIMAL(15,2) DEFAULT 0,
  
  -- Total
  total_value DECIMAL(15,2) NOT NULL,
  
  -- Additional Details
  remarks TEXT,

  -- Traceability - Link back to source data
  item_workflow_id BIGINT,
  processed_item_vendor_dispatch_batch_id BIGINT,
  
  -- Standard Audit Fields
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE,
  deleted_at TIMESTAMP,
  
  -- Constraints
  CONSTRAINT uq_challan_vendor_line_number UNIQUE (delivery_challan_id, line_number),
  CONSTRAINT chk_challan_vendor_line_quantity_positive CHECK (quantity > 0),
  CONSTRAINT chk_challan_vendor_line_taxable_value_positive CHECK (taxable_value >= 0)
);

-- Create indexes for performance
CREATE INDEX idx_challan_vendor_line_item_challan ON challan_vendor_line_item(delivery_challan_id);
CREATE INDEX idx_challan_vendor_line_item_vendor_dispatch_batch ON challan_vendor_line_item(vendor_dispatch_batch_id);
CREATE INDEX idx_challan_vendor_line_item_hsn ON challan_vendor_line_item(hsn_code);
CREATE INDEX idx_challan_vendor_line_item_tenant ON challan_vendor_line_item(tenant_id);
CREATE INDEX idx_challan_vendor_line_item_deleted ON challan_vendor_line_item(deleted);
CREATE INDEX idx_challan_vendor_line_item_workflow ON challan_vendor_line_item(item_workflow_id);
CREATE INDEX idx_challan_vendor_line_processed_item ON challan_vendor_line_item(processed_item_vendor_dispatch_batch_id);

-- Add comments for documentation
COMMENT ON TABLE challan_vendor_line_item IS 'Individual line items for delivery challans with vendor dispatch batches';
COMMENT ON COLUMN challan_vendor_line_item.line_number IS 'Sequential line number within the challan';
COMMENT ON COLUMN challan_vendor_line_item.item_name IS 'Name/description of the item';
COMMENT ON COLUMN challan_vendor_line_item.hsn_code IS 'HSN/SAC code for GST classification';
COMMENT ON COLUMN challan_vendor_line_item.taxable_value IS 'Taxable value before tax application';
COMMENT ON COLUMN challan_vendor_line_item.vendor_dispatch_batch_id IS 'Reference to VendorDispatchBatch for traceability';
COMMENT ON COLUMN challan_vendor_line_item.item_workflow_id IS 'Reference to ItemWorkflow for traceability and heat number retrieval';
COMMENT ON COLUMN challan_vendor_line_item.processed_item_vendor_dispatch_batch_id IS 'Reference to ProcessedItemVendorDispatchBatch for traceability';

-- =====================================================
-- 10. Create tenant_vendor_challan_settings table
-- =====================================================

CREATE SEQUENCE IF NOT EXISTS vendor_challan_settings_sequence START 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS tenant_vendor_challan_settings (
  -- Primary Key
  id BIGINT PRIMARY KEY DEFAULT nextval('vendor_challan_settings_sequence'),
  
  -- Foreign Key to tenant
  tenant_id BIGINT NOT NULL REFERENCES tenant(id),
  
  -- Vendor Challan Number Configuration
  challan_prefix VARCHAR(10) DEFAULT 'VCH',
  start_from INTEGER DEFAULT 1 CHECK (start_from >= 1),
  current_sequence INTEGER DEFAULT 1 CHECK (current_sequence >= 1 AND current_sequence <= 999999),
  series_format VARCHAR(20) DEFAULT '2025-26',
  
  -- Status
  is_active BOOLEAN DEFAULT TRUE,
  
  -- Standard Audit Fields
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE,
  
  -- Constraints
  CONSTRAINT chk_vendor_challan_prefix CHECK (challan_prefix ~ '^[A-Z0-9]*$'),
  CONSTRAINT chk_vendor_challan_start_from CHECK (start_from >= 1),
  CONSTRAINT chk_vendor_challan_current_sequence CHECK (current_sequence >= 1 AND current_sequence <= 999999),
  CONSTRAINT uq_vendor_challan_settings_tenant_deleted UNIQUE (tenant_id, deleted)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_vendor_challan_settings_tenant ON tenant_vendor_challan_settings (tenant_id);
CREATE INDEX IF NOT EXISTS idx_vendor_challan_settings_active ON tenant_vendor_challan_settings (tenant_id, is_active, deleted);

-- Add comments for documentation
COMMENT ON TABLE tenant_vendor_challan_settings IS 'Vendor-specific challan settings for tenants';
COMMENT ON COLUMN tenant_vendor_challan_settings.tenant_id IS 'Reference to the tenant';
COMMENT ON COLUMN tenant_vendor_challan_settings.challan_prefix IS 'Prefix for vendor challan numbers';
COMMENT ON COLUMN tenant_vendor_challan_settings.start_from IS 'Starting sequence number for vendor challans';
COMMENT ON COLUMN tenant_vendor_challan_settings.current_sequence IS 'Current sequence number for vendor challans';
COMMENT ON COLUMN tenant_vendor_challan_settings.series_format IS 'Series format for vendor challan numbers';
COMMENT ON COLUMN tenant_vendor_challan_settings.is_active IS 'Whether these settings are active';

-- =====================================================
-- 11. Add Challan Prefix to Tenant Challan Settings
-- =====================================================
ALTER TABLE tenant_challan_settings
ADD COLUMN IF NOT EXISTS challan_prefix VARCHAR(10) DEFAULT 'CHN';

-- Add column comment
COMMENT ON COLUMN tenant_challan_settings.challan_prefix IS 'Prefix for challan numbers (e.g., CHN, DC)';

-- Update existing records to have 'CHN' as prefix if null
UPDATE tenant_challan_settings
SET challan_prefix = 'CHN'
WHERE challan_prefix IS NULL;

-- =====================================================
-- 12. Verification
-- =====================================================

-- Verify delivery_challan table creation
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables 
             WHERE table_name = 'delivery_challan') THEN
    RAISE NOTICE '✓ Table delivery_challan created successfully';
  ELSE
    RAISE EXCEPTION 'ERROR: Table delivery_challan was not created';
  END IF;
END $$;

-- Verify challan_line_item table creation
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables 
             WHERE table_name = 'challan_line_item') THEN
    RAISE NOTICE '✓ Table challan_line_item created successfully';
  ELSE
    RAISE EXCEPTION 'ERROR: Table challan_line_item was not created';
  END IF;
END $$;

-- Verify challan_dispatch_batch junction table creation
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables 
             WHERE table_name = 'challan_dispatch_batch') THEN
    RAISE NOTICE '✓ Table challan_dispatch_batch created successfully';
  ELSE
    RAISE EXCEPTION 'ERROR: Table challan_dispatch_batch was not created';
  END IF;
END $$;

-- Verify vendor challan tables
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables 
             WHERE table_name IN ('challan_vendor_dispatch_batch', 'challan_vendor_line_item', 'tenant_vendor_challan_settings')) THEN
    RAISE NOTICE '✓ Vendor challan tables created successfully';
  ELSE
    RAISE EXCEPTION 'ERROR: Vendor challan tables not created';
  END IF;
END $$;

-- Verify critical columns in delivery_challan
DO $$
DECLARE
  buyer_col BOOLEAN;
  vendor_col BOOLEAN;
  billing_buyer BOOLEAN;
  shipping_buyer BOOLEAN;
  billing_vendor BOOLEAN;
  shipping_vendor BOOLEAN;
  is_vendor BOOLEAN;
  estimated BOOLEAN;
BEGIN
  SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'delivery_challan' AND column_name = 'buyer_id') INTO buyer_col;
  SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'delivery_challan' AND column_name = 'vendor_id') INTO vendor_col;
  SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'delivery_challan' AND column_name = 'buyer_billing_entity_id') INTO billing_buyer;
  SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'delivery_challan' AND column_name = 'buyer_shipping_entity_id') INTO shipping_buyer;
  SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'delivery_challan' AND column_name = 'vendor_billing_entity_id') INTO billing_vendor;
  SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'delivery_challan' AND column_name = 'vendor_shipping_entity_id') INTO shipping_vendor;
  SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'delivery_challan' AND column_name = 'is_vendor_challan') INTO is_vendor;
  SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'delivery_challan' AND column_name = 'estimated_value') INTO estimated;
  
  IF buyer_col AND vendor_col AND billing_buyer AND shipping_buyer AND billing_vendor AND shipping_vendor AND is_vendor AND estimated THEN
    RAISE NOTICE '✓ All new buyer/vendor columns exist in delivery_challan';
  ELSE
    RAISE EXCEPTION 'ERROR: Missing columns - buyer:%, vendor:%, billingBuyer:%, shippingBuyer:%, billingVendor:%, shippingVendor:%, isVendor:%, estimated:%',
                    buyer_col, vendor_col, billing_buyer, shipping_buyer, billing_vendor, shipping_vendor, is_vendor, estimated;
  END IF;
END $$;

-- Verify sequences created
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.sequences 
             WHERE sequence_name IN ('delivery_challan_sequence', 'challan_line_item_sequence', 
                                     'challan_dispatch_batch_sequence', 'challan_vendor_dispatch_batch_sequence',
                                     'challan_vendor_line_item_sequence', 'vendor_challan_settings_sequence')) THEN
    RAISE NOTICE '✓ All sequences created successfully';
  ELSE
    RAISE EXCEPTION 'ERROR: One or more sequences missing';
  END IF;
END $$;

-- Verify dispatch_batch challan fields
DO $$
DECLARE
  challan_number_exists BOOLEAN;
  challan_date_time_exists BOOLEAN;
BEGIN
  SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'dispatch_batch' AND column_name = 'challan_number') INTO challan_number_exists;
  SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'dispatch_batch' AND column_name = 'challan_date_time') INTO challan_date_time_exists;
  
  IF challan_number_exists AND challan_date_time_exists THEN
    RAISE NOTICE '✓ dispatch_batch.challan_number and challan_date_time columns added';
  ELSE
    RAISE EXCEPTION 'ERROR: Dispatch batch challan fields missing - challan_number:%, challan_date_time:%',
                    challan_number_exists, challan_date_time_exists;
  END IF;
END $$;

-- Verify vendor_dispatch_batch challan fields
DO $$
DECLARE
  challan_number_exists BOOLEAN;
  challan_date_time_exists BOOLEAN;
  delivery_challan_id_exists BOOLEAN;
BEGIN
  SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'vendor_dispatch_batch' AND column_name = 'challan_number') INTO challan_number_exists;
  SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'vendor_dispatch_batch' AND column_name = 'challan_date_time') INTO challan_date_time_exists;
  SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'vendor_dispatch_batch' AND column_name = 'delivery_challan_id') INTO delivery_challan_id_exists;
  
  IF challan_number_exists AND challan_date_time_exists AND delivery_challan_id_exists THEN
    RAISE NOTICE '✓ vendor_dispatch_batch challan fields added';
  ELSE
    RAISE EXCEPTION 'ERROR: Vendor dispatch batch challan fields missing';
  END IF;
END $$;

-- =====================================================
-- 13. Final Success Message
-- =====================================================
DO $$
BEGIN
  RAISE NOTICE '========================================';
  RAISE NOTICE 'V1_72 CONSOLIDATED Migration completed successfully!';
  RAISE NOTICE 'Created: delivery_challan with buyer/vendor/entity structure';
  RAISE NOTICE 'Created: challan_line_item (for buyer challans)';
  RAISE NOTICE 'Created: challan_vendor_line_item (for vendor challans)';
  RAISE NOTICE 'Created: challan_dispatch_batch junction table';
  RAISE NOTICE 'Created: challan_vendor_dispatch_batch junction table';
  RAISE NOTICE 'Created: tenant_vendor_challan_settings';
  RAISE NOTICE 'Added: challan fields to dispatch_batch table';
  RAISE NOTICE 'Added: challan fields to vendor_dispatch_batch table';
  RAISE NOTICE 'Added: estimated_value and cancellation support';
  RAISE NOTICE 'Added: is_vendor_challan flag';
  RAISE NOTICE 'Pattern: Separate buyer/vendor with billing/shipping entities';
  RAISE NOTICE 'Challan Management System is ready!';
  RAISE NOTICE '========================================';
END $$;
