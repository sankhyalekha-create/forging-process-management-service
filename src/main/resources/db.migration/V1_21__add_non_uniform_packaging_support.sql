-- Add use_uniform_packaging column to dispatch_batch table
ALTER TABLE dispatch_batch
ADD COLUMN use_uniform_packaging BOOLEAN DEFAULT TRUE;

-- Create dispatch_package table
CREATE SEQUENCE dispatch_package_sequence
  INCREMENT 1
  START 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  CACHE 1;

CREATE TABLE dispatch_package (
  id BIGINT NOT NULL,
  dispatch_batch_id BIGINT NOT NULL,
  packaging_type VARCHAR(255) NOT NULL,
  quantity_in_package INTEGER NOT NULL,
  package_number INTEGER,
  created_at TIMESTAMP WITHOUT TIME ZONE,
  updated_at TIMESTAMP WITHOUT TIME ZONE,
  deleted_at TIMESTAMP WITHOUT TIME ZONE,
  deleted BOOLEAN DEFAULT FALSE,
  
  CONSTRAINT pk_dispatch_package PRIMARY KEY (id),
  CONSTRAINT fk_dispatch_package_dispatch_batch FOREIGN KEY (dispatch_batch_id)
    REFERENCES dispatch_batch (id) ON DELETE CASCADE
);

-- Add index on dispatch_batch_id for better performance
CREATE INDEX idx_dispatch_package_dispatch_batch ON dispatch_package (dispatch_batch_id);

-- Add index on deleted flag for better performance when querying non-deleted packages
CREATE INDEX idx_dispatch_package_deleted ON dispatch_package (deleted);

-- Comment for the non-uniform packaging feature
COMMENT ON TABLE dispatch_package IS 'Stores individual packages within a dispatch batch, supporting non-uniform packaging quantities';
COMMENT ON COLUMN dispatch_batch.use_uniform_packaging IS 'Flag indicating whether all packages in this batch have the same quantity'; 