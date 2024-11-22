CREATE SEQUENCE tenant_sequence START 1 INCREMENT BY 1;

CREATE TABLE Tenant (
                        id BIGINT PRIMARY KEY DEFAULT nextval('tenant_sequence'),
                        tenant_name VARCHAR(255) NOT NULL,
                        tenant_org_id VARCHAR(255) NOT NULL UNIQUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        deleted_at TIMESTAMP,
                        deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_tenant_name ON tenant (tenant_name);
CREATE INDEX idx_tenant_org_id ON tenant (tenant_org_id);


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
                          CONSTRAINT fk_supplier_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

CREATE UNIQUE INDEX unique_supplier_name_active
    ON supplier (supplier_name)
    WHERE deleted = false;

CREATE INDEX idx_supplier_supplier_name ON supplier (supplier_name);

-- Sequence for product ID generation
CREATE SEQUENCE product_sequence START 1 INCREMENT BY 1;

CREATE TABLE product (
                         id BIGINT PRIMARY KEY DEFAULT nextval('product_sequence'),
                         product_name VARCHAR(255) NOT NULL,
                         product_code VARCHAR(255) NOT NULL UNIQUE,
                         product_sku VARCHAR(255) NOT NULL UNIQUE,
                         unit_of_measurement VARCHAR(255) NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         deleted_at TIMESTAMP,
                         deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_product_product_name ON product (product_name);

CREATE UNIQUE INDEX unique_product_name_active
    ON product (product_name)
    WHERE deleted = false;

CREATE UNIQUE INDEX unique_product_code_active
    ON product (product_code)
    WHERE deleted = false;

CREATE UNIQUE INDEX unique_product_sku_active
    ON product (product_sku)
    WHERE deleted = false;

CREATE TABLE product_supplier (
                                  product_id BIGINT,
                                  supplier_id BIGINT,
                                  PRIMARY KEY (product_id, supplier_id),
                                  CONSTRAINT fk_product FOREIGN KEY (product_id) REFERENCES product(id),
                                  CONSTRAINT fk_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id)
);

-- Sequence for product ID generation
CREATE SEQUENCE item_sequence START 1 INCREMENT BY 1;

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
                              supplier_id BIGINT NOT NULL,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              deleted_at TIMESTAMP,
                              deleted BOOLEAN DEFAULT FALSE,
                              tenant_id BIGINT NOT NULL,
                              CONSTRAINT fk_raw_material_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
                              CONSTRAINT fk_raw_material_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id)
);

-- Indexes for RawMaterial Table
CREATE INDEX idx_raw_material_invoice_number ON raw_material (raw_material_invoice_number);
CREATE INDEX idx_raw_material_hsn_code ON raw_material (raw_material_hsn_code);
CREATE INDEX idx_raw_material_tenant_id ON raw_material (tenant_id);

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

CREATE INDEX idx_raw_material_product_raw_material_id_product_id ON raw_material_product (raw_material_id, product_id);



-- Sequence for Heat ID generation
CREATE SEQUENCE heat_sequence START 1 INCREMENT BY 1;

