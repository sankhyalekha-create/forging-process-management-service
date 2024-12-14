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

-- Sequence for forging_line ID generation
create SEQUENCE forging_line_sequence START 1 INCREMENT BY 1;
-- Table: forging_line
CREATE TABLE forging_line (
                              id BIGINT NOT NULL PRIMARY KEY,
                              forging_line_name VARCHAR(255) NOT NULL UNIQUE,
                              forging_details VARCHAR(255),
                              forging_line_status VARCHAR(50) NOT NULL,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              deleted_at TIMESTAMP,
                              deleted BOOLEAN DEFAULT FALSE,
                              tenant_id BIGINT NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
                              CONSTRAINT fk_forging_line_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);


-- Index on forging_line_name
CREATE UNIQUE INDEX unique_idx_forging_line_name
    ON forging_line(forging_line_name)
    WHERE deleted = false;

-- Create sequence for ProcessedItem
CREATE SEQUENCE processed_item_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Create table ProcessedItem
CREATE TABLE processed_item (
                                id BIGINT DEFAULT nextval('processed_item_sequence') PRIMARY KEY,
                                expected_forge_pieces_count INT NOT NULL,
                                actual_forge_pieces_count INT,
                                item_id BIGINT NOT NULL,
                                item_status VARCHAR(50) NOT NULL,
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                deleted_at TIMESTAMP,
                                deleted BOOLEAN DEFAULT FALSE,
                                CONSTRAINT fk_processed_item_item FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE CASCADE
);

-- Create indexes for ProcessedItem
CREATE INDEX idx_processed_item_item_id ON processed_item (item_id);
CREATE INDEX idx_processed_item_item_status ON processed_item (item_status);


-- Create sequence for Forge
CREATE SEQUENCE forge_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Create table Forge
CREATE TABLE forge (
                       id BIGINT DEFAULT nextval('forge_sequence') PRIMARY KEY,
                       forge_traceability_number VARCHAR(255) NOT NULL UNIQUE,
                       processed_item_id BIGINT NOT NULL,
                       forging_line_id BIGINT NOT NULL,
                       forging_status VARCHAR(50) NOT NULL,
                       start_at TIMESTAMP,
                       end_at TIMESTAMP,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       deleted_at TIMESTAMP,
                       deleted BOOLEAN DEFAULT FALSE,
                       CONSTRAINT fk_forge_processed_item FOREIGN KEY (processed_item_id) REFERENCES processed_item(id) ON DELETE CASCADE,
                       CONSTRAINT fk_forge_forging_line FOREIGN KEY (forging_line_id) REFERENCES forging_line(id) ON DELETE CASCADE
);

-- Create indexes for Forge
CREATE INDEX idx_forge_forging_line_id ON forge (forging_line_id);
CREATE INDEX idx_forge_processed_item_id ON forge (processed_item_id);

-- Sequence for ForgeHeat Table
CREATE SEQUENCE forge_heat_sequence START WITH 1 INCREMENT BY 1;

-- Table ForgeHeat

CREATE TABLE forge_heat (
                            id BIGINT PRIMARY KEY DEFAULT nextval('forge_heat_sequence'),
                            forge_id BIGINT NOT NULL,
                            heat_id BIGINT NOT NULL,
                            heat_quantity_used DOUBLE PRECISION NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP,
                            deleted_at TIMESTAMP,
                            deleted BOOLEAN DEFAULT FALSE,
                            CONSTRAINT fk_forge_heat_forge FOREIGN KEY (forge_id) REFERENCES forge (id),
                            CONSTRAINT fk_forge_heat_heat FOREIGN KEY (heat_id) REFERENCES heat (id)
);


-- Indexes for Forge Table
CREATE INDEX idx_forge_forge_traceability_number ON forge (forge_traceability_number);
CREATE INDEX idx_forge_item_id ON forge (item_id);
CREATE INDEX idx_forge_forging_line_id ON forge (forging_line_id);

-- Index for ForgeHeat Table
CREATE INDEX idx_forge_heat_heat_id ON forge_heat (heat_id);
CREATE INDEX idx_forge_heat_forge_id ON forge_heat (forge_id);

-- Sequence for furnace ID generation
create SEQUENCE furnace_sequence START 1 INCREMENT BY 1;

-- Table: Furnace
CREATE TABLE furnace (
                         id BIGINT NOT NULL PRIMARY KEY,
                         furnace_name VARCHAR(255) NOT NULL,
                         furnace_capacity DOUBLE PRECISION NOT NULL DEFAULT 0.0,
                         furnace_location VARCHAR(255),
                         furnace_details VARCHAR(500),
                         furnace_status VARCHAR(50) NOT NULL, -- Enum: 'IDLE', 'IN_PROGRESS', 'COMPLETED'
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         deleted_at TIMESTAMP,
                         deleted BOOLEAN DEFAULT FALSE,
                         tenant_id BIGINT NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
                         CONSTRAINT fk_furnace_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);


-- Index on furnace_name
CREATE INDEX idx_furnace_name ON furnace(furnace_name);

-- Create Script for HeatTreatmentBatch and BatchItemSelection

-- Create Sequence for HeatTreatmentBatch
CREATE SEQUENCE heat_treatment_batch_sequence START WITH 1 INCREMENT BY 1;

-- Create Table HeatTreatmentBatch
CREATE TABLE heat_treatment_batch (
                                      id BIGINT PRIMARY KEY DEFAULT nextval('heat_treatment_batch_sequence'),
                                      total_weight DOUBLE PRECISION NOT NULL DEFAULT 0.0,
                                      furnace_id BIGINT NOT NULL,
                                      heat_treatment_batch_status VARCHAR(255) NOT NULL,
                                      lab_testing_report TEXT,
                                      lab_testing_status VARCHAR(255),
                                      start_at TIMESTAMP,
                                      end_at TIMESTAMP,
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      deleted_at TIMESTAMP,
                                      deleted BOOLEAN DEFAULT FALSE,
                                      CONSTRAINT fk_furnace FOREIGN KEY (furnace_id) REFERENCES furnace (id)
);

-- Create Sequence for BatchItemSelection
CREATE SEQUENCE batch_item_selection_sequence START WITH 1 INCREMENT BY 1;
-- Create Table BatchItemSelection
CREATE TABLE batch_item_selection (
                                      id BIGINT PRIMARY KEY DEFAULT nextval('batch_item_selection_sequence'),
                                      forge_id BIGINT NOT NULL,
                                      available_forged_pieces_count INTEGER NOT NULL,
                                      heat_treat_batch_pieces_count INTEGER,
                                      actual_heat_treat_batch_pieces_count INTEGER,
                                      heat_treatment_batch_id BIGINT NOT NULL,
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      deleted_at TIMESTAMP,
                                      deleted BOOLEAN DEFAULT FALSE,
                                      CONSTRAINT fk_forge FOREIGN KEY (forge_id) REFERENCES forge (id),
                                      CONSTRAINT fk_heat_treatment_batch FOREIGN KEY (heat_treatment_batch_id) REFERENCES heat_treatment_batch (id)
);

-- Indexes for Performance
CREATE INDEX idx_heat_treatment_batch_furnace_id ON heat_treatment_batch (furnace_id);
CREATE INDEX idx_batch_item_selection_forge_id ON batch_item_selection (forge_id);
CREATE INDEX idx_batch_item_selection_heat_treatment_batch_id ON batch_item_selection (heat_treatment_batch_id);

