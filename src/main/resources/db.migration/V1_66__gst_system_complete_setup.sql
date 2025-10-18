-- Migration: Complete GST System Setup (Invoice, Challan, E-Way Bill)
-- Version: V1_61
-- Description: Consolidated migration for complete GST system implementation including
--              GST entities, enhanced buyer/vendor entities, and dispatch batch enhancements

BEGIN;

-- ======================================================================
-- PART 1: Create GST Core Entities
-- ======================================================================

-- Sequence for delivery challan ID generation
CREATE SEQUENCE delivery_challan_sequence START 1 INCREMENT BY 1;

-- Delivery Challan table
CREATE TABLE delivery_challan (
    id BIGINT PRIMARY KEY DEFAULT nextval('delivery_challan_sequence'),
    challan_number VARCHAR(50) NOT NULL,
    challan_date TIMESTAMP NOT NULL,
    challan_type VARCHAR(50) NOT NULL,
    dispatch_batch_id BIGINT,
    converted_to_invoice_id BIGINT,
    consignee_buyer_entity_id BIGINT REFERENCES buyer_entity(id),
    consignee_vendor_entity_id BIGINT REFERENCES vendor_entity(id),
    transportation_reason TEXT NOT NULL,
    transportation_mode VARCHAR(20) DEFAULT 'ROAD',
    expected_delivery_date DATE,
    actual_delivery_date DATE,
    total_quantity DECIMAL(15,3) DEFAULT 0,
    total_value DECIMAL(15,2) DEFAULT 0,
    status VARCHAR(20) DEFAULT 'DRAFT',
    document_path VARCHAR(500),
    tenant_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_delivery_challan_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_delivery_challan_dispatch_batch FOREIGN KEY (dispatch_batch_id) REFERENCES dispatch_batch(id),
    CONSTRAINT chk_challan_single_consignee CHECK (
        (consignee_buyer_entity_id IS NOT NULL AND consignee_vendor_entity_id IS NULL) OR
        (consignee_buyer_entity_id IS NULL AND consignee_vendor_entity_id IS NOT NULL)
    ),
    CONSTRAINT chk_challan_type CHECK (challan_type IN ('JOB_WORK', 'BRANCH_TRANSFER', 'SAMPLE_DISPATCH', 'RETURN_GOODS', 'OTHER')),
    CONSTRAINT chk_challan_status CHECK (status IN ('DRAFT', 'GENERATED', 'DISPATCHED', 'RECEIVED', 'CANCELLED', 'CONVERTED_TO_INVOICE')),
    CONSTRAINT chk_challan_transportation_mode CHECK (transportation_mode IN ('ROAD', 'RAIL', 'AIR', 'SHIP'))
);

-- Sequence for invoice ID generation
CREATE SEQUENCE invoice_sequence START 1 INCREMENT BY 1;

-- Invoice table
CREATE TABLE invoice (
    id BIGINT PRIMARY KEY DEFAULT nextval('invoice_sequence'),
    invoice_number VARCHAR(50) NOT NULL,
    invoice_date TIMESTAMP NOT NULL,
    invoice_type VARCHAR(20) DEFAULT 'TAX_INVOICE',
    dispatch_batch_id BIGINT,
    delivery_challan_id BIGINT,
    original_invoice_id BIGINT,
    recipient_buyer_entity_id BIGINT REFERENCES buyer_entity(id),
    recipient_vendor_entity_id BIGINT REFERENCES vendor_entity(id),
    place_of_supply VARCHAR(50) NOT NULL,
    is_inter_state BOOLEAN DEFAULT FALSE,
    total_taxable_value DECIMAL(15,2) NOT NULL,
    total_cgst_amount DECIMAL(15,2) DEFAULT 0,
    total_sgst_amount DECIMAL(15,2) DEFAULT 0,
    total_igst_amount DECIMAL(15,2) DEFAULT 0,
    total_invoice_value DECIMAL(15,2) NOT NULL,
    payment_terms VARCHAR(500),
    due_date DATE,
    status VARCHAR(20) DEFAULT 'DRAFT',
    document_path VARCHAR(500),
    tenant_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_invoice_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_invoice_dispatch_batch FOREIGN KEY (dispatch_batch_id) REFERENCES dispatch_batch(id),
    CONSTRAINT fk_invoice_delivery_challan FOREIGN KEY (delivery_challan_id) REFERENCES delivery_challan(id),
    CONSTRAINT fk_invoice_original FOREIGN KEY (original_invoice_id) REFERENCES invoice(id),
    CONSTRAINT chk_invoice_single_recipient CHECK (
        (recipient_buyer_entity_id IS NOT NULL AND recipient_vendor_entity_id IS NULL) OR
        (recipient_buyer_entity_id IS NULL AND recipient_vendor_entity_id IS NOT NULL)
    ),
    CONSTRAINT chk_invoice_type CHECK (invoice_type IN ('TAX_INVOICE', 'BILL_OF_SUPPLY', 'EXPORT_INVOICE', 'REVISED_INVOICE', 'CREDIT_NOTE', 'DEBIT_NOTE')),
    CONSTRAINT chk_invoice_status CHECK (status IN ('DRAFT', 'GENERATED', 'SENT', 'PAID', 'PARTIALLY_PAID', 'CANCELLED', 'OVERDUE'))
);

-- Sequence for e-way bill ID generation
CREATE SEQUENCE eway_bill_sequence START 1 INCREMENT BY 1;

-- E-Way Bill table
CREATE TABLE eway_bill (
    id BIGINT PRIMARY KEY DEFAULT nextval('eway_bill_sequence'),
    eway_bill_number VARCHAR(12) NOT NULL UNIQUE,
    invoice_id BIGINT,
    delivery_challan_id BIGINT,
    dispatch_batch_id BIGINT,
    recipient_buyer_entity_id BIGINT REFERENCES buyer_entity(id),
    recipient_vendor_entity_id BIGINT REFERENCES vendor_entity(id),
    supplier_gstin VARCHAR(15) NOT NULL,
    supplier_name VARCHAR(200) NOT NULL,
    supplier_address TEXT NOT NULL,
    supplier_state_code VARCHAR(2) NOT NULL,
    supplier_pincode VARCHAR(6) NOT NULL,
    recipient_gstin VARCHAR(15),
    recipient_name VARCHAR(200) NOT NULL,
    recipient_address TEXT NOT NULL,
    recipient_state_code VARCHAR(2) NOT NULL,
    recipient_pincode VARCHAR(6) NOT NULL,
    document_number VARCHAR(50) NOT NULL,
    document_date DATE NOT NULL,
    document_type VARCHAR(20) NOT NULL,
    total_taxable_value DECIMAL(15,2) NOT NULL,
    total_tax_amount DECIMAL(15,2) NOT NULL,
    total_invoice_value DECIMAL(15,2) NOT NULL,
    transportation_mode VARCHAR(20) NOT NULL,
    transporter_name VARCHAR(200),
    transporter_id VARCHAR(15),
    vehicle_number VARCHAR(20),
    distance INTEGER,
    generated_date TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'GENERATED',
    tenant_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_eway_bill_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_eway_bill_invoice FOREIGN KEY (invoice_id) REFERENCES invoice(id),
    CONSTRAINT fk_eway_bill_delivery_challan FOREIGN KEY (delivery_challan_id) REFERENCES delivery_challan(id),
    CONSTRAINT fk_eway_bill_dispatch_batch FOREIGN KEY (dispatch_batch_id) REFERENCES dispatch_batch(id),
    CONSTRAINT chk_eway_bill_single_recipient CHECK (
        (recipient_buyer_entity_id IS NOT NULL AND recipient_vendor_entity_id IS NULL) OR
        (recipient_buyer_entity_id IS NULL AND recipient_vendor_entity_id IS NOT NULL)
    ),
    CONSTRAINT chk_eway_bill_status CHECK (status IN ('GENERATED', 'CANCELLED', 'VALID', 'EXPIRED', 'REJECTED')),
    CONSTRAINT chk_eway_bill_transportation_mode CHECK (transportation_mode IN ('ROAD', 'RAIL', 'AIR', 'SHIP')),
    CONSTRAINT chk_eway_bill_document_type CHECK (document_type IN ('INVOICE', 'CHALLAN', 'BILL_OF_SUPPLY', 'CREDIT_NOTE', 'DEBIT_NOTE'))
);

-- Sequence for GST configuration ID generation
CREATE SEQUENCE gst_configuration_sequence START 1 INCREMENT BY 1;

-- GST Configuration table
CREATE TABLE gst_configuration (
    id BIGINT PRIMARY KEY DEFAULT nextval('gst_configuration_sequence'),
    tenant_id BIGINT NOT NULL UNIQUE,
    company_gstin VARCHAR(15) NOT NULL,
    company_legal_name VARCHAR(200) NOT NULL,
    company_trade_name VARCHAR(200),
    company_address TEXT NOT NULL,
    company_state_code VARCHAR(2) NOT NULL,
    company_pincode VARCHAR(6) NOT NULL,
    invoice_number_prefix VARCHAR(10),
    current_invoice_sequence INTEGER DEFAULT 1,
    challan_number_prefix VARCHAR(10),
    current_challan_sequence INTEGER DEFAULT 1,
    eway_bill_threshold DECIMAL(15,2) DEFAULT 50000.00,
    auto_generate_eway_bill BOOLEAN DEFAULT TRUE,
    default_cgst_rate DECIMAL(5,2) DEFAULT 9.00,
    default_sgst_rate DECIMAL(5,2) DEFAULT 9.00,
    default_igst_rate DECIMAL(5,2) DEFAULT 18.00,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_gst_configuration_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

-- ======================================================================
-- PART 2: Enhance Buyer and Vendor Entities for GST
-- ======================================================================

-- Add GST fields to buyer table
ALTER TABLE buyer 
ADD COLUMN state_code VARCHAR(2),
ADD COLUMN pincode VARCHAR(6);

-- Add GST fields to buyer_entity table
ALTER TABLE buyer_entity 
ADD COLUMN state_code VARCHAR(2),
ADD COLUMN pincode VARCHAR(6);

-- Add GST fields to vendor table
ALTER TABLE vendor 
ADD COLUMN state_code VARCHAR(2),
ADD COLUMN pincode VARCHAR(6);

-- Add GST fields to vendor_entity table
ALTER TABLE vendor_entity 
ADD COLUMN state_code VARCHAR(2),
ADD COLUMN pincode VARCHAR(6);

-- ======================================================================
-- PART 3: Enhance Dispatch Batch for GST
-- ======================================================================

-- Add GST-related columns to dispatch_batch
ALTER TABLE dispatch_batch 
ADD COLUMN gstin VARCHAR(15),
ADD COLUMN hsn_code VARCHAR(10),
ADD COLUMN taxable_value DECIMAL(15,2),
ADD COLUMN cgst_amount DECIMAL(15,2) DEFAULT 0,
ADD COLUMN sgst_amount DECIMAL(15,2) DEFAULT 0,
ADD COLUMN igst_amount DECIMAL(15,2) DEFAULT 0,
ADD COLUMN total_tax_amount DECIMAL(15,2) DEFAULT 0,
ADD COLUMN total_amount DECIMAL(15,2),
ADD COLUMN transportation_mode VARCHAR(20) DEFAULT 'ROAD',
ADD COLUMN transportation_distance INTEGER,
ADD COLUMN requires_eway_bill BOOLEAN DEFAULT FALSE,
ADD COLUMN eway_bill_threshold_met BOOLEAN DEFAULT FALSE;

-- ======================================================================
-- PART 4: Add Validation Constraints
-- ======================================================================

-- Buyer/Vendor pincode validation (should be 6 digits)
ALTER TABLE buyer ADD CONSTRAINT chk_buyer_pincode_format CHECK (pincode IS NULL OR pincode ~ '^[0-9]{6}$');
ALTER TABLE buyer_entity ADD CONSTRAINT chk_buyer_entity_pincode_format CHECK (pincode IS NULL OR pincode ~ '^[0-9]{6}$');
ALTER TABLE vendor ADD CONSTRAINT chk_vendor_pincode_format CHECK (pincode IS NULL OR pincode ~ '^[0-9]{6}$');
ALTER TABLE vendor_entity ADD CONSTRAINT chk_vendor_entity_pincode_format CHECK (pincode IS NULL OR pincode ~ '^[0-9]{6}$');

-- Buyer/Vendor state code validation (should be 2 digits)
ALTER TABLE buyer ADD CONSTRAINT chk_buyer_state_code_format CHECK (state_code IS NULL OR state_code ~ '^[0-9]{2}$');
ALTER TABLE buyer_entity ADD CONSTRAINT chk_buyer_entity_state_code_format CHECK (state_code IS NULL OR state_code ~ '^[0-9]{2}$');
ALTER TABLE vendor ADD CONSTRAINT chk_vendor_state_code_format CHECK (state_code IS NULL OR state_code ~ '^[0-9]{2}$');
ALTER TABLE vendor_entity ADD CONSTRAINT chk_vendor_entity_state_code_format CHECK (state_code IS NULL OR state_code ~ '^[0-9]{2}$');

-- Dispatch batch constraints
ALTER TABLE dispatch_batch ADD CONSTRAINT chk_dispatch_transportation_mode CHECK (transportation_mode IN ('ROAD', 'RAIL', 'AIR', 'SHIP', 'OTHER'));
ALTER TABLE dispatch_batch ADD CONSTRAINT chk_dispatch_transportation_distance CHECK (transportation_distance IS NULL OR transportation_distance > 0);

-- ======================================================================
-- PART 5: Create Indexes for Performance
-- ======================================================================

-- GST core entities indexes
CREATE INDEX idx_delivery_challan_dispatch_batch ON delivery_challan(dispatch_batch_id);
CREATE INDEX idx_delivery_challan_tenant_status ON delivery_challan(tenant_id, status);
CREATE INDEX idx_delivery_challan_challan_date ON delivery_challan(challan_date);
CREATE INDEX idx_delivery_challan_consignee_buyer ON delivery_challan(consignee_buyer_entity_id);
CREATE INDEX idx_delivery_challan_consignee_vendor ON delivery_challan(consignee_vendor_entity_id);
CREATE INDEX idx_delivery_challan_deleted ON delivery_challan(deleted);

CREATE INDEX idx_invoice_dispatch_batch ON invoice(dispatch_batch_id);
CREATE INDEX idx_invoice_tenant_status ON invoice(tenant_id, status);
CREATE INDEX idx_invoice_date ON invoice(invoice_date);
CREATE INDEX idx_invoice_recipient_buyer ON invoice(recipient_buyer_entity_id);
CREATE INDEX idx_invoice_recipient_vendor ON invoice(recipient_vendor_entity_id);
CREATE INDEX idx_invoice_deleted ON invoice(deleted);

CREATE INDEX idx_eway_bill_invoice ON eway_bill(invoice_id);
CREATE INDEX idx_eway_bill_delivery_challan ON eway_bill(delivery_challan_id);
CREATE INDEX idx_eway_bill_dispatch_batch ON eway_bill(dispatch_batch_id);
CREATE INDEX idx_eway_bill_tenant_status ON eway_bill(tenant_id, status);
CREATE INDEX idx_eway_bill_recipient_buyer ON eway_bill(recipient_buyer_entity_id);
CREATE INDEX idx_eway_bill_recipient_vendor ON eway_bill(recipient_vendor_entity_id);
CREATE INDEX idx_eway_bill_number ON eway_bill(eway_bill_number);
CREATE INDEX idx_eway_bill_deleted ON eway_bill(deleted);

CREATE INDEX idx_gst_configuration_tenant ON gst_configuration(tenant_id);
CREATE INDEX idx_gst_configuration_gstin ON gst_configuration(company_gstin);
CREATE INDEX idx_gst_configuration_deleted ON gst_configuration(deleted);

-- Buyer/Vendor GST indexes
CREATE INDEX idx_buyer_state_code ON buyer(state_code);
CREATE INDEX idx_buyer_entity_state_code ON buyer_entity(state_code);
CREATE INDEX idx_vendor_state_code ON vendor(state_code);
CREATE INDEX idx_vendor_entity_state_code ON vendor_entity(state_code);

-- Dispatch batch GST indexes
CREATE INDEX idx_dispatch_batch_gstin ON dispatch_batch(gstin);
CREATE INDEX idx_dispatch_batch_hsn_code ON dispatch_batch(hsn_code);
CREATE INDEX idx_dispatch_batch_requires_eway_bill ON dispatch_batch(requires_eway_bill);

-- ======================================================================
-- PART 6: Add Comments for Documentation
-- ======================================================================

-- GST Core Entities
COMMENT ON TABLE delivery_challan IS 'Delivery challan for goods movement without immediate sale';
COMMENT ON TABLE invoice IS 'Tax invoice for sales with pricing and tax details';
COMMENT ON TABLE eway_bill IS 'Electronic way bill for goods movement compliance';
COMMENT ON TABLE gst_configuration IS 'Tenant-specific GST configuration and settings';

-- Buyer/Vendor GST fields
COMMENT ON COLUMN buyer.state_code IS 'State code (2 digits) for GST jurisdiction';
COMMENT ON COLUMN buyer.pincode IS 'Pincode (6 digits) for address identification';
COMMENT ON COLUMN buyer_entity.state_code IS 'State code (2 digits) for GST jurisdiction';
COMMENT ON COLUMN buyer_entity.pincode IS 'Pincode (6 digits) for address identification';
COMMENT ON COLUMN vendor.state_code IS 'State code (2 digits) for GST jurisdiction';
COMMENT ON COLUMN vendor.pincode IS 'Pincode (6 digits) for address identification';
COMMENT ON COLUMN vendor_entity.state_code IS 'State code (2 digits) for GST jurisdiction';
COMMENT ON COLUMN vendor_entity.pincode IS 'Pincode (6 digits) for address identification';

-- Dispatch batch GST fields
COMMENT ON COLUMN dispatch_batch.gstin IS 'GSTIN of the supplier/manufacturer';
COMMENT ON COLUMN dispatch_batch.hsn_code IS 'HSN code for the dispatched goods';
COMMENT ON COLUMN dispatch_batch.transportation_mode IS 'Mode of transportation for the goods';
COMMENT ON COLUMN dispatch_batch.transportation_distance IS 'Distance in kilometers for transportation';
COMMENT ON COLUMN dispatch_batch.requires_eway_bill IS 'Whether this dispatch requires E-Way Bill';
COMMENT ON COLUMN dispatch_batch.eway_bill_threshold_met IS 'Whether dispatch value meets E-Way Bill threshold';

COMMIT;

-- ======================================================================
-- MIGRATION SUMMARY
-- ======================================================================
-- Complete GST System Setup includes:
-- 
-- 1. GST Core Entities:
--    - delivery_challan: For goods movement tracking
--    - invoice: For sales and tax invoicing
--    - eway_bill: For GST compliance and transportation
--    - gst_configuration: For tenant-specific GST settings
--
-- 2. Enhanced Buyer/Vendor Entities:
--    - Added state_code and pincode fields
--    - Added validation constraints and indexes
--
-- 3. Enhanced Dispatch Batch:
--    - Added GST-related fields for tax calculation
--    - Added transportation and E-Way Bill fields
--
-- 4. Entity Integration:
--    - All GST entities reference existing Buyer/Vendor entities
--    - Proper foreign key constraints and validation
--    - Optimized indexes for performance
--
-- 5. Data Integrity:
--    - Check constraints for enum values
--    - Single consignee/recipient validation
--    - Proper cascade relationships
--
-- Benefits:
-- - Complete GST compliance system
-- - Integrated with existing entity structure
-- - High performance with proper indexing
-- - Data integrity with validation constraints
-- - Scalable architecture for future enhancements
-- ======================================================================
