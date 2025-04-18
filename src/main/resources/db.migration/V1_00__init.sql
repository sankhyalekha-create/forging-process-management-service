CREATE SEQUENCE tenant_sequence START 1 INCREMENT BY 1;

CREATE TABLE Tenant (
                        id BIGINT PRIMARY KEY DEFAULT nextval('tenant_sequence'),
                        tenant_name VARCHAR(255) NOT NULL,
                        tenant_org_id VARCHAR(255) NOT NULL UNIQUE,
                        address VARCHAR(500),
                        phone_number VARCHAR(10),
                        gstin VARCHAR(15) UNIQUE,
                        email VARCHAR(255) UNIQUE,
                        other_details VARCHAR(1000),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        deleted_at TIMESTAMP,
                        deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_tenant_name ON tenant (tenant_name) where deleted=false;
CREATE INDEX idx_tenant_org_id ON tenant (tenant_org_id) where deleted=false;
CREATE INDEX idx_tenant_gstin ON tenant (gstin) where deleted=false;
CREATE INDEX idx_tenant_email ON tenant (email) where deleted=false;


-- Sequence for supplier ID generation
CREATE SEQUENCE supplier_sequence START 1 INCREMENT BY 1;

CREATE TABLE supplier (
                          id BIGINT PRIMARY KEY DEFAULT nextval('supplier_sequence'),
                          supplier_name VARCHAR(255) NOT NULL,
                          supplier_detail TEXT,
                          tenant_id BIGINT NOT NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          deleted_at TIMESTAMP,
                          deleted BOOLEAN DEFAULT FALSE,
                          CONSTRAINT fk_supplier_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
                          CONSTRAINT unique_supplier_name_tenant_deleted UNIQUE (supplier_name, tenant_id, deleted)
);

CREATE INDEX idx_supplier_name_tenant_id
    ON supplier (supplier_name, tenant_id) where deleted=false;

CREATE INDEX idx_supplier_supplier_name ON supplier (supplier_name) where deleted=false;

-- Sequence for product ID generation
CREATE SEQUENCE product_sequence START 1 INCREMENT BY 1;

CREATE TABLE product (
                         id BIGINT PRIMARY KEY DEFAULT nextval('product_sequence'),
                         product_name VARCHAR(255) NOT NULL,
                         product_code VARCHAR(255) NOT NULL UNIQUE,
                         unit_of_measurement VARCHAR(255) NOT NULL,
                         tenant_id BIGINT NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         deleted_at TIMESTAMP,
                         deleted BOOLEAN DEFAULT FALSE,
                         CONSTRAINT fk_product_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

CREATE INDEX idx_product_product_name_tenant ON product (product_name, tenant_id) where deleted=false;

CREATE UNIQUE INDEX unique_product_name_tenant_active
    ON product (product_name, tenant_id)
    WHERE deleted = false;

CREATE INDEX idx_product_product_code_tenant ON product (product_code, tenant_id) where deleted=false;

CREATE UNIQUE INDEX unique_product_code_tenant_active
    ON product (product_code, tenant_id)
    WHERE deleted = false;


CREATE TABLE product_supplier (
                                  product_id BIGINT,
                                  supplier_id BIGINT,
                                  PRIMARY KEY (product_id, supplier_id),
                                  CONSTRAINT fk_product FOREIGN KEY (product_id) REFERENCES product(id),
                                  CONSTRAINT fk_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id)
);

-- Sequence for RawMaterial ID generation
CREATE SEQUENCE raw_material_sequence START 1 INCREMENT BY 1;

-- RawMaterial Table
CREATE TABLE raw_material (
                              id BIGINT PRIMARY KEY DEFAULT nextval('raw_material_sequence'),
                              raw_material_invoice_date TIMESTAMP,
                              po_number VARCHAR(255),
                              raw_material_receiving_date TIMESTAMP NOT NULL,
                              raw_material_invoice_number VARCHAR(255) NOT NULL,
                              raw_material_total_quantity DOUBLE PRECISION,
                              raw_material_total_pieces INT,
                              unit_of_measurement VARCHAR(255) NOT NULL,
                              raw_material_hsn_code VARCHAR(255) NOT NULL,
                              raw_material_goods_description TEXT,
                              supplier_id BIGINT NOT NULL,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              deleted_at TIMESTAMP,
                              deleted BOOLEAN DEFAULT FALSE,
                              tenant_id BIGINT NOT NULL,
                              CONSTRAINT fk_raw_material_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
                              CONSTRAINT fk_raw_material_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id),
                              CONSTRAINT unique_raw_material_invoice_tenant_deleted UNIQUE (raw_material_invoice_number, tenant_id, deleted) -- Unique constraint
);

-- Indexes for RawMaterial Table
CREATE INDEX idx_raw_material_invoice_number_tenant_id
    ON raw_material (raw_material_invoice_number, tenant_id) where deleted=false; -- Index for the unique constraint
CREATE INDEX idx_raw_material_hsn_code ON raw_material (raw_material_hsn_code) where deleted=false;
CREATE INDEX idx_raw_material_tenant_id ON raw_material (tenant_id) where deleted=false;

-- Sequence for product ID generation
CREATE SEQUENCE raw_material_product_sequence START 1 INCREMENT BY 1;

CREATE TABLE raw_material_product (
                                      id BIGINT PRIMARY KEY DEFAULT nextval('raw_material_product_sequence'),
                                      raw_material_id BIGINT,
                                      product_id BIGINT NOT NULL,
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      deleted_at TIMESTAMP,
                                      deleted BOOLEAN DEFAULT FALSE,
                                      CONSTRAINT fk_raw_material_product_raw_material FOREIGN KEY (raw_material_id) REFERENCES raw_material(id),
                                      CONSTRAINT fk_raw_material_product_product FOREIGN KEY (product_id) REFERENCES product(id)

);

CREATE INDEX idx_raw_material_product_raw_material_id_product_id ON raw_material_product (raw_material_id, product_id) where deleted=false;



-- Sequence for Heat ID generation
CREATE SEQUENCE heat_sequence START 1 INCREMENT BY 1;

-- RawMaterialHeat Table
CREATE TABLE heat (
                      id BIGINT PRIMARY KEY DEFAULT nextval('heat_sequence'),
                      heat_number VARCHAR(255) NOT NULL,
                      heat_quantity DOUBLE PRECISION,
                      available_heat_quantity DOUBLE PRECISION,
                      is_in_pieces  BOOLEAN DEFAULT FALSE,
                      pieces_count INT,
                      available_pieces_count INT,
                      test_certificate_number VARCHAR(255) NOT NULL,
                      location TEXT,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      deleted_at TIMESTAMP,
                      deleted BOOLEAN DEFAULT FALSE,
                      raw_material_product_id BIGINT,
                      CONSTRAINT fk_heat_raw_material_product FOREIGN KEY (raw_material_product_id) REFERENCES raw_material_product(id)
);

-- Indexes for RawMaterialHeat Table
CREATE INDEX idx_heat_number ON heat (heat_number) where deleted=false;
CREATE INDEX idx_heat_raw_material_product_id ON heat (raw_material_product_id) where deleted=false;
