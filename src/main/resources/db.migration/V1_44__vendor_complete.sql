-- Migration: Complete Vendor Management System
-- Version: V1_44
-- Description: Complete vendor management with dispatch, receive, and workflow integration

BEGIN;

-- ======================================================================
-- PART 1: Update Workflow Operation Types to include VENDOR
-- ======================================================================

-- Drop existing check constraints
ALTER TABLE workflow_step DROP CONSTRAINT IF EXISTS workflow_step_operation_type_check;
ALTER TABLE item_workflow_step DROP CONSTRAINT IF EXISTS item_workflow_step_operation_type_check;

-- Add updated check constraints including VENDOR
ALTER TABLE workflow_step 
ADD CONSTRAINT workflow_step_operation_type_check 
CHECK (operation_type IN ('FORGING', 'HEAT_TREATMENT', 'MACHINING', 'VENDOR', 'QUALITY', 'DISPATCH'));

ALTER TABLE item_workflow_step 
ADD CONSTRAINT item_workflow_step_operation_type_check 
CHECK (operation_type IN ('FORGING', 'HEAT_TREATMENT', 'MACHINING', 'VENDOR', 'QUALITY', 'DISPATCH'));

-- ======================================================================
-- PART 2: Basic Vendor Tables
-- ======================================================================

-- Sequence for vendor ID generation
CREATE SEQUENCE vendor_sequence START 1 INCREMENT BY 1;

CREATE TABLE vendor (
    id BIGINT PRIMARY KEY DEFAULT nextval('vendor_sequence'),
    vendor_name VARCHAR(255) NOT NULL,
    address TEXT,
    gstin_uin VARCHAR(15),
    phone_number VARCHAR(15),
    pan_number VARCHAR(10),
    tenant_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_vendor_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT unique_vendor_name_tenant_deleted UNIQUE (vendor_name, tenant_id, deleted)
);

CREATE INDEX idx_vendor_name ON vendor (vendor_name) WHERE deleted = false;
CREATE INDEX idx_vendor_tenant_id ON vendor (tenant_id) WHERE deleted = false;
CREATE INDEX idx_vendor_pan_number ON vendor(pan_number) WHERE deleted = false;

-- Sequence for vendor entity ID generation
CREATE SEQUENCE vendor_entity_sequence START 1 INCREMENT BY 1;

CREATE TABLE vendor_entity (
    id BIGINT PRIMARY KEY DEFAULT nextval('vendor_entity_sequence'),
    vendor_entity_name VARCHAR(255) NOT NULL,
    address TEXT,
    gstin_uin VARCHAR(15),
    phone_number VARCHAR(15),
    pan_number VARCHAR(10),
    is_billing_entity BOOLEAN DEFAULT FALSE,
    is_shipping_entity BOOLEAN DEFAULT FALSE,
    vendor_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_vendor_entity_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(id),
    CONSTRAINT unique_entity_name_vendor_deleted UNIQUE (vendor_entity_name, vendor_id, deleted)
);

CREATE INDEX idx_vendor_entity_name ON vendor_entity (vendor_entity_name) WHERE deleted = false;
CREATE INDEX idx_vendor_entity_vendor_id ON vendor_entity (vendor_id) WHERE deleted = false;
CREATE INDEX idx_vendor_entity_pan_number ON vendor_entity(pan_number) WHERE deleted = false;

-- ======================================================================
-- PART 3: Vendor Dispatch Batch (Updated Architecture)
-- ======================================================================

-- Sequence for vendor dispatch batch ID generation
CREATE SEQUENCE vendor_dispatch_batch_sequence START 1 INCREMENT BY 1;

