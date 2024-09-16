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
                              id BIGINT NOT NULL PRIMARY KEY,
                              raw_material_receiving_date TIMESTAMP NOT NULL,
                              raw_material_invoice_number VARCHAR(255) NOT NULL,
                              raw_material_total_quantity REAL NOT NULL,
                              raw_material_input_code VARCHAR(255) NOT NULL,
                              raw_material_hsn_code VARCHAR(255) NOT NULL,
                              raw_material_goods_description TEXT,
                              tenant_id BIGINT NOT NULL,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              deleted_at TIMESTAMP,
                              deleted BOOLEAN DEFAULT FALSE,
                              UNIQUE (raw_material_invoice_number)
                              CONSTRAINT fk_raw_material_tenant FOREIGN KEY (tenant_id) REFERENCES Tenant(id));

-- Indexes for RawMaterial Table
CREATE INDEX idx_raw_material_invoice_number ON raw_material (raw_material_invoice_number);
CREATE INDEX idx_raw_material_input_code ON raw_material (raw_material_input_code);
CREATE INDEX idx_raw_material_hsn_code ON raw_material (raw_material_hsn_code);
CREATE INDEX idx_raw_material_tenant_id ON raw_material (tenant_id);


-- Sequence for RawMaterialHeat ID generation
CREATE SEQUENCE raw_material_heat_sequence START 1 INCREMENT BY 1;

-- RawMaterialHeat Table
CREATE TABLE raw_material_heat (
                                   id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('raw_material_heat_sequence'),
                                   heat_number VARCHAR(255) NOT NULL,
                                   heat_quantity REAL NOT NULL,
                                   raw_material_test_certificate_number VARCHAR(255) NOT NULL,
                                   bar_diameter VARCHAR(255) NOT NULL,
                                   raw_material_receiving_inspection_report_number VARCHAR(255) NOT NULL,
                                   raw_material_inspection_source VARCHAR(255),
                                   raw_material_location VARCHAR(255),
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   deleted_at TIMESTAMP,
                                   deleted BOOLEAN DEFAULT FALSE,
                                   raw_material_id BIGINT,
                                   CONSTRAINT fk_raw_material_heat_raw_material FOREIGN KEY (raw_material_id) REFERENCES raw_material(id)
);

-- Indexes for RawMaterialHeat Table
CREATE INDEX idx_raw_material_heat_number ON raw_material_heat (heat_number);
CREATE INDEX idx_raw_material_heat_certificate_number ON raw_material_heat (raw_material_test_certificate_number);
CREATE INDEX idx_raw_material_heat_receiving_report_number ON raw_material_heat (raw_material_receiving_inspection_report_number);
