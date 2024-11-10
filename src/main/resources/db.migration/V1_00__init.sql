CREATE SEQUENCE tenant_sequence START 1 INCREMENT BY 50;

CREATE TABLE Tenant (
                        id BIGINT NOT NULL PRIMARY KEY,
                        tenant_name VARCHAR(255) NOT NULL,
                        tenant_org_id VARCHAR(255) NOT NULL UNIQUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        deleted_at TIMESTAMP,
                        deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_tenant_name ON tenant (tenant_name);
CREATE INDEX idx_tenant_org_id ON tenant (tenant_org_id);


-- Sequence for RawMaterial ID generation
create SEQUENCE raw_material_sequence START 1 INCREMENT BY 1;

-- RawMaterial Table
CREATE TABLE raw_material (
                              id BIGINT PRIMARY KEY DEFAULT nextval('raw_material_sequence'),
                              raw_material_invoice_date TIMESTAMP,
                              po_number VARCHAR(255),
                              raw_material_receiving_date TIMESTAMP NOT NULL,
                              raw_material_invoice_number VARCHAR(255) NOT NULL UNIQUE,
                              raw_material_total_quantity DOUBLE PRECISION NOT NULL,
                              raw_material_hsn_code VARCHAR(255) NOT NULL,
                              raw_material_goods_description TEXT,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              deleted_at TIMESTAMP,
                              deleted BOOLEAN DEFAULT FALSE,
                              tenant_id BIGINT NOT NULL,
                              CONSTRAINT fk_raw_material_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

-- Indexes for RawMaterial Table
CREATE INDEX idx_raw_material_invoice_number ON raw_material (raw_material_invoice_number);
CREATE INDEX idx_raw_material_hsn_code ON raw_material (raw_material_hsn_code);
CREATE INDEX idx_raw_material_tenant_id ON raw_material (tenant_id);

-- Sequence for supplier ID generation
CREATE SEQUENCE supplier_sequence START 1 INCREMENT BY 1;

CREATE TABLE supplier (
                          id BIGINT PRIMARY KEY DEFAULT nextval('supplier_sequence'),
                          supplier_name VARCHAR(255) NOT NULL UNIQUE,
                          supplier_detail TEXT,
                          tenant_id BIGINT NOT NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          deleted_at TIMESTAMP,
                          deleted BOOLEAN DEFAULT FALSE,
                          CONSTRAINT fk_supplier_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

CREATE INDEX idx_supplier_supplier_name ON supplier (supplier_name);

-- Sequence for product ID generation
CREATE SEQUENCE product_sequence START 1 INCREMENT BY 1;

CREATE TABLE product (
                         id BIGINT PRIMARY KEY DEFAULT nextval('product_sequence'),
                         product_name VARCHAR(255) NOT NULL,
                         product_code VARCHAR(255) NOT NULL,
                         product_sku VARCHAR(255) NOT NULL,
                         unit_of_measurement VARCHAR(255) NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         deleted_at TIMESTAMP,
                         deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_product_product_name ON product (product_name);

CREATE TABLE product_supplier (
                                  product_id BIGINT,
                                  supplier_id BIGINT,
                                  PRIMARY KEY (product_id, supplier_id),
                                  CONSTRAINT fk_product FOREIGN KEY (product_id) REFERENCES product(id),
                                  CONSTRAINT fk_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id)
);

-- Sequence for product ID generation
CREATE SEQUENCE item_sequence START 1 INCREMENT BY 1;

CREATE TABLE item (
                      id BIGINT PRIMARY KEY DEFAULT nextval('item_sequence'),
                      item_code VARCHAR(255),
                      status VARCHAR(255),
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      deleted_at TIMESTAMP,
                      deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_item_item_code ON item (item_code);

-- Sequence for product ID generation
CREATE SEQUENCE item_product_sequence START 1 INCREMENT BY 1;

CREATE TABLE item_product (
                              id BIGINT PRIMARY KEY DEFAULT nextval('item_product_sequence'),
                              item_id BIGINT,
                              product_id BIGINT,
                              product_weight DOUBLE PRECISION,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              deleted_at TIMESTAMP,
                              deleted BOOLEAN DEFAULT FALSE,
                              CONSTRAINT fk_item_product_item FOREIGN KEY (item_id) REFERENCES item(id),
                              CONSTRAINT fk_item_product_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE INDEX idx_item_product_item_id_product_id ON item_product (item_id, product_id);

-- Sequence for product ID generation
CREATE SEQUENCE raw_material_product_sequence START 1 INCREMENT BY 1;

CREATE TABLE raw_material_product (
                                      id BIGINT PRIMARY KEY DEFAULT nextval('raw_material_product_sequence'),
                                      raw_material_id BIGINT NOT NULL,
                                      product_id BIGINT NOT NULL,
                                      supplier_id BIGINT NOT NULL,
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      deleted_at TIMESTAMP,
                                      deleted BOOLEAN DEFAULT FALSE,
                                      CONSTRAINT fk_raw_material_product_raw_material FOREIGN KEY (raw_material_id) REFERENCES raw_material(id),
                                      CONSTRAINT fk_raw_material_product_product FOREIGN KEY (product_id) REFERENCES product(id),
                                      CONSTRAINT fk_raw_material_product_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id)
);

CREATE INDEX idx_raw_material_product_raw_material_id_product_id ON raw_material_product (raw_material_id, product_id);



-- Sequence for Heat ID generation
CREATE SEQUENCE heat_sequence START 1 INCREMENT BY 1;

-- RawMaterialHeat Table
CREATE TABLE heat (
                                   id BIGINT PRIMARY KEY DEFAULT nextval('heat_sequence'),
                                   heat_number VARCHAR(255) NOT NULL,
                                   heat_quantity DOUBLE PRECISION NOT NULL,
                                   available_heat_quantity DOUBLE PRECISION NOT NULL,
                                   heat_test_certificate_number VARCHAR(255) NOT NULL,
                                   heat_location TEXT,
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   deleted_at TIMESTAMP,
                                   deleted BOOLEAN DEFAULT FALSE,
                                   raw_material_product_id BIGINT NOT NULL,
                                   CONSTRAINT fk_heat_raw_material_product FOREIGN KEY (raw_material_product_id) REFERENCES raw_material_product(id)
);

-- Indexes for RawMaterialHeat Table
CREATE INDEX idx_heat_number ON heat (heat_number);
CREATE INDEX idx_heat_raw_material_product_id ON heat (raw_material_product_id);