CREATE TABLE vendor_dispatch_batch (
    id BIGINT PRIMARY KEY DEFAULT nextval('vendor_dispatch_batch_sequence'),
    vendor_dispatch_batch_number VARCHAR(255) NOT NULL,
    original_vendor_dispatch_batch_number VARCHAR(255),
    vendor_dispatch_batch_status VARCHAR(50) NOT NULL,
    dispatched_at TIMESTAMP,

    remarks TEXT,
    packaging_type VARCHAR(50),
    packaging_quantity INTEGER,
    per_packaging_quantity INTEGER,
    use_uniform_packaging BOOLEAN DEFAULT FALSE,
    tenant_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    billing_entity_id BIGINT NOT NULL,
    shipping_entity_id BIGINT NOT NULL,
    item_weight_type VARCHAR(30),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_vendor_dispatch_batch_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_vendor_dispatch_batch_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(id),
    CONSTRAINT fk_vendor_dispatch_batch_billing_entity FOREIGN KEY (billing_entity_id) REFERENCES vendor_entity(id),
    CONSTRAINT fk_vendor_dispatch_batch_shipping_entity FOREIGN KEY (shipping_entity_id) REFERENCES vendor_entity(id),
    CONSTRAINT unique_vendor_dispatch_batch_number_tenant_deleted UNIQUE (vendor_dispatch_batch_number, tenant_id, deleted)
);

CREATE INDEX idx_vendor_dispatch_batch_number ON vendor_dispatch_batch (vendor_dispatch_batch_number) WHERE deleted = false;
CREATE INDEX idx_vendor_dispatch_batch_vendor_id ON vendor_dispatch_batch (vendor_id) WHERE deleted = false;
CREATE INDEX idx_vendor_dispatch_batch_status ON vendor_dispatch_batch (vendor_dispatch_batch_status) WHERE deleted = false;

-- Create vendor_dispatch_batch_processes table for @ElementCollection
CREATE TABLE vendor_dispatch_batch_processes (
    vendor_dispatch_batch_id BIGINT NOT NULL,
    process_type VARCHAR(50) NOT NULL,
    CONSTRAINT fk_vendor_dispatch_batch_processes_vendor_dispatch_batch
        FOREIGN KEY (vendor_dispatch_batch_id) REFERENCES vendor_dispatch_batch(id) ON DELETE CASCADE
);

CREATE INDEX idx_vendor_dispatch_batch_processes_vendor_dispatch_batch_id 
    ON vendor_dispatch_batch_processes(vendor_dispatch_batch_id);

-- ======================================================================
-- PART 4: Processed Item Vendor Dispatch Batch (Workflow Integration)
-- ======================================================================

-- Sequence for processed item vendor dispatch batch ID generation
CREATE SEQUENCE processed_item_vendor_dispatch_batch_sequence START 1 INCREMENT BY 1;

-- Create processed_item_vendor_dispatch_batch table for vendor workflow integration
CREATE TABLE processed_item_vendor_dispatch_batch (
    id BIGINT PRIMARY KEY DEFAULT nextval('processed_item_vendor_dispatch_batch_sequence'),
    vendor_dispatch_batch_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    item_status VARCHAR(100) NOT NULL DEFAULT 'VENDOR_DISPATCH_NOT_STARTED',

    -- Workflow Integration Fields
    workflow_identifier VARCHAR(255),
    item_workflow_id BIGINT,
    previous_operation_processed_item_id BIGINT,

    -- Simple Dispatch Tracking - Just what was sent to vendor
    is_in_pieces BOOLEAN NOT NULL DEFAULT FALSE, -- TRUE for count-based measurements, FALSE for weight/volume measurements
    dispatched_pieces_count INTEGER,
    dispatched_quantity DOUBLE PRECISION,

    -- Expected quantities
    total_expected_pieces_count INTEGER DEFAULT 0,

    -- Total Received Tracking - Track cumulative received quantities across all VendorReceiveBatch
    total_received_pieces_count INTEGER DEFAULT 0,

    -- Total Rejected Tracking - Track cumulative rejected quantities across all VendorReceiveBatch
    total_rejected_pieces_count INTEGER DEFAULT 0,

    -- Total Tenant Rejects Tracking - Track cumulative tenant rejects across all VendorReceiveBatch
    total_tenant_rejects_count INTEGER DEFAULT 0,

    -- Total Pieces Eligible for Next Operation
    total_pieces_eligible_for_next_operation INTEGER DEFAULT 0,

    -- Completion Status
    fully_received BOOLEAN DEFAULT FALSE,

    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    -- Foreign Key Constraints
    CONSTRAINT fk_processed_item_vendor_dispatch_batch_vendor_dispatch_batch
        FOREIGN KEY (vendor_dispatch_batch_id) REFERENCES vendor_dispatch_batch(id),
    CONSTRAINT fk_processed_item_vendor_dispatch_batch_item
        FOREIGN KEY (item_id) REFERENCES item(id),
    CONSTRAINT fk_processed_item_vendor_dispatch_batch_item_workflow
        FOREIGN KEY (item_workflow_id) REFERENCES item_workflow(id),

    -- Unique constraint to ensure one-to-one relationship
    CONSTRAINT unique_processed_item_vendor_dispatch_batch_vendor_dispatch_batch
        UNIQUE (vendor_dispatch_batch_id)
);

