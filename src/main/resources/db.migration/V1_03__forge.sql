-- Sequence for forging_line ID generation
create SEQUENCE forging_line_sequence START 1 INCREMENT BY 1;

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
                              CONSTRAINT fk_forging_line_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
                              CONSTRAINT uq_forging_line_name_tenant UNIQUE (forging_line_name, tenant_id)
);


-- Index on tenant_id for faster lookup
CREATE INDEX idx_forging_line_name_tenant_id
    ON forging_line (forging_line_name, tenant_id) where deleted=false; -- Index for the unique constraint


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
                               expected_forge_pieces_count INTEGER NOT NULL,
                               actual_forge_pieces_count INTEGER,
                               available_forge_pieces_count_for_heat INTEGER,
                               item_id BIGINT NOT NULL,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               deleted_at TIMESTAMP,
                               deleted BOOLEAN DEFAULT FALSE,
                               CONSTRAINT fk_item FOREIGN KEY (item_id) REFERENCES item (id)
);

-- Create indexes for ProcessedItem
CREATE INDEX idx_processed_item_item_id ON processed_item (item_id) where deleted=false;


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
                       forge_traceability_number VARCHAR(255) UNIQUE,
                       processed_item_id BIGINT NOT NULL,
                       forging_line_id BIGINT NOT NULL,
                       forging_status VARCHAR(50) NOT NULL,
                       apply_at TIMESTAMP,
                       start_at TIMESTAMP,
                       end_at TIMESTAMP,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       deleted_at TIMESTAMP,
                       deleted BOOLEAN DEFAULT FALSE,
                       tenant_id BIGINT NOT NULL,
                       CONSTRAINT fk_forge_processed_item FOREIGN KEY (processed_item_id) REFERENCES processed_item(id) ON DELETE CASCADE,
                       CONSTRAINT fk_forge_forging_line FOREIGN KEY (forging_line_id) REFERENCES forging_line(id) ON DELETE CASCADE
);

-- Indexes for Forge Table
CREATE INDEX idx_forge_forge_traceability_number ON forge (forge_traceability_number);
CREATE INDEX idx_forge_processed_item_id ON forge (processed_item_id) where deleted=false;
CREATE INDEX idx_forge_forging_line_id ON forge (forging_line_id) where deleted=false;

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


-- Index for ForgeHeat Table
CREATE INDEX idx_forge_heat_heat_id ON forge_heat (heat_id) where deleted=false;
CREATE INDEX idx_forge_heat_forge_id ON forge_heat (forge_id) where deleted=false;
