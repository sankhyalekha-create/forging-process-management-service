-- Add new fields to supplier table
ALTER TABLE supplier
    ADD COLUMN phone_number VARCHAR(15),
    ADD COLUMN pan_number VARCHAR(10),
    ADD COLUMN gstin_number VARCHAR(15);

-- Add indexes for the new fields
CREATE INDEX idx_supplier_phone_number ON supplier(phone_number);
CREATE INDEX idx_supplier_pan_number ON supplier(pan_number);
CREATE INDEX idx_supplier_gstin_number ON supplier(gstin_number);

-- Add comments for the new columns
COMMENT ON COLUMN supplier.phone_number IS 'Phone number of the supplier';
COMMENT ON COLUMN supplier.pan_number IS 'PAN number of the supplier';
COMMENT ON COLUMN supplier.gstin_number IS 'GSTIN number of the supplier'; 