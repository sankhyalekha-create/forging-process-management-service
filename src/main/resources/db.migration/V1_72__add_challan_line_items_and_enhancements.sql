-- Migration V1_72: Complete Delivery Challan System with Line Items and GST Support
-- Description: Create delivery_challan, challan_line_item, and challan_dispatch_batch tables
-- Date: November 4, 2025

-- =====================================================
-- 1. Create Sequences
-- =====================================================
CREATE SEQUENCE IF NOT EXISTS delivery_challan_sequence START 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS challan_line_item_sequence START 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS challan_dispatch_batch_sequence START 1 INCREMENT BY 1;

-- =====================================================
-- 2. Create Delivery Challan Table (Main Table)
-- =====================================================
CREATE TABLE IF NOT EXISTS delivery_challan (
  -- Primary Key
  id BIGINT PRIMARY KEY DEFAULT nextval('delivery_challan_sequence'),
  
  -- Challan Identification
  challan_number VARCHAR(50) NOT NULL,
  challan_date_time TIMESTAMP NOT NULL,
  challan_type VARCHAR(50) NOT NULL,
  other_challan_type_details VARCHAR(500),
  
  -- References
  order_id BIGINT,
  converted_to_invoice_id BIGINT,
  
  -- Consignee Details (Either buyer or vendor entity)
  consignee_buyer_entity_id BIGINT REFERENCES buyer_entity(id),
  consignee_vendor_entity_id BIGINT REFERENCES vendor_entity(id),
  
  -- Transportation Details
  transportation_reason TEXT NOT NULL,
  transportation_mode VARCHAR(20) DEFAULT 'ROAD',
  expected_delivery_date DATE,
  actual_delivery_date DATE,
  
  -- Extended Transportation Details
  transporter_name VARCHAR(200),
  transporter_id VARCHAR(15),
  vehicle_number VARCHAR(20),
  transportation_distance INTEGER,
  
  -- Financial Summary
  total_quantity DECIMAL(15,3) DEFAULT 0,
  total_taxable_value DECIMAL(15,2) NOT NULL DEFAULT 0,
  total_cgst_amount DECIMAL(15,2) DEFAULT 0,
  total_sgst_amount DECIMAL(15,2) DEFAULT 0,
  total_igst_amount DECIMAL(15,2) DEFAULT 0,
  total_value DECIMAL(15,2) DEFAULT 0,
  
  -- Status and Workflow
  status VARCHAR(20) DEFAULT 'DRAFT',
  
  -- Document References
  document_path VARCHAR(500),
  
  -- Consignor/Supplier Details (Persisted at generation for data integrity)
  consignor_gstin VARCHAR(15),
  consignor_name VARCHAR(200),
  consignor_address VARCHAR(500),
  consignor_state_code VARCHAR(2),
  
  -- Display Fields
  terms_and_conditions VARCHAR(2000),
  bank_name VARCHAR(100),
  account_number VARCHAR(20),
  ifsc_code VARCHAR(11),
  amount_in_words VARCHAR(500),
  
  -- Standard Audit Fields
  tenant_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP,
  deleted BOOLEAN DEFAULT FALSE,
  
  -- Foreign Key Constraints
  CONSTRAINT fk_delivery_challan_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
  
  -- Business Constraints
  CONSTRAINT chk_challan_single_consignee CHECK (
    (consignee_buyer_entity_id IS NOT NULL AND consignee_vendor_entity_id IS NULL) OR
    (consignee_buyer_entity_id IS NULL AND consignee_vendor_entity_id IS NOT NULL)
  ),
  CONSTRAINT chk_challan_type CHECK (challan_type IN ('JOB_WORK', 'BRANCH_TRANSFER', 'SAMPLE_DISPATCH', 'RETURN_GOODS', 'OTHER')),
  CONSTRAINT chk_challan_status CHECK (status IN ('DRAFT', 'GENERATED', 'DISPATCHED', 'DELIVERED', 'CONVERTED_TO_INVOICE', 'CANCELLED')),
  CONSTRAINT chk_challan_transportation_mode CHECK (transportation_mode IN ('ROAD', 'RAIL', 'AIR', 'SHIP'))
);

