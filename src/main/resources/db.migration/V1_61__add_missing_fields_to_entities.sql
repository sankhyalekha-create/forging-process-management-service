-- Add missing fields to entities
-- Migration: V1_61__add_missing_fields_to_entities.sql

-- Add email, stateCode, and pincode to buyer table
ALTER TABLE buyer 
ADD COLUMN email VARCHAR(255),
ADD COLUMN state_code VARCHAR(2),
ADD COLUMN pincode VARCHAR(6);

-- Add email, stateCode, and pincode to buyer_entity table
ALTER TABLE buyer_entity 
ADD COLUMN email VARCHAR(255),
ADD COLUMN state_code VARCHAR(2),
ADD COLUMN pincode VARCHAR(6);

-- Add stateCode and pincode to tenant table
ALTER TABLE tenant 
ADD COLUMN state_code VARCHAR(2),
ADD COLUMN pincode VARCHAR(6);

-- Add address, email, stateCode, and pincode to supplier table
ALTER TABLE supplier 
ADD COLUMN address VARCHAR(500),
ADD COLUMN email VARCHAR(255),
ADD COLUMN state_code VARCHAR(2),
ADD COLUMN pincode VARCHAR(6);

-- Add email, stateCode, and pincode to vendor table
ALTER TABLE vendor 
ADD COLUMN email VARCHAR(255),
ADD COLUMN state_code VARCHAR(2),
ADD COLUMN pincode VARCHAR(6);

-- Add email, stateCode, and pincode to vendor_entity table
ALTER TABLE vendor_entity 
ADD COLUMN email VARCHAR(255),
ADD COLUMN state_code VARCHAR(2),
ADD COLUMN pincode VARCHAR(6);

-- Add comments for documentation
COMMENT ON COLUMN buyer.email IS 'Email address of the buyer';
COMMENT ON COLUMN buyer.state_code IS 'State code (2 digits) for GST jurisdiction';
COMMENT ON COLUMN buyer.pincode IS 'Pincode (6 digits) for address identification';

COMMENT ON COLUMN buyer_entity.email IS 'Email address of the buyer entity';
COMMENT ON COLUMN buyer_entity.state_code IS 'State code (2 digits) for GST jurisdiction';
COMMENT ON COLUMN buyer_entity.pincode IS 'Pincode (6 digits) for address identification';

COMMENT ON COLUMN tenant.state_code IS 'State code (2 digits) for GST jurisdiction';
COMMENT ON COLUMN tenant.pincode IS 'Pincode (6 digits) for address identification';

COMMENT ON COLUMN supplier.address IS 'Address of the supplier';
COMMENT ON COLUMN supplier.email IS 'Email address of the supplier';
COMMENT ON COLUMN supplier.state_code IS 'State code (2 digits) for GST jurisdiction';
COMMENT ON COLUMN supplier.pincode IS 'Pincode (6 digits) for address identification';

COMMENT ON COLUMN vendor.email IS 'Email address of the vendor';
COMMENT ON COLUMN vendor.state_code IS 'State code (2 digits) for GST jurisdiction';
COMMENT ON COLUMN vendor.pincode IS 'Pincode (6 digits) for address identification';

COMMENT ON COLUMN vendor_entity.email IS 'Email address of the vendor entity';
COMMENT ON COLUMN vendor_entity.state_code IS 'State code (2 digits) for GST jurisdiction';
COMMENT ON COLUMN vendor_entity.pincode IS 'Pincode (6 digits) for address identification';