-- RawMaterialHeat Table
CREATE TABLE heat (
                      id BIGINT PRIMARY KEY DEFAULT nextval('heat_sequence'),
                      heat_number VARCHAR(255) NOT NULL,
                      heat_quantity DOUBLE PRECISION NOT NULL,
                      available_heat_quantity DOUBLE PRECISION NOT NULL,
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
CREATE INDEX idx_heat_number ON heat (heat_number);
CREATE INDEX idx_heat_raw_material_product_id ON heat (raw_material_product_id);

-- 1. Create Sequence for 'item' table
CREATE SEQUENCE item_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- 2. Create 'item' table
CREATE TABLE item (
                      id BIGINT PRIMARY KEY DEFAULT nextval('item_sequence'),
                      item_name VARCHAR(255) NOT NULL,
                      status VARCHAR(50) NOT NULL,
                      item_weight DOUBLE PRECISION NOT NULL,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      deleted_at TIMESTAMP,
                      deleted BOOLEAN DEFAULT FALSE,
                      tenant_id BIGINT NOT NULL
);

-- 3. Create Sequence for 'item_product' table
CREATE SEQUENCE item_product_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- 4. Create 'item_product' table
CREATE TABLE item_product (
                              id BIGINT PRIMARY KEY DEFAULT nextval('item_product_sequence'),
                              item_id BIGINT NOT NULL,
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

-- Sequence for furnace ID generation
create SEQUENCE furnace_sequence START 1 INCREMENT BY 1;

-- Table: Furnace
CREATE TABLE furnace (
                         id BIGINT NOT NULL PRIMARY KEY,
                         furnace_name VARCHAR(255) NOT NULL,
                         furnace_capacity FLOAT NOT NULL,
                         furnace_location VARCHAR(255),
                         furnace_details VARCHAR(500),
                         furnace_status VARCHAR(50) NOT NULL, -- Enum: 'IDLE', 'IN_PROGRESS', 'COMPLETED'
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         deleted_at TIMESTAMP,
                         deleted BOOLEAN DEFAULT FALSE,
                         tenant_id BIGINT NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
                         CONSTRAINT fk_furnace_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);


-- Index on furnace_name
CREATE INDEX idx_furnace_name ON furnace(furnace_name);

-- Sequence for forging_line ID generation
create SEQUENCE forging_line_sequence START 1 INCREMENT BY 1;
-- Table: forging_line
CREATE TABLE forging_line (
                              id BIGINT NOT NULL PRIMARY KEY,
                              forging_line_name VARCHAR(255) NOT NULL,
                              forging_details VARCHAR(255),
                              forging_status VARCHAR(50) NOT NULL, -- Enum: 'IDLE', 'IN_PROGRESS', 'COMPLETED'
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              deleted_at TIMESTAMP,
                              deleted BOOLEAN DEFAULT FALSE,
                              tenant_id BIGINT NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
                              CONSTRAINT fk_forging_line_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);


-- Index on forging_line_name
CREATE INDEX idx_forging_line_name ON forging_line(forging_line_name);

-- Sequence for forge_traceability ID generation
create SEQUENCE forge_traceability_sequence START 1 INCREMENT BY 1;

-- Table: forge_traceability
CREATE TABLE forge_traceability (
                                    id BIGINT NOT NULL PRIMARY KEY,
                                    heat_id BIGINT NOT NULL,
                                    heat_id_quantity_used FLOAT NOT NULL,
                                    start_at TIMESTAMP,
                                    end_at TIMESTAMP,
                                    forging_line_id BIGINT NOT NULL REFERENCES forging_line(id) ON DELETE CASCADE,
                                    forge_piece_weight FLOAT NOT NULL,
                                    actual_forge_count INT,
                                    forging_status VARCHAR(50) NOT NULL, -- Enum: 'IDLE', 'IN_PROGRESS', 'COMPLETED'
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    deleted_at TIMESTAMP,
                                    deleted BOOLEAN DEFAULT FALSE,
                                    CONSTRAINT fk_forge_traceability_forging_line FOREIGN KEY (forging_line_id) REFERENCES forging_line(id)
);

-- Index on heat_id
CREATE INDEX idx_forge_traceability_heat_id ON forge_traceability(heat_id);

-- Index on forging_line_id
CREATE INDEX idx_forge_traceability_forging_line_id ON forge_traceability(forging_line_id);

-- Sequence for heat_treatment_batch ID generation
create SEQUENCE heat_treatment_batch_sequence START 1 INCREMENT BY 1;
-- Table: heat_treatment_batch
CREATE TABLE heat_treatment_batch (
                                      id BIGINT NOT NULL PRIMARY KEY,
                                      furnace_id BIGINT NOT NULL,
                                      start_at TIMESTAMP NOT NULL,
                                      end_at TIMESTAMP,
                                      heat_treatment_batch_status TEXT NOT NULL,
                                      lab_testing_report VARCHAR(255),
                                      lab_testing_status VARCHAR(255),
                                      created_at TIMESTAMP,
                                      updated_at TIMESTAMP,
                                      deleted_at TIMESTAMP,
                                      deleted BOOLEAN DEFAULT FALSE,
                                      CONSTRAINT fk_heat_treatment_batch_furnace FOREIGN KEY (furnace_id) REFERENCES furnace(id)
);



-- Index on heat_treatment_batch_status
CREATE INDEX idx_heat_treatment_furnace_id ON heat_treatment_batch(furnace_id);

CREATE TABLE heat_treatment_batch_forge_traceability (
                                                         heat_treatment_batch_id BIGINT NOT NULL,
                                                         forge_traceability_id BIGINT NOT NULL,
                                                         PRIMARY KEY (heat_treatment_batch_id, forge_traceability_id),
                                                         CONSTRAINT fk_htb_forge_traceability_htb FOREIGN KEY (heat_treatment_batch_id) REFERENCES heat_treatment_batch(id),
                                                         CONSTRAINT fk_htb_forge_traceability_ft FOREIGN KEY (forge_traceability_id) REFERENCES forge_traceability(id)
);

