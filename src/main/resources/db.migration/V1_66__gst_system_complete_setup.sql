BEGIN;

-- Sequence for invoice ID generation
CREATE SEQUENCE invoice_sequence START 1 INCREMENT BY 1;

-- Invoice table
CREATE TABLE invoice (
                         id BIGINT PRIMARY KEY DEFAULT nextval('invoice_sequence'),
                         invoice_number VARCHAR(50) NOT NULL,
                         invoice_date TIMESTAMP NOT NULL,
                         invoice_type VARCHAR(20) DEFAULT 'TAX_INVOICE',
                         dispatch_batch_id BIGINT,
                         original_invoice_id BIGINT,
                         order_id BIGINT,
                         customer_po_number VARCHAR(50),
                         customer_po_date DATE,
                         recipient_buyer_entity_id BIGINT REFERENCES buyer_entity(id),
                         recipient_vendor_entity_id BIGINT REFERENCES vendor_entity(id),
                         place_of_supply VARCHAR(50) NOT NULL,
                         is_inter_state BOOLEAN DEFAULT FALSE,
                         transportation_mode VARCHAR(20) DEFAULT 'ROAD',
                         transportation_distance INTEGER,
                         transporter_name VARCHAR(200),
                         transporter_id VARCHAR(15),
                         vehicle_number VARCHAR(20),
                         dispatch_date TIMESTAMP,
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
                         CONSTRAINT fk_invoice_original FOREIGN KEY (original_invoice_id) REFERENCES invoice(id),
                         CONSTRAINT chk_invoice_single_recipient CHECK (
                             (recipient_buyer_entity_id IS NOT NULL AND recipient_vendor_entity_id IS NULL) OR
                             (recipient_buyer_entity_id IS NULL AND recipient_vendor_entity_id IS NOT NULL)
                             ),
                         CONSTRAINT chk_invoice_type CHECK (invoice_type IN ('TAX_INVOICE', 'BILL_OF_SUPPLY', 'EXPORT_INVOICE', 'REVISED_INVOICE', 'CREDIT_NOTE', 'DEBIT_NOTE')),
                         CONSTRAINT chk_invoice_status CHECK (status IN ('DRAFT', 'GENERATED', 'SENT', 'PAID', 'PARTIALLY_PAID', 'CANCELLED', 'OVERDUE')),
                         CONSTRAINT chk_invoice_transportation_mode CHECK (transportation_mode IN ('ROAD', 'RAIL', 'AIR', 'SHIP'))
);

-- Sequence for invoice line item ID generation
CREATE SEQUENCE invoice_line_item_sequence START 1 INCREMENT BY 1;

-- Invoice Line Item table
CREATE TABLE invoice_line_item (
                                   id BIGINT PRIMARY KEY DEFAULT nextval('invoice_line_item_sequence'),
                                   invoice_id BIGINT NOT NULL,
                                   line_number INTEGER NOT NULL,
                                   item_name VARCHAR(500) NOT NULL,
                                   work_type VARCHAR(100),
                                   hsn_code VARCHAR(10),
                                   quantity DECIMAL(15,3) NOT NULL,
                                   unit_of_measurement VARCHAR(10) DEFAULT 'PCS',
                                   unit_price DECIMAL(15,2) NOT NULL,
                                   taxable_value DECIMAL(15,2) NOT NULL,
                                   cgst_rate DECIMAL(5,2) DEFAULT 0,
                                   cgst_amount DECIMAL(15,2) DEFAULT 0,
                                   sgst_rate DECIMAL(5,2) DEFAULT 0,
                                   sgst_amount DECIMAL(15,2) DEFAULT 0,
                                   igst_rate DECIMAL(5,2) DEFAULT 0,
                                   igst_amount DECIMAL(15,2) DEFAULT 0,
                                   discount_percentage DECIMAL(5,2) DEFAULT 0,
                                   discount_amount DECIMAL(15,2) DEFAULT 0,
                                   line_total DECIMAL(15,2) NOT NULL,
                                   item_workflow_id BIGINT,
                                   processed_item_dispatch_batch_id BIGINT,
                                   tenant_id BIGINT NOT NULL,
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   CONSTRAINT fk_invoice_line_item_invoice FOREIGN KEY (invoice_id) REFERENCES invoice(id) ON DELETE CASCADE,
                                   CONSTRAINT fk_invoice_line_item_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
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
                                   default_cgst_rate DECIMAL(5,2) DEFAULT 0,
                                   default_sgst_rate DECIMAL(5,2) DEFAULT 0,
                                   default_igst_rate DECIMAL(5,2) DEFAULT 0,
                                   is_active BOOLEAN DEFAULT TRUE,
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   deleted_at TIMESTAMP,
                                   deleted BOOLEAN DEFAULT FALSE,
                                   CONSTRAINT fk_gst_configuration_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);


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


ALTER TABLE dispatch_batch ADD CONSTRAINT chk_dispatch_transportation_mode CHECK (transportation_mode IN ('ROAD', 'RAIL', 'AIR', 'SHIP', 'OTHER'));
ALTER TABLE dispatch_batch ADD CONSTRAINT chk_dispatch_transportation_distance CHECK (transportation_distance IS NULL OR transportation_distance > 0);


CREATE INDEX idx_invoice_dispatch_batch ON invoice(dispatch_batch_id);
CREATE INDEX idx_invoice_order ON invoice(order_id);
CREATE INDEX idx_invoice_tenant_status ON invoice(tenant_id, status);
CREATE INDEX idx_invoice_date ON invoice(invoice_date);
CREATE INDEX idx_invoice_recipient_buyer ON invoice(recipient_buyer_entity_id);
CREATE INDEX idx_invoice_recipient_vendor ON invoice(recipient_vendor_entity_id);
CREATE INDEX idx_invoice_deleted ON invoice(deleted);

CREATE INDEX idx_invoice_line_item_invoice ON invoice_line_item(invoice_id);
CREATE INDEX idx_invoice_line_item_hsn ON invoice_line_item(hsn_code);
CREATE INDEX idx_invoice_line_item_work_type ON invoice_line_item(work_type);
CREATE INDEX idx_invoice_line_item_item_workflow ON invoice_line_item(item_workflow_id);

CREATE INDEX idx_gst_configuration_tenant ON gst_configuration(tenant_id);
CREATE INDEX idx_gst_configuration_gstin ON gst_configuration(company_gstin);
CREATE INDEX idx_gst_configuration_deleted ON gst_configuration(deleted);

-- Dispatch batch GST indexes
CREATE INDEX idx_dispatch_batch_gstin ON dispatch_batch(gstin);
CREATE INDEX idx_dispatch_batch_hsn_code ON dispatch_batch(hsn_code);
CREATE INDEX idx_dispatch_batch_requires_eway_bill ON dispatch_batch(requires_eway_bill);


COMMENT ON TABLE invoice IS 'Tax invoice for sales with pricing and tax details';
COMMENT ON TABLE invoice_line_item IS 'Itemized line entries for invoice with GST-compliant details';
COMMENT ON TABLE gst_configuration IS 'Tenant-specific GST configuration and settings';

-- Invoice order reference fields
COMMENT ON COLUMN invoice.order_id IS 'Reference to the associated order for traceability and reporting';
COMMENT ON COLUMN invoice.customer_po_number IS 'Customer''s Purchase Order number for reference and reconciliation';
COMMENT ON COLUMN invoice.customer_po_date IS 'Date of the customer''s Purchase Order';

-- Invoice transportation fields
COMMENT ON COLUMN invoice.transportation_mode IS 'Mode of transportation for goods (for E-Way Bill)';
COMMENT ON COLUMN invoice.transportation_distance IS 'Distance in kilometers for transportation (for E-Way Bill)';
COMMENT ON COLUMN invoice.transporter_name IS 'Name of the transporter';
COMMENT ON COLUMN invoice.transporter_id IS 'GSTIN of the transporter';
COMMENT ON COLUMN invoice.vehicle_number IS 'Vehicle registration number';
COMMENT ON COLUMN invoice.dispatch_date IS 'Date and time when goods were dispatched';

-- Dispatch batch GST fields
COMMENT ON COLUMN dispatch_batch.gstin IS 'GSTIN of the supplier/manufacturer';
COMMENT ON COLUMN dispatch_batch.hsn_code IS 'HSN code for the dispatched goods';
COMMENT ON COLUMN dispatch_batch.transportation_mode IS 'Mode of transportation for the goods';
COMMENT ON COLUMN dispatch_batch.transportation_distance IS 'Distance in kilometers for transportation';
COMMENT ON COLUMN dispatch_batch.requires_eway_bill IS 'Whether this dispatch requires E-Way Bill';
COMMENT ON COLUMN dispatch_batch.eway_bill_threshold_met IS 'Whether dispatch value meets E-Way Bill threshold';

COMMIT;
