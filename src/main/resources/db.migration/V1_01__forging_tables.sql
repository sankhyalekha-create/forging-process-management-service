-- Table: Furnace
CREATE TABLE furnace (
                         id BIGINT PRIMARY KEY,
                         furnance_name VARCHAR(255) NOT NULL,
                         furnance_capacity FLOAT NOT NULL,
                         furnance_details VARCHAR(1000),
                         furnance_status TEXT NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         deleted_at TIMESTAMP
);

-- Index on furnance_name
CREATE INDEX idx_furnance_name ON furnace(furnance_name);

-- Table: forging_line
CREATE TABLE forging_line (
                              id BIGINT PRIMARY KEY,
                              forging_line_name VARCHAR(255) NOT NULL,
                              forging_details VARCHAR(1000),
                              forging_status TEXT NOT NULL,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              deleted_at TIMESTAMP
);

-- Index on forging_line_name
CREATE INDEX idx_forging_line_name ON forging_line(forging_line_name);

-- Table: forge_tracebility
CREATE TABLE forge_tracebility (
                                   id BIGINT PRIMARY KEY,
                                   heat_id BIGINT NOT NULL,
                                   heat_id_quantity_used FLOAT NOT NULL,
                                   start_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   end_at TIMESTAMP,
                                   forging_line_id BIGINT NOT NULL REFERENCES forging_line(id) ON DELETE CASCADE,
                                   forge_piece_weight FLOAT NOT NULL,
                                   actual_forge_count INTEGER,
                                   forging_status TEXT NOT NULL,
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   deleted_at TIMESTAMP,
                                   CONSTRAINT fk_forge_tracebility_forging_line FOREIGN KEY (forging_line_id) REFERENCES forging_line(id)
);

-- Index on heat_id
CREATE INDEX idx_forge_tracebility_heat_id ON forge_tracebility(heat_id);

-- Index on forging_line_id
CREATE INDEX idx_forge_tracebility_forging_line_id ON forge_tracebility(forging_line_id);

-- Table: heat_treatment_batch
CREATE TABLE heat_treatment_batch (
                                      id BIGINT PRIMARY KEY,
                                      forge_tracebility_id BIGINT NOT NULL REFERENCES forge_tracebility(id) ON DELETE CASCADE,
                                      start_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      end_at TIMESTAMP,
                                      furnance_id BIGINT NOT NULL REFERENCES Furnace(id) ON DELETE CASCADE,
                                      heat_treatment_batch_status TEXT NOT NULL,
                                      lab_testing_report VARCHAR(255),
                                      lab_testing_status VARCHAR(50),
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      deleted_at TIMESTAMP,
                                      CONSTRAINT fk_heat_treatment_batch_forge_tracebility FOREIGN KEY (forge_tracebility_id) REFERENCES forge_tracebility(id)
);

-- Index on heat_treatment_batch_status
CREATE INDEX idx_heat_treatment_batch_status ON heat_treatment_batch(heat_treatment_batch_status);