-- =====================================================
-- 3. Create Indexes for Delivery Challan
-- =====================================================
CREATE INDEX idx_delivery_challan_tenant_status ON delivery_challan(tenant_id, status);
CREATE INDEX idx_delivery_challan_challan_date_time ON delivery_challan(challan_date_time);
CREATE INDEX idx_delivery_challan_consignee_buyer ON delivery_challan(consignee_buyer_entity_id);
CREATE INDEX idx_delivery_challan_consignee_vendor ON delivery_challan(consignee_vendor_entity_id);
CREATE INDEX idx_delivery_challan_deleted ON delivery_challan(deleted);
CREATE INDEX idx_delivery_challan_vehicle ON delivery_challan(vehicle_number);
CREATE INDEX idx_delivery_challan_transporter ON delivery_challan(transporter_id);
CREATE INDEX idx_delivery_challan_order ON delivery_challan(order_id);

-- =====================================================
-- 4. Add Comments to Delivery Challan Table
-- =====================================================
COMMENT ON TABLE delivery_challan IS 'Delivery challan for goods movement with GST compliance (job work, branch transfer, samples, etc.)';
COMMENT ON COLUMN delivery_challan.challan_number IS 'Unique challan number (e.g., CHN/2025-26/001)';
COMMENT ON COLUMN delivery_challan.challan_type IS 'Type of challan - determines GST applicability';
COMMENT ON COLUMN delivery_challan.other_challan_type_details IS 'Details provided when challan type is OTHER';
COMMENT ON COLUMN delivery_challan.order_id IS 'Reference to the order for traceability';
COMMENT ON COLUMN delivery_challan.total_taxable_value IS 'Total declared value of goods (required for all challans)';
COMMENT ON COLUMN delivery_challan.total_cgst_amount IS 'Total CGST amount';
COMMENT ON COLUMN delivery_challan.total_sgst_amount IS 'Total SGST amount';
COMMENT ON COLUMN delivery_challan.total_igst_amount IS 'Total IGST amount';
COMMENT ON COLUMN delivery_challan.status IS 'Lifecycle status of the challan';
COMMENT ON COLUMN delivery_challan.consignor_gstin IS 'Consignor GSTIN persisted at challan generation';
COMMENT ON COLUMN delivery_challan.consignor_name IS 'Consignor name persisted at challan generation';
COMMENT ON COLUMN delivery_challan.consignor_address IS 'Consignor address persisted at challan generation';
COMMENT ON COLUMN delivery_challan.consignor_state_code IS 'Consignor state code persisted at challan generation';
COMMENT ON COLUMN delivery_challan.terms_and_conditions IS 'Terms and conditions text';
COMMENT ON COLUMN delivery_challan.bank_name IS 'Bank name for reference';
COMMENT ON COLUMN delivery_challan.account_number IS 'Account number for reference';
COMMENT ON COLUMN delivery_challan.ifsc_code IS 'IFSC code for reference';
COMMENT ON COLUMN delivery_challan.amount_in_words IS 'Total amount in words for display';

-- =====================================================
-- 5. Create Challan Line Item Table
-- =====================================================
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
  
  -- Constraints
  CONSTRAINT uq_challan_line_number UNIQUE (delivery_challan_id, line_number),
  CONSTRAINT chk_challan_line_quantity_positive CHECK (quantity > 0),
  CONSTRAINT chk_challan_line_taxable_value_positive CHECK (taxable_value >= 0)
);

-- =====================================================
-- 6. Create Indexes for Challan Line Item
-- =====================================================
CREATE INDEX idx_challan_line_item_challan ON challan_line_item(delivery_challan_id);
CREATE INDEX idx_challan_line_item_hsn ON challan_line_item(hsn_code);
CREATE INDEX idx_challan_line_item_tenant ON challan_line_item(tenant_id);
CREATE INDEX idx_challan_line_item_deleted ON challan_line_item(deleted);
CREATE INDEX idx_challan_line_item_workflow ON challan_line_item(item_workflow_id);
CREATE INDEX idx_challan_line_item_processed_item ON challan_line_item(processed_item_dispatch_batch_id);

-- =====================================================
-- 7. Add Comments to Challan Line Item Table
-- =====================================================
COMMENT ON TABLE challan_line_item IS 'Individual line items for delivery challans with tax calculations';
COMMENT ON COLUMN challan_line_item.line_number IS 'Sequential line number within the challan';
COMMENT ON COLUMN challan_line_item.item_name IS 'Name/description of the item';
COMMENT ON COLUMN challan_line_item.hsn_code IS 'HSN/SAC code for GST classification';
COMMENT ON COLUMN challan_line_item.taxable_value IS 'Taxable value before tax application';
COMMENT ON COLUMN challan_line_item.item_workflow_id IS 'Reference to ItemWorkflow for traceability and heat number retrieval';
COMMENT ON COLUMN challan_line_item.processed_item_dispatch_batch_id IS 'Reference to ProcessedItemDispatchBatch for traceability';

