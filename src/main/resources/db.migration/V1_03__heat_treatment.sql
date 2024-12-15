-- Sequence for furnace ID generation
create SEQUENCE furnace_sequence START 1 INCREMENT BY 1;

-- Table: Furnace
CREATE TABLE furnace (
                         id BIGINT NOT NULL PRIMARY KEY,
                         furnace_name VARCHAR(255) NOT NULL UNIQUE,
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
CREATE UNIQUE INDEX unique_idx_furnace_name ON furnace(furnace_name) WHERE deleted = false;

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

-- Indexes for Performance
CREATE INDEX idx_heat_treatment_batch_furnace_id ON heat_treatment_batch (furnace_id);