-- Create indexes for processed_item_vendor_dispatch_batch table performance
CREATE INDEX idx_processed_item_vendor_dispatch_batch_vendor_dispatch_batch_id
    ON processed_item_vendor_dispatch_batch(vendor_dispatch_batch_id);

CREATE INDEX idx_processed_item_vendor_dispatch_batch_item_id
    ON processed_item_vendor_dispatch_batch(item_id);

CREATE INDEX idx_processed_item_vendor_dispatch_batch_item_workflow_id
    ON processed_item_vendor_dispatch_batch(item_workflow_id);

CREATE INDEX idx_processed_item_vendor_dispatch_batch_workflow_identifier
    ON processed_item_vendor_dispatch_batch(workflow_identifier);

CREATE INDEX idx_processed_item_vendor_dispatch_batch_item_status
    ON processed_item_vendor_dispatch_batch(item_status);

CREATE INDEX idx_processed_item_vendor_dispatch_batch_deleted
    ON processed_item_vendor_dispatch_batch(deleted);

-- ======================================================================
-- PART 5: Vendor Dispatch Heat (Heat Consumption Tracking)
-- ======================================================================

-- Sequence for vendor dispatch heat ID generation
CREATE SEQUENCE vendor_dispatch_heat_sequence START 1 INCREMENT BY 1;

-- Create vendor_dispatch_heat table
CREATE TABLE IF NOT EXISTS vendor_dispatch_heat (
    id BIGINT PRIMARY KEY DEFAULT nextval('vendor_dispatch_heat_sequence'),
    processed_item_vendor_dispatch_batch_id BIGINT NOT NULL,
    heat_id BIGINT NOT NULL,
    consumption_type VARCHAR(10) NOT NULL CHECK (consumption_type IN ('QUANTITY', 'PIECES')),
    quantity_used DECIMAL(10,3),
    pieces_used INTEGER,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    -- Add foreign key constraints
    CONSTRAINT fk_vendor_dispatch_heat_processed_item_vendor_dispatch_batch
        FOREIGN KEY (processed_item_vendor_dispatch_batch_id)
        REFERENCES processed_item_vendor_dispatch_batch(id),
    CONSTRAINT fk_vendor_dispatch_heat_heat
        FOREIGN KEY (heat_id)
        REFERENCES heat(id),

    -- Add check constraints for consumption type validation
    CONSTRAINT chk_vendor_dispatch_heat_quantity_consumption
        CHECK (
            (consumption_type = 'QUANTITY' AND quantity_used IS NOT NULL AND quantity_used > 0 AND pieces_used IS NULL) OR
            (consumption_type = 'PIECES' AND pieces_used IS NOT NULL AND pieces_used > 0 AND quantity_used IS NULL)
        )
);

-- Create indexes for vendor_dispatch_heat table performance
CREATE INDEX idx_vendor_dispatch_heat_processed_item_vendor_dispatch_batch_id
    ON vendor_dispatch_heat(processed_item_vendor_dispatch_batch_id) WHERE deleted = false;