-- =====================================================
-- 8. Create Challan Dispatch Batch Junction Table
-- =====================================================
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
  deleted BOOLEAN DEFAULT FALSE,
  
  -- Constraints
  CONSTRAINT uq_challan_dispatch_batch UNIQUE (delivery_challan_id, dispatch_batch_id)
);

-- =====================================================
-- 8A. Add WorkType Support to Delivery Challan System
-- =====================================================

-- Add work_type column to delivery_challan table
ALTER TABLE delivery_challan
ADD COLUMN IF NOT EXISTS work_type VARCHAR(20);

-- Add comment to document the column
COMMENT ON COLUMN delivery_challan.work_type IS 'Work type (JOB_WORK_ONLY or WITH_MATERIAL) - determines which HSN/SAC and tax rates to use';

-- Create index for work_type filtering
CREATE INDEX IF NOT EXISTS idx_delivery_challan_work_type ON delivery_challan(work_type) WHERE deleted = false;

-- Add work_type column to challan_line_item table
ALTER TABLE challan_line_item
ADD COLUMN IF NOT EXISTS work_type VARCHAR(100);

-- Add comment to document the column
COMMENT ON COLUMN challan_line_item.work_type IS 'Work type (JOB_WORK_ONLY or WITH_MATERIAL) for this line item - determines HSN/SAC and tax rates used';

-- =====================================================
-- 9. Create Indexes for Challan Dispatch Batch Junction Table
-- =====================================================
CREATE INDEX idx_challan_dispatch_batch_challan ON challan_dispatch_batch(delivery_challan_id);
CREATE INDEX idx_challan_dispatch_batch_dispatch ON challan_dispatch_batch(dispatch_batch_id);
CREATE INDEX idx_challan_dispatch_batch_tenant ON challan_dispatch_batch(tenant_id);
CREATE INDEX idx_challan_dispatch_batch_deleted ON challan_dispatch_batch(deleted);

-- =====================================================
-- 10. Add Comments to Challan Dispatch Batch Table
-- =====================================================
COMMENT ON TABLE challan_dispatch_batch IS 'Junction table for many-to-many relationship between challans and dispatch batches';
COMMENT ON COLUMN challan_dispatch_batch.delivery_challan_id IS 'Reference to the delivery challan';
COMMENT ON COLUMN challan_dispatch_batch.dispatch_batch_id IS 'Reference to the dispatch batch';

-- =====================================================
-- 11. Verification Queries (For Testing)
-- =====================================================
-- These are informational and will not affect the migration

-- Verify delivery_challan table creation
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables 
             WHERE table_name = 'delivery_challan') THEN
    RAISE NOTICE 'SUCCESS: Table delivery_challan created successfully';
  ELSE
    RAISE EXCEPTION 'ERROR: Table delivery_challan was not created';
  END IF;
END $$;

-- Verify challan_line_item table creation
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables 
             WHERE table_name = 'challan_line_item') THEN
    RAISE NOTICE 'SUCCESS: Table challan_line_item created successfully';
  ELSE
    RAISE EXCEPTION 'ERROR: Table challan_line_item was not created';
  END IF;
END $$;

-- Verify challan_dispatch_batch junction table creation
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables 
             WHERE table_name = 'challan_dispatch_batch') THEN
    RAISE NOTICE 'SUCCESS: Table challan_dispatch_batch created successfully';
  ELSE
    RAISE EXCEPTION 'ERROR: Table challan_dispatch_batch was not created';
  END IF;
END $$;

-- Verify critical columns in delivery_challan
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns 
             WHERE table_name = 'delivery_challan' 
             AND column_name IN ('total_taxable_value', 'order_id', 'challan_type', 'other_challan_type_details')) THEN
    RAISE NOTICE 'SUCCESS: All required columns exist in delivery_challan';
  ELSE
    RAISE EXCEPTION 'ERROR: Required columns missing in delivery_challan';
  END IF;
END $$;

