-- Sequence for furnace ID generation
CREATE SEQUENCE furnace_sequence START 1 INCREMENT BY 1;

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
                         CONSTRAINT fk_furnace_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
                         CONSTRAINT uq_furnace_name_tenant_deleted UNIQUE (furnace_name, tenant_id, deleted)
);

-- Index on tenant_id for faster lookup
CREATE INDEX idx_furnace_name_tenant_id
    ON furnace (furnace_name, tenant_id) WHERE deleted = false;

-- Sequence for heat treatment batch ID generation
CREATE SEQUENCE heat_treatment_batch_sequence START WITH 1 INCREMENT BY 1;

-- Create Table HeatTreatmentBatch
CREATE TABLE heat_treatment_batch (
                                      id BIGINT PRIMARY KEY DEFAULT nextval('heat_treatment_batch_sequence'),
                                      heat_treatment_batch_number VARCHAR(255),
                                      total_weight DOUBLE PRECISION NOT NULL DEFAULT 0.0,
                                      furnace_id BIGINT NOT NULL,
                                      heat_treatment_batch_status VARCHAR(50) NOT NULL,  -- Enum: 'IDLE', 'IN_PROGRESS', 'COMPLETED'
                                      lab_testing_report TEXT,
                                      lab_testing_status VARCHAR(255),
                                      apply_at TIMESTAMP,
                                      start_at TIMESTAMP,
                                      end_at TIMESTAMP,
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      deleted_at TIMESTAMP,
                                      deleted BOOLEAN DEFAULT FALSE,
                                      tenant_id BIGINT NOT NULL,
                                      CONSTRAINT fk_furnace FOREIGN KEY (furnace_id) REFERENCES furnace (id),
                                      CONSTRAINT uk_heat_treatment_batch_number_tenant_deleted UNIQUE (heat_treatment_batch_number, tenant_id, deleted)
);

-- Index for furnace_id for faster lookup
CREATE INDEX idx_heat_treatment_batch_furnace_id ON heat_treatment_batch (furnace_id);

-- Index for heat treatment batch number for faster lookup
CREATE INDEX idx_heat_treatment_batch_number ON heat_treatment_batch (heat_treatment_batch_number);

-- Index for heat treatment batch number and tenant combination for faster lookup
CREATE INDEX idx_heat_treatment_batch_number_tenant ON heat_treatment_batch (heat_treatment_batch_number, tenant_id) WHERE deleted = false;

-- Sequence for processed item heat treatment batch ID generation
CREATE SEQUENCE processed_item_ht_batch_sequence START WITH 1 INCREMENT BY 1;

-- Create Table ProcessedItemHeatTreatmentBatch
CREATE TABLE processed_item_heat_treatment_batch (
                                                     id BIGINT PRIMARY KEY DEFAULT nextval('processed_item_ht_batch_sequence'),
                                                     processed_item_id BIGINT NOT NULL,
                                                     heat_treatment_batch_id BIGINT NOT NULL,
                                                     item_status VARCHAR(50) NOT NULL,  -- Enum: 'IDLE', 'IN_PROGRESS', 'COMPLETED'
                                                     heat_treat_batch_pieces_count INTEGER NOT NULL,
                                                     actual_heat_treat_batch_pieces_count INTEGER,
                                                     initial_machining_batch_pieces_count INTEGER,
                                                     available_machining_batch_pieces_count INTEGER,
                                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                     deleted_at TIMESTAMP,
                                                     deleted BOOLEAN DEFAULT FALSE,
                                                     CONSTRAINT fk_processed_item FOREIGN KEY (processed_item_id) REFERENCES processed_item (id),
                                                     CONSTRAINT fk_heat_treatment_batch FOREIGN KEY (heat_treatment_batch_id) REFERENCES heat_treatment_batch (id)
);

-- Index for processed_item_id for faster lookup
CREATE INDEX idx_processed_item_heat_treatment_batch_processed_item_id ON processed_item_heat_treatment_batch (processed_item_id);

-- Index for heat_treatment_batch_id for faster lookup
CREATE INDEX idx_processed_item_heat_treatment_batch_heat_treatment_batch_id ON processed_item_heat_treatment_batch (heat_treatment_batch_id);
