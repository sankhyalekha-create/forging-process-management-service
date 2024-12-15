-- 1. Create Sequence for 'item' table
CREATE SEQUENCE item_sequence START 1 INCREMENT BY 1;

-- 2. Create 'item' table
CREATE TABLE item (
                      id BIGINT PRIMARY KEY DEFAULT nextval('item_sequence'),
                      item_name VARCHAR(255) NOT NULL,
                      item_code VARCHAR(255) NOT NULL,
                      item_weight DOUBLE PRECISION NOT NULL,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      deleted_at TIMESTAMP,
                      deleted BOOLEAN DEFAULT FALSE,
                      tenant_id BIGINT NOT NULL
);

-- 3. Create Sequence for 'item_product' table
CREATE SEQUENCE item_product_sequence START 1 INCREMENT BY 1;

-- 4. Create 'item_product' table
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

-- 5. Create index on 'item' table for 'item_name' column
CREATE INDEX idx_item_name ON item (item_name);

-- 6. Create index on 'item_product' table for 'item_id' and 'product_id' columns
CREATE INDEX idx_item_product_item_id ON item_product (item_id);
CREATE INDEX idx_item_product_product_id ON item_product (product_id);
CREATE INDEX idx_item_product_item_id_product_id ON item_product (item_id, product_id);