-- Verify sequences created
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.sequences 
             WHERE sequence_name IN ('delivery_challan_sequence', 'challan_line_item_sequence', 'challan_dispatch_batch_sequence')) THEN
    RAISE NOTICE 'SUCCESS: All sequences created successfully';
  ELSE
    RAISE EXCEPTION 'ERROR: One or more sequences missing';
  END IF;
END $$;

-- =====================================================
-- 12. Add Challan Prefix to Tenant Challan Settings
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
-- 13. Final Verification
-- =====================================================

-- Verify challan_prefix column added
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns 
             WHERE table_name = 'tenant_challan_settings' 
             AND column_name = 'challan_prefix') THEN
    RAISE NOTICE 'SUCCESS: Column challan_prefix added to tenant_challan_settings';
  ELSE
    RAISE EXCEPTION 'ERROR: Column challan_prefix missing in tenant_challan_settings';
  END IF;
END $$;

-- =====================================================
-- 14. Add Unique Index on Challan Number and Tenant ID
-- =====================================================

-- Step 1: Check for duplicate challan numbers
DO $$
DECLARE
    duplicate_count INTEGER;
    rec RECORD;
BEGIN
    SELECT COUNT(*) INTO duplicate_count
    FROM (
        SELECT challan_number, tenant_id, COUNT(*) as cnt
        FROM delivery_challan
        WHERE deleted = false
        GROUP BY challan_number, tenant_id
        HAVING COUNT(*) > 1
    ) duplicates;
    
    IF duplicate_count > 0 THEN
        RAISE NOTICE 'Found % duplicate challan number(s) for the same tenant', duplicate_count;
        RAISE NOTICE 'Listing duplicates:';
        
        -- Show the duplicates
        FOR rec IN (
            SELECT challan_number, tenant_id, COUNT(*) as cnt
            FROM delivery_challan
            WHERE deleted = false
            GROUP BY challan_number, tenant_id
            HAVING COUNT(*) > 1
        ) LOOP
            RAISE NOTICE 'Challan Number: %, Tenant ID: %, Count: %', rec.challan_number, rec.tenant_id, rec.cnt;
        END LOOP;
    ELSE
        RAISE NOTICE 'No duplicate challan numbers found. Proceeding with constraint addition.';
    END IF;
END $$;

-- Step 2: Fix duplicates by appending a suffix to duplicate challan numbers
-- Keep the oldest challan with the original number, modify newer ones
DO $$
DECLARE
    rec RECORD;
    dup_rec RECORD;
    counter INTEGER;
BEGIN
    -- For each duplicate group
    FOR rec IN (
        SELECT challan_number, tenant_id, COUNT(*) as cnt
        FROM delivery_challan
        WHERE deleted = false
        GROUP BY challan_number, tenant_id
        HAVING COUNT(*) > 1
    ) LOOP
        counter := 1;
        
        -- For each duplicate challan (except the first/oldest one)
        FOR dup_rec IN (
            SELECT id, challan_number, created_at
            FROM delivery_challan
            WHERE challan_number = rec.challan_number
              AND tenant_id = rec.tenant_id
              AND deleted = false
            ORDER BY created_at ASC
            OFFSET 1  -- Skip the oldest one
        ) LOOP
            -- Update the challan number by appending a suffix
            UPDATE delivery_challan
            SET challan_number = rec.challan_number || '-DUP' || counter
            WHERE id = dup_rec.id;
            
            RAISE NOTICE 'Renamed duplicate challan ID % from "%" to "%"', 
                dup_rec.id, rec.challan_number, rec.challan_number || '-DUP' || counter;
            
            counter := counter + 1;
        END LOOP;
    END LOOP;
END $$;

-- Step 3: Add unique index to prevent duplicate challan numbers for the same tenant
-- Use a partial unique index to only enforce uniqueness on non-deleted records
CREATE UNIQUE INDEX uk_challan_number_tenant 
ON delivery_challan (challan_number, tenant_id) 
WHERE deleted = false;

-- Add comment for the index
COMMENT ON INDEX uk_challan_number_tenant IS 
'Ensures challan numbers are unique within a tenant for active (non-deleted) challans only. Deleted challans can have duplicate numbers and will be reused.';

-- =====================================================
-- 15. Verify WorkType Columns Added
-- =====================================================
DO $$
DECLARE
  delivery_challan_work_type_exists BOOLEAN;
  challan_line_item_work_type_exists BOOLEAN;
  delivery_challan_work_type_index_exists BOOLEAN;
