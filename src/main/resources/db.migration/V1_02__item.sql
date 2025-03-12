-- Create sequence for Item entity
CREATE SEQUENCE item_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Create table for Item entity
CREATE TABLE item (
                      id BIGINT DEFAULT nextval('item_sequence') PRIMARY KEY,
                      item_name VARCHAR(255) NOT NULL,
                      item_code VARCHAR(255),
                      item_weight DOUBLE PRECISION NOT NULL,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      deleted_at TIMESTAMP,
                      deleted BOOLEAN DEFAULT FALSE,
                      tenant_id BIGINT NOT NULL,
                      CONSTRAINT fk_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id) ON DELETE CASCADE,
                      CONSTRAINT unique_item_name_tenant_id UNIQUE (item_name, tenant_id), -- Unique constraint for item name
                      CONSTRAINT unique_item_code_tenant_id UNIQUE (item_code, tenant_id) -- Unique constraint for item code
);

-- Index on tenant_id for faster lookup
CREATE INDEX idx_item_name_tenant_id
    ON item (item_name, tenant_id) where deleted=false; -- Index for the item name unique constraint

CREATE INDEX idx_item_code_tenant_id
    ON item (item_code, tenant_id) where deleted=false; -- Index for the item code unique constraint

CREATE INDEX idx_item_tenant_id ON item (tenant_id) where deleted=false;
CREATE INDEX idx_item_name ON item (item_name) where deleted=false;

-- Create Sequence for 'item_product' table
CREATE SEQUENCE item_product_sequence START 1 INCREMENT BY 1;

-- Create 'item_product' table
CREATE TABLE item_product (
                              id BIGINT PRIMARY KEY DEFAULT nextval('item_product_sequence'),
                              item_id BIGINT,
                              product_id BIGINT NOT NULL,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              deleted_at TIMESTAMP,
                              deleted BOOLEAN DEFAULT FALSE,
                              CONSTRAINT fk_item FOREIGN KEY (item_id) REFERENCES item (id) ON DELETE CASCADE,
                              CONSTRAINT fk_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE
);


-- Create index on 'item_product' table for 'item_id' and 'product_id' columns
CREATE INDEX idx_item_product_item_id ON item_product (item_id) where deleted=false;
CREATE INDEX idx_item_product_product_id ON item_product (product_id) where deleted=false;
CREATE INDEX idx_item_product_item_id_product_id ON item_product (item_id, product_id) where deleted=false;
