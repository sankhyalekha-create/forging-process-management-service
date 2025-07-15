-- Add PAN number field to buyer table
ALTER TABLE buyer
ADD COLUMN pan_number VARCHAR(10);

-- Add PAN number field to buyer_entity table
ALTER TABLE buyer_entity
ADD COLUMN pan_number VARCHAR(10);

-- Create indexes for the new PAN number fields for query performance
CREATE INDEX idx_buyer_pan_number ON buyer(pan_number) WHERE deleted = false;
CREATE INDEX idx_buyer_entity_pan_number ON buyer_entity(pan_number) WHERE deleted = false;

-- Add comments for PAN number columns
COMMENT ON COLUMN buyer.pan_number IS 'PAN number of the buyer';
COMMENT ON COLUMN buyer_entity.pan_number IS 'PAN number of the buyer entity'; 