BEGIN
  -- Check if delivery_challan.work_type column exists
  SELECT EXISTS (
    SELECT 1 
    FROM information_schema.columns 
    WHERE table_name = 'delivery_challan' 
    AND column_name = 'work_type'
  ) INTO delivery_challan_work_type_exists;
  
  -- Check if challan_line_item.work_type column exists
  SELECT EXISTS (
    SELECT 1 
    FROM information_schema.columns 
    WHERE table_name = 'challan_line_item' 
    AND column_name = 'work_type'
  ) INTO challan_line_item_work_type_exists;
  
  -- Check if index exists
  SELECT EXISTS (
    SELECT 1 
    FROM pg_indexes 
    WHERE tablename = 'delivery_challan' 
    AND indexname = 'idx_delivery_challan_work_type'
  ) INTO delivery_challan_work_type_index_exists;
  
  -- Raise notice with verification results
  IF delivery_challan_work_type_exists AND challan_line_item_work_type_exists AND delivery_challan_work_type_index_exists THEN
    RAISE NOTICE '  ✓ delivery_challan.work_type column added';
    RAISE NOTICE '  ✓ challan_line_item.work_type column added';
    RAISE NOTICE '  ✓ idx_delivery_challan_work_type index created';
  ELSE
    RAISE EXCEPTION 'WorkType verification failed! Missing: delivery_challan.work_type=%, challan_line_item.work_type=%, index=%',
                    delivery_challan_work_type_exists, challan_line_item_work_type_exists, delivery_challan_work_type_index_exists;
  END IF;
END $$;

-- =====================================================
-- 16. Add Challan Fields to Dispatch Batch Table
-- =====================================================

-- Add challan_number and challan_date_time columns to dispatch_batch table
-- These fields store challan information when a challan is created for the dispatch batch
ALTER TABLE dispatch_batch
ADD COLUMN IF NOT EXISTS challan_number VARCHAR(255),
ADD COLUMN IF NOT EXISTS challan_date_time TIMESTAMP;

-- Add comments for clarity
COMMENT ON COLUMN dispatch_batch.challan_number IS 'Challan number when dispatch batch is associated with a delivery challan';
COMMENT ON COLUMN dispatch_batch.challan_date_time IS 'Challan date and time when dispatch batch is associated with a delivery challan';

-- Verify challan fields added to dispatch_batch
DO $$
DECLARE
  challan_number_exists BOOLEAN;
  challan_date_time_exists BOOLEAN;
BEGIN
  -- Check if dispatch_batch.challan_number column exists
  SELECT EXISTS (
    SELECT 1 
    FROM information_schema.columns 
    WHERE table_name = 'dispatch_batch' 
    AND column_name = 'challan_number'
  ) INTO challan_number_exists;
  
  -- Check if dispatch_batch.challan_date_time column exists
  SELECT EXISTS (
    SELECT 1 
    FROM information_schema.columns 
    WHERE table_name = 'dispatch_batch' 
    AND column_name = 'challan_date_time'
  ) INTO challan_date_time_exists;
  
  -- Raise notice with verification results
  IF challan_number_exists AND challan_date_time_exists THEN
    RAISE NOTICE '  ✓ dispatch_batch.challan_number column added';
    RAISE NOTICE '  ✓ dispatch_batch.challan_date_time column added';
  ELSE
    RAISE EXCEPTION 'Dispatch batch challan fields verification failed! Missing: challan_number=%, challan_date_time=%',
                    challan_number_exists, challan_date_time_exists;
  END IF;
END $$;

-- =====================================================
-- 17. Final Success Message
-- =====================================================
DO $$
BEGIN
  RAISE NOTICE '========================================';
  RAISE NOTICE 'V1_72 Migration completed successfully!';
  RAISE NOTICE 'Created: delivery_challan, challan_line_item, challan_dispatch_batch';
  RAISE NOTICE 'Added: challan_prefix to tenant_challan_settings';
  RAISE NOTICE 'Added: consignor details, terms & conditions, bank details, amount_in_words to delivery_challan';
  RAISE NOTICE 'Added: Unique index on challan_number and tenant_id (active records only)';
  RAISE NOTICE 'Added: work_type to delivery_challan and challan_line_item tables';
  RAISE NOTICE 'Added: challan_number and challan_date_time to dispatch_batch table';
  RAISE NOTICE 'Challan Management System is ready!';
  RAISE NOTICE '========================================';
END $$;

