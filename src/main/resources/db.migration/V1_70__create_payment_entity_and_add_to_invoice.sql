-- ===================================
-- Payment Entity Creation and Invoice Enhancement
-- Version: V1_70
-- Description: Creates payment table for advanced payment tracking
--              Adds PARTIALLY_PAID status to invoices
--              Adds total_paid_amount tracking to invoices
--              Adds total_tds_amount_deducted tracking to invoices
-- ===================================

-- ===================================
-- Part 1: Create Payment Table
-- ===================================

CREATE TABLE payment (
    id BIGINT NOT NULL,
    invoice_id BIGINT NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    payment_date_time TIMESTAMP NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    payment_reference VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    recorded_by VARCHAR(100),
    payment_proof_path VARCHAR(500),
    tds_amount NUMERIC(15, 2) DEFAULT 0.00,
    tds_reference VARCHAR(100),
    notes VARCHAR(1000),
    tenant_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_payment PRIMARY KEY (id),
    CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id) REFERENCES invoice(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payment_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT,
    CONSTRAINT chk_payment_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_payment_tds_non_negative CHECK (tds_amount >= 0)
);

-- Create sequence for payment ID generation
CREATE SEQUENCE IF NOT EXISTS payment_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- ===================================
-- Part 2: Create Indexes for Performance
-- ===================================

CREATE INDEX idx_payment_invoice ON payment(invoice_id);
CREATE INDEX idx_payment_date_time ON payment(payment_date_time);
CREATE INDEX idx_payment_status ON payment(status);
CREATE INDEX idx_payment_tenant ON payment(tenant_id);
CREATE INDEX idx_payment_deleted ON payment(deleted);

-- ===================================
-- Part 3: Add Comments for Documentation
-- ===================================

COMMENT ON TABLE payment IS 'Payment tracking table for invoice payments - supports partial payments, TDS, and payment history';
COMMENT ON COLUMN payment.id IS 'Unique payment identifier';
COMMENT ON COLUMN payment.invoice_id IS 'Reference to the invoice this payment is for';
COMMENT ON COLUMN payment.amount IS 'Payment amount in rupees';
COMMENT ON COLUMN payment.payment_date_time IS 'Date and time when payment was received';
COMMENT ON COLUMN payment.payment_method IS 'Payment method: CASH, CHEQUE, BANK_TRANSFER, UPI, NEFT_RTGS, DEMAND_DRAFT, CREDIT_CARD, DEBIT_CARD, OTHER';
COMMENT ON COLUMN payment.payment_reference IS 'Payment reference number (UTR, Cheque no., Transaction ID, etc.)';
COMMENT ON COLUMN payment.status IS 'Payment status: RECEIVED, PENDING_CLEARANCE, REVERSED, CANCELLED';
COMMENT ON COLUMN payment.recorded_by IS 'Name of person who recorded the payment';
COMMENT ON COLUMN payment.payment_proof_path IS 'Path to payment proof document (receipt, screenshot, etc.)';
COMMENT ON COLUMN payment.tds_amount IS 'Tax Deducted at Source (TDS) amount';
COMMENT ON COLUMN payment.tds_reference IS 'TDS certificate or reference number';
COMMENT ON COLUMN payment.notes IS 'Additional notes about the payment';
COMMENT ON COLUMN payment.tenant_id IS 'Tenant this payment belongs to';
COMMENT ON COLUMN payment.created_at IS 'Payment record creation timestamp';
COMMENT ON COLUMN payment.updated_at IS 'Payment record last update timestamp';
COMMENT ON COLUMN payment.deleted_at IS 'Soft delete timestamp';
COMMENT ON COLUMN payment.deleted IS 'Soft delete flag';

-- ===================================
-- Part 4: Add total_paid_amount to Invoice Table
-- ===================================

ALTER TABLE invoice
    ADD COLUMN total_paid_amount NUMERIC(15, 2) DEFAULT 0.00;

COMMENT ON COLUMN invoice.total_paid_amount IS 'Total amount paid against this invoice (sum of all RECEIVED payments)';

-- ===================================
-- Part 5: Add total_tds_amount_deducted to Invoice Table
-- ===================================

ALTER TABLE invoice
    ADD COLUMN total_tds_amount_deducted NUMERIC(15, 2) DEFAULT 0.00 NOT NULL;

COMMENT ON COLUMN invoice.total_tds_amount_deducted IS 'Cumulative TDS (Tax Deducted at Source) amount across all RECEIVED payments for this invoice';

-- ===================================
-- Part 6: Create Indexes on Invoice Payment Tracking Columns
-- ===================================

CREATE INDEX idx_invoice_total_paid_amount ON invoice(total_paid_amount);
CREATE INDEX idx_invoice_tds_amount ON invoice(total_tds_amount_deducted);

-- ===================================
-- Part 7: Update Invoice Status Documentation (add PARTIALLY_PAID)
-- ===================================
-- Note: PARTIALLY_PAID status is now available in InvoiceStatus enum
-- No database changes needed - enum values are application-level

COMMENT ON COLUMN invoice.status IS 'Invoice status: DRAFT, GENERATED, SENT, PARTIALLY_PAID, PAID, CANCELLED';