CREATE INDEX idx_vendor_dispatch_heat_heat_id
    ON vendor_dispatch_heat(heat_id) WHERE deleted = false;

-- ======================================================================
-- PART 6: Vendor Receive Batch
-- ======================================================================

-- Sequence for vendor receive batch ID generation
CREATE SEQUENCE vendor_receive_batch_sequence START 1 INCREMENT BY 1;

CREATE TABLE vendor_receive_batch (
    id BIGINT PRIMARY KEY DEFAULT nextval('vendor_receive_batch_sequence'),
    vendor_receive_batch_number VARCHAR(255) NOT NULL,
    original_vendor_receive_batch_number VARCHAR(255),
    vendor_receive_batch_status VARCHAR(50) NOT NULL,
    received_at TIMESTAMP,
    is_in_pieces BOOLEAN NOT NULL DEFAULT FALSE, -- TRUE for count-based measurements, FALSE for weight/volume measurements
    received_pieces_count INTEGER, -- Used when is_in_pieces = TRUE
    rejected_pieces_count INTEGER, -- Rejected pieces count (for count-based measurements)
    tenant_rejects_count INTEGER, -- Tenant rejects count - items already rejected by tenant
    pieces_eligible_for_next_operation INTEGER, -- Pieces eligible for next operation in workflow
    quality_check_required BOOLEAN DEFAULT FALSE,
    quality_check_completed BOOLEAN DEFAULT FALSE,
    remarks TEXT,
    packaging_type VARCHAR(50),
    packaging_quantity INTEGER,
    per_packaging_quantity INTEGER,
    tenant_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    vendor_dispatch_batch_id BIGINT, -- Optional association with dispatch batch
    billing_entity_id BIGINT NOT NULL,
    shipping_entity_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_vendor_receive_batch_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_vendor_receive_batch_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(id),
    CONSTRAINT fk_vendor_receive_batch_dispatch_batch FOREIGN KEY (vendor_dispatch_batch_id) REFERENCES vendor_dispatch_batch(id),
    CONSTRAINT fk_vendor_receive_batch_billing_entity FOREIGN KEY (billing_entity_id) REFERENCES vendor_entity(id),
    CONSTRAINT fk_vendor_receive_batch_shipping_entity FOREIGN KEY (shipping_entity_id) REFERENCES vendor_entity(id),
    CONSTRAINT unique_vendor_receive_batch_number_tenant_deleted UNIQUE (vendor_receive_batch_number, tenant_id, deleted)
);

CREATE INDEX idx_vendor_receive_batch_number ON vendor_receive_batch (vendor_receive_batch_number) WHERE deleted = false;
CREATE INDEX idx_vendor_receive_batch_vendor_id ON vendor_receive_batch (vendor_id) WHERE deleted = false;
CREATE INDEX idx_vendor_receive_batch_dispatch_batch_id ON vendor_receive_batch (vendor_dispatch_batch_id) WHERE deleted = false;
CREATE INDEX idx_vendor_receive_batch_status ON vendor_receive_batch (vendor_receive_batch_status) WHERE deleted = false;
CREATE INDEX idx_vendor_receive_quality_check ON vendor_receive_batch (quality_check_required, quality_check_completed) WHERE deleted = false;

-- ======================================================================
-- PART 7: Comments and Documentation
-- ======================================================================

-- Add comments for PAN number columns
COMMENT ON COLUMN vendor.pan_number IS 'PAN number of the vendor';
COMMENT ON COLUMN vendor_entity.pan_number IS 'PAN number of the vendor entity';

-- Add comments for vendor dispatch batch
COMMENT ON TABLE vendor_dispatch_batch IS 'Tracks vendor dispatch batches with workflow integration';

