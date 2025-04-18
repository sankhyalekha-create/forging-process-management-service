-- Add buyer_id column to dispatch_batch table
ALTER TABLE dispatch_batch
ADD COLUMN buyer_id BIGINT NOT NULL;
ALTER TABLE dispatch_batch
ADD COLUMN billing_entity_id BIGINT NOT NULL,
ADD COLUMN shipping_entity_id BIGINT NOT NULL;

-- Add foreign key constraint
ALTER TABLE dispatch_batch
ADD CONSTRAINT fk_dispatch_batch_buyer
FOREIGN KEY (buyer_id)
REFERENCES buyer(id);

ALTER TABLE dispatch_batch
ADD CONSTRAINT fk_dispatch_batch_billing_entity
FOREIGN KEY (billing_entity_id)
REFERENCES buyer_entity(id),
ADD CONSTRAINT fk_dispatch_batch_shipping_entity
FOREIGN KEY (shipping_entity_id)
REFERENCES buyer_entity(id);

-- Add index for better query performance
CREATE INDEX idx_dispatch_batch_buyer_id ON dispatch_batch(buyer_id); 
CREATE INDEX idx_dispatch_batch_billing_entity_id ON dispatch_batch(billing_entity_id);
CREATE INDEX idx_dispatch_batch_shipping_entity_id ON dispatch_batch(shipping_entity_id); 