-- Sequence for buyer ID generation
CREATE SEQUENCE buyer_sequence START 1 INCREMENT BY 1;

CREATE TABLE buyer (
                       id BIGINT PRIMARY KEY DEFAULT nextval('buyer_sequence'),
                       buyer_name VARCHAR(255) NOT NULL,
                       address TEXT,
                       gstin_uin VARCHAR(15),
                       phone_number VARCHAR(15),
                       tenant_id BIGINT NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       deleted_at TIMESTAMP,
                       deleted BOOLEAN DEFAULT FALSE,
                       CONSTRAINT fk_buyer_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
                       CONSTRAINT unique_buyer_name_tenant_deleted UNIQUE (buyer_name, tenant_id, deleted)
);

CREATE INDEX idx_buyer_name ON buyer (buyer_name) WHERE deleted = false;
CREATE INDEX idx_buyer_tenant_id ON buyer (tenant_id) WHERE deleted = false;

-- Sequence for buyer entity ID generation
CREATE SEQUENCE buyer_entity_sequence START 1 INCREMENT BY 1;

CREATE TABLE buyer_entity (
                              id BIGINT PRIMARY KEY DEFAULT nextval('buyer_entity_sequence'),
                              buyer_entity_name VARCHAR(255) NOT NULL,
                              address TEXT,
                              gstin_uin VARCHAR(15),
                              phone_number VARCHAR(15),
                              is_billing_entity BOOLEAN DEFAULT FALSE,
                              is_shipping_entity BOOLEAN DEFAULT FALSE,
                              buyer_id BIGINT NOT NULL,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              deleted_at TIMESTAMP,
                              deleted BOOLEAN DEFAULT FALSE,
                              CONSTRAINT fk_buyer_entity_buyer FOREIGN KEY (buyer_id) REFERENCES buyer(id),
                              CONSTRAINT unique_entity_name_buyer_deleted UNIQUE (buyer_entity_name, buyer_id, deleted)
);

CREATE INDEX idx_buyer_entity_name ON buyer_entity (buyer_entity_name) WHERE deleted = false;
CREATE INDEX idx_buyer_entity_buyer_id ON buyer_entity (buyer_id) WHERE deleted = false;