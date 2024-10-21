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
                                    start_at TIMESTAMP NOT NULL,
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