-- Add comments for processed_item_vendor_dispatch_batch table
COMMENT ON TABLE processed_item_vendor_dispatch_batch IS 'Tracks vendor dispatch batches at item level for workflow integration following ProcessedItem pattern';
COMMENT ON COLUMN processed_item_vendor_dispatch_batch.vendor_dispatch_batch_id IS 'Reference to the vendor dispatch batch (OneToOne relationship)';
COMMENT ON COLUMN processed_item_vendor_dispatch_batch.item_id IS 'Reference to the item being processed';
COMMENT ON COLUMN processed_item_vendor_dispatch_batch.item_status IS 'Current status of the item in vendor dispatch workflow';
COMMENT ON COLUMN processed_item_vendor_dispatch_batch.workflow_identifier IS 'Unique identifier for workflow tracking across operations';
COMMENT ON COLUMN processed_item_vendor_dispatch_batch.item_workflow_id IS 'Reference to the item workflow for integration';
COMMENT ON COLUMN processed_item_vendor_dispatch_batch.previous_operation_processed_item_id IS 'ID of the processed item from previous operation for piece consumption';

-- Add comments for vendor_dispatch_heat table
COMMENT ON TABLE vendor_dispatch_heat IS 'Tracks heat consumption for vendor dispatch operations, maintaining material traceability';
COMMENT ON COLUMN vendor_dispatch_heat.processed_item_vendor_dispatch_batch_id IS 'Reference to the processed item vendor dispatch batch that consumed the heat';
COMMENT ON COLUMN vendor_dispatch_heat.heat_id IS 'Reference to the heat that was consumed';
COMMENT ON COLUMN vendor_dispatch_heat.consumption_type IS 'Type of consumption: QUANTITY or PIECES';
COMMENT ON COLUMN vendor_dispatch_heat.quantity_used IS 'Quantity consumed (if consumption_type is QUANTITY)';
COMMENT ON COLUMN vendor_dispatch_heat.pieces_used IS 'Number of pieces used (if consumption_type is PIECES)';

-- Add comments for vendor_receive_batch reject tracking fields
COMMENT ON COLUMN vendor_receive_batch.rejected_pieces_count IS 'Rejected pieces count for count-based measurements';
COMMENT ON COLUMN vendor_receive_batch.tenant_rejects_count IS 'Tenant rejects count - items already rejected by tenant but dispatched to vendor';
COMMENT ON COLUMN vendor_receive_batch.pieces_eligible_for_next_operation IS 'Pieces eligible for next operation in workflow';

-- Add comments for processed_item_vendor_dispatch_batch reject tracking fields
COMMENT ON COLUMN processed_item_vendor_dispatch_batch.total_rejected_pieces_count IS 'Total rejected pieces count across all associated vendor receive batches';
COMMENT ON COLUMN processed_item_vendor_dispatch_batch.total_tenant_rejects_count IS 'Total tenant rejects count across all associated vendor receive batches';
COMMENT ON COLUMN processed_item_vendor_dispatch_batch.total_pieces_eligible_for_next_operation IS 'Total pieces eligible for next operation across all associated vendor receive batches';

-- ======================================================================
-- PART 8: Validation and verification
-- ======================================================================

-- Verify that all tables were created successfully
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'vendor') THEN
        RAISE EXCEPTION 'Migration failed: vendor table not created';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'vendor_entity') THEN
        RAISE EXCEPTION 'Migration failed: vendor_entity table not created';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'vendor_dispatch_batch') THEN
        RAISE EXCEPTION 'Migration failed: vendor_dispatch_batch table not created';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'processed_item_vendor_dispatch_batch') THEN
        RAISE EXCEPTION 'Migration failed: processed_item_vendor_dispatch_batch table not created';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'vendor_dispatch_heat') THEN
        RAISE EXCEPTION 'Migration failed: vendor_dispatch_heat table not created';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'vendor_receive_batch') THEN
        RAISE EXCEPTION 'Migration failed: vendor_receive_batch table not created';
    END IF;
END $$;

-- Verify that the OneToOne constraint exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'unique_processed_item_vendor_dispatch_batch_vendor_dispatch_batch'
    ) THEN
        RAISE EXCEPTION 'Migration failed: OneToOne constraint not created for processed_item_vendor_dispatch_batch';
    END IF;
END $$;

COMMIT; 