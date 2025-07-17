-- Migration: Create Vendor Inventory System
-- Version: V1_45
-- Description: Add vendor inventory tracking to separate tenant inventory from vendor inventory

BEGIN;

-- Create sequence for vendor_inventory table
CREATE SEQUENCE IF NOT EXISTS vendor_inventory_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Create vendor_inventory table
CREATE TABLE vendor_inventory (
    id BIGINT NOT NULL DEFAULT nextval('vendor_inventory_sequence'),
    vendor_id BIGINT NOT NULL,
    original_heat_id BIGINT NOT NULL,
    raw_material_product_id BIGINT NOT NULL,
    heat_number VARCHAR(255) NOT NULL,
    total_dispatched_quantity DOUBLE PRECISION,
    available_quantity DOUBLE PRECISION,
    is_in_pieces BOOLEAN NOT NULL DEFAULT FALSE,
    total_dispatched_pieces INTEGER,
    available_pieces_count INTEGER,
    test_certificate_number VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    
    CONSTRAINT pk_vendor_inventory PRIMARY KEY (id),
    CONSTRAINT fk_vendor_inventory_vendor FOREIGN KEY (vendor_id) 
        REFERENCES vendor(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vendor_inventory_original_heat FOREIGN KEY (original_heat_id) 
        REFERENCES heat(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vendor_inventory_raw_material_product FOREIGN KEY (raw_material_product_id) 
        REFERENCES raw_material_product(id) ON DELETE RESTRICT,
    
    -- Ensure vendor can only have one inventory record per heat
    CONSTRAINT uk_vendor_inventory_vendor_heat_deleted 
        UNIQUE (vendor_id, original_heat_id, deleted),
    
    -- Check constraints for quantity consistency
    CONSTRAINT chk_vendor_inventory_quantity_consistency 
        CHECK (
            (is_in_pieces = false AND total_dispatched_quantity IS NOT NULL AND available_quantity IS NOT NULL 
             AND total_dispatched_pieces IS NULL AND available_pieces_count IS NULL) OR
            (is_in_pieces = true AND total_dispatched_pieces IS NOT NULL AND available_pieces_count IS NOT NULL 
             AND total_dispatched_quantity IS NULL AND available_quantity IS NULL)
        ),
    
    -- Check constraints for non-negative values
    CONSTRAINT chk_vendor_inventory_total_dispatched_quantity_positive 
        CHECK (total_dispatched_quantity IS NULL OR total_dispatched_quantity >= 0),
    CONSTRAINT chk_vendor_inventory_available_quantity_positive 
        CHECK (available_quantity IS NULL OR available_quantity >= 0),
    CONSTRAINT chk_vendor_inventory_total_dispatched_pieces_positive 
        CHECK (total_dispatched_pieces IS NULL OR total_dispatched_pieces >= 0),
    CONSTRAINT chk_vendor_inventory_available_pieces_count_positive 
        CHECK (available_pieces_count IS NULL OR available_pieces_count >= 0),
    
    -- Check that available <= total dispatched
    CONSTRAINT chk_vendor_inventory_available_lte_total_quantity 
        CHECK (available_quantity IS NULL OR total_dispatched_quantity IS NULL OR available_quantity <= total_dispatched_quantity),
    CONSTRAINT chk_vendor_inventory_available_lte_total_pieces 
        CHECK (available_pieces_count IS NULL OR total_dispatched_pieces IS NULL OR available_pieces_count <= total_dispatched_pieces)
);

-- Create indexes for vendor_inventory table performance
CREATE INDEX idx_vendor_inventory_vendor_id 
    ON vendor_inventory(vendor_id) WHERE deleted = false;

CREATE INDEX idx_vendor_inventory_original_heat_id 
    ON vendor_inventory(original_heat_id) WHERE deleted = false;

CREATE INDEX idx_vendor_inventory_raw_material_product_id 
    ON vendor_inventory(raw_material_product_id) WHERE deleted = false;

CREATE INDEX idx_vendor_inventory_heat_number 
    ON vendor_inventory(heat_number) WHERE deleted = false;

CREATE INDEX idx_vendor_inventory_created_at 
    ON vendor_inventory(created_at) WHERE deleted = false;

-- Index for finding available inventory
CREATE INDEX idx_vendor_inventory_available_quantity 
    ON vendor_inventory(vendor_id, available_quantity) 
    WHERE deleted = false AND is_in_pieces = false AND available_quantity > 0;

CREATE INDEX idx_vendor_inventory_available_pieces 
    ON vendor_inventory(vendor_id, available_pieces_count) 
    WHERE deleted = false AND is_in_pieces = true AND available_pieces_count > 0;

-- Add comments for documentation
COMMENT ON TABLE vendor_inventory IS 'Tracks inventory dispatched to vendors separately from tenant inventory';
COMMENT ON COLUMN vendor_inventory.vendor_id IS 'The vendor who has this inventory';
COMMENT ON COLUMN vendor_inventory.original_heat_id IS 'Reference to the original heat from tenant inventory';
COMMENT ON COLUMN vendor_inventory.raw_material_product_id IS 'Reference to the raw material product';
COMMENT ON COLUMN vendor_inventory.heat_number IS 'Copy of heat number for reference';
COMMENT ON COLUMN vendor_inventory.total_dispatched_quantity IS 'Total quantity originally dispatched to vendor';
COMMENT ON COLUMN vendor_inventory.available_quantity IS 'Current available quantity at vendor';
COMMENT ON COLUMN vendor_inventory.is_in_pieces IS 'Whether this inventory is tracked in pieces or quantity';
COMMENT ON COLUMN vendor_inventory.total_dispatched_pieces IS 'Total pieces originally dispatched to vendor';
COMMENT ON COLUMN vendor_inventory.available_pieces_count IS 'Current available pieces at vendor';

-- Create sequences for transaction tables
CREATE SEQUENCE IF NOT EXISTS vendor_inventory_transactions_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS vendor_inventory_transaction_items_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Create vendor_inventory_transactions table
CREATE TABLE vendor_inventory_transactions (
    id BIGINT NOT NULL DEFAULT nextval('vendor_inventory_transactions_sequence'),
    tenant_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    transaction_date_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remarks TEXT,
    total_quantity_transferred DOUBLE PRECISION,
    total_pieces_transferred INTEGER,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_vendor_inventory_transactions PRIMARY KEY (id),
    CONSTRAINT fk_vendor_inventory_transactions_tenant FOREIGN KEY (tenant_id) 
        REFERENCES tenant(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vendor_inventory_transactions_vendor FOREIGN KEY (vendor_id) 
        REFERENCES vendor(id) ON DELETE RESTRICT,
    
    -- Check constraint for valid transaction types
    CONSTRAINT chk_vendor_inventory_transactions_type 
        CHECK (transaction_type IN ('TRANSFER_TO_VENDOR', 'RETURN_FROM_VENDOR')),
    
    -- Check constraints for non-negative values
    CONSTRAINT chk_vendor_inventory_transactions_total_quantity_positive 
        CHECK (total_quantity_transferred IS NULL OR total_quantity_transferred >= 0),
    CONSTRAINT chk_vendor_inventory_transactions_total_pieces_positive 
        CHECK (total_pieces_transferred IS NULL OR total_pieces_transferred >= 0)
);

-- Create vendor_inventory_transaction_items table
CREATE TABLE vendor_inventory_transaction_items (
    id BIGINT NOT NULL DEFAULT nextval('vendor_inventory_transaction_items_sequence'),
    vendor_inventory_transaction_id BIGINT NOT NULL,
    heat_id BIGINT NOT NULL,
    quantity_transferred DOUBLE PRECISION,
    pieces_transferred INTEGER,
    heat_number VARCHAR(255),
    test_certificate_number VARCHAR(255),
    location VARCHAR(255),
    is_in_pieces BOOLEAN,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_vendor_inventory_transaction_items PRIMARY KEY (id),
    CONSTRAINT fk_vendor_inventory_transaction_items_transaction FOREIGN KEY (vendor_inventory_transaction_id) 
        REFERENCES vendor_inventory_transactions(id) ON DELETE CASCADE,
    CONSTRAINT fk_vendor_inventory_transaction_items_heat FOREIGN KEY (heat_id) 
        REFERENCES heat(id) ON DELETE RESTRICT,
    
    -- Check constraints for quantity consistency
    CONSTRAINT chk_vendor_inventory_transaction_items_quantity_consistency 
        CHECK (
            (is_in_pieces = false AND quantity_transferred IS NOT NULL AND pieces_transferred IS NULL) OR
            (is_in_pieces = true AND pieces_transferred IS NOT NULL AND quantity_transferred IS NULL) OR
            (is_in_pieces IS NULL)
        ),
    
    -- Check constraints for non-negative values
    CONSTRAINT chk_vendor_inventory_transaction_items_quantity_positive 
        CHECK (quantity_transferred IS NULL OR quantity_transferred >= 0),
    CONSTRAINT chk_vendor_inventory_transaction_items_pieces_positive 
        CHECK (pieces_transferred IS NULL OR pieces_transferred >= 0)
);

-- Create indexes for vendor_inventory_transactions table
CREATE INDEX idx_vendor_inventory_transactions_tenant_id 
    ON vendor_inventory_transactions(tenant_id);

CREATE INDEX idx_vendor_inventory_transactions_vendor_id 
    ON vendor_inventory_transactions(vendor_id);

CREATE INDEX idx_vendor_inventory_transactions_type 
    ON vendor_inventory_transactions(transaction_type);

CREATE INDEX idx_vendor_inventory_transactions_date_time 
    ON vendor_inventory_transactions(transaction_date_time DESC);

CREATE INDEX idx_vendor_inventory_transactions_created_at 
    ON vendor_inventory_transactions(created_at DESC);

-- Create indexes for vendor_inventory_transaction_items table
CREATE INDEX idx_vendor_inventory_transaction_items_transaction_id 
    ON vendor_inventory_transaction_items(vendor_inventory_transaction_id);

CREATE INDEX idx_vendor_inventory_transaction_items_heat_id 
    ON vendor_inventory_transaction_items(heat_id);

CREATE INDEX idx_vendor_inventory_transaction_items_heat_number 
    ON vendor_inventory_transaction_items(heat_number);

CREATE INDEX idx_vendor_inventory_transaction_items_created_at 
    ON vendor_inventory_transaction_items(created_at DESC);

-- Add comments for transaction tables
COMMENT ON TABLE vendor_inventory_transactions IS 'Tracks batch transactions for vendor inventory transfers and returns';
COMMENT ON COLUMN vendor_inventory_transactions.tenant_id IS 'The tenant this transaction belongs to';
COMMENT ON COLUMN vendor_inventory_transactions.vendor_id IS 'The vendor involved in this transaction';
COMMENT ON COLUMN vendor_inventory_transactions.transaction_type IS 'Type of transaction: TRANSFER_TO_VENDOR or RETURN_FROM_VENDOR';
COMMENT ON COLUMN vendor_inventory_transactions.transaction_date_time IS 'When the transaction occurred';
COMMENT ON COLUMN vendor_inventory_transactions.remarks IS 'Optional remarks about the transaction';
COMMENT ON COLUMN vendor_inventory_transactions.total_quantity_transferred IS 'Total quantity involved in this transaction';
COMMENT ON COLUMN vendor_inventory_transactions.total_pieces_transferred IS 'Total pieces involved in this transaction';

COMMENT ON TABLE vendor_inventory_transaction_items IS 'Individual heat items involved in vendor inventory transactions';
COMMENT ON COLUMN vendor_inventory_transaction_items.vendor_inventory_transaction_id IS 'Reference to the parent transaction';
COMMENT ON COLUMN vendor_inventory_transaction_items.heat_id IS 'Reference to the heat involved in this transaction item';
COMMENT ON COLUMN vendor_inventory_transaction_items.quantity_transferred IS 'Quantity transferred for this heat';
COMMENT ON COLUMN vendor_inventory_transaction_items.pieces_transferred IS 'Pieces transferred for this heat';
COMMENT ON COLUMN vendor_inventory_transaction_items.heat_number IS 'Historical copy of heat number';
COMMENT ON COLUMN vendor_inventory_transaction_items.test_certificate_number IS 'Historical copy of test certificate number';
COMMENT ON COLUMN vendor_inventory_transaction_items.location IS 'Historical copy of heat location';
COMMENT ON COLUMN vendor_inventory_transaction_items.is_in_pieces IS 'Historical copy of whether heat is tracked in pieces';

COMMIT; 