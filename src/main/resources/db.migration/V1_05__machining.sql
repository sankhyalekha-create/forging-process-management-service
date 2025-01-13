-- Sequence for Machine
CREATE SEQUENCE machine_sequence START WITH 1 INCREMENT BY 1;

-- Machine Table
CREATE TABLE machine (
                         id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('machine_sequence'),
                         machine_name VARCHAR(255) NOT NULL,
                         machine_location VARCHAR(255),
                         machine_details TEXT,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         deleted_at TIMESTAMP,
                         deleted BOOLEAN DEFAULT FALSE,
                         tenant_id BIGINT NOT NULL,
                         CONSTRAINT uq_machine_name_tenant UNIQUE (machine_name, tenant_id),
                         CONSTRAINT fk_machine_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

-- Index for Machine Table
CREATE INDEX idx_machine_name ON machine(machine_name) WHERE deleted = false;

-- Index on tenant_id for faster lookup
CREATE INDEX idx_machine_name_tenant_id
    ON machine (machine_name, tenant_id) WHERE deleted = false;

-- Sequence for MachineSet
CREATE SEQUENCE machine_set_sequence START WITH 1 INCREMENT BY 1;

-- MachineSet Table
CREATE TABLE machine_set (
                             id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('machine_set_sequence'),
                             machine_set_name VARCHAR(255) NOT NULL UNIQUE,
                             machine_set_description TEXT,
                             machine_set_status VARCHAR(50) NOT NULL,
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             deleted_at TIMESTAMP,
                             deleted BOOLEAN DEFAULT FALSE
);

-- Index for MachineSet Table
CREATE INDEX idx_machine_set_name ON machine_set(machine_set_name) WHERE deleted = false;

-- MachineSet-Machine Join Table
CREATE TABLE machine_set_machine (
                                     machine_set_id BIGINT NOT NULL,
                                     machine_id BIGINT NOT NULL,
                                     PRIMARY KEY (machine_set_id, machine_id),
                                     CONSTRAINT fk_machine_set FOREIGN KEY (machine_set_id) REFERENCES machine_set(id),
                                     CONSTRAINT fk_machine FOREIGN KEY (machine_id) REFERENCES machine(id)
);

-- Sequence for MachiningBatch
CREATE SEQUENCE machining_batch_sequence START WITH 1 INCREMENT BY 1;

-- MachiningBatch Table
CREATE TABLE machining_batch (
                                 id BIGINT PRIMARY KEY DEFAULT nextval('machining_batch_sequence'),
                                 machining_batch_number VARCHAR(255) NOT NULL UNIQUE,
                                 machine_set BIGINT NOT NULL,
                                 machining_batch_status VARCHAR(50) NOT NULL,
                                 machining_batch_type VARCHAR(50) NOT NULL,
                                 start_at TIMESTAMP,
                                 end_at TIMESTAMP,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 deleted_at TIMESTAMP,
                                 deleted BOOLEAN DEFAULT FALSE,
                                 CONSTRAINT fk_machine_set FOREIGN KEY (machine_set) REFERENCES machine_set(id)
);

-- Sequence for ProcessedItemMachiningBatch
CREATE SEQUENCE processed_item_machining_batch_sequence START WITH 1 INCREMENT BY 1;

-- ProcessedItemMachiningBatch Table
CREATE TABLE processed_item_machining_batch (
                                                id BIGINT PRIMARY KEY DEFAULT nextval('processed_item_machining_batch_sequence'),
                                                processed_item_id BIGINT NOT NULL,
                                                machining_batch_id BIGINT NOT NULL,
                                                item_status VARCHAR(50) NOT NULL,
                                                machining_batch_pieces_count INTEGER NOT NULL,
                                                actual_machining_batch_pieces_count INTEGER,
                                                reject_machining_batch_pieces_count INTEGER,
                                                rework_pieces_count INTEGER,
                                                initial_inspection_batch_pieces_count INTEGER,
                                                available_inspection_batch_pieces_count INTEGER,
                                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                deleted_at TIMESTAMP,
                                                deleted BOOLEAN DEFAULT FALSE,
                                                CONSTRAINT fk_processed_item FOREIGN KEY (processed_item_id) REFERENCES processed_item(id),
                                                CONSTRAINT fk_machining_batch FOREIGN KEY (machining_batch_id) REFERENCES machining_batch(id)
);

CREATE INDEX idx_processed_item_id_processed_item_machining_batch ON processed_item_machining_batch(processed_item_id) WHERE deleted = false;
CREATE INDEX idx_machining_batch_id_processed_item_machining_batch ON processed_item_machining_batch(machining_batch_id) WHERE deleted = false;
CREATE INDEX idx_available_inspection_batch_pieces_count_processed_item_machining_batch ON processed_item_machining_batch(available_inspection_batch_pieces_count) WHERE deleted = false;


-- Sequence for DailyMachiningBatch
CREATE SEQUENCE daily_machining_batch_sequence START WITH 1 INCREMENT BY 1;

-- DailyMachiningBatch Table
CREATE TABLE daily_machining_batch (
                                       id BIGINT PRIMARY KEY DEFAULT nextval('daily_machining_batch_sequence'),
                                       machining_batch_id BIGINT NOT NULL,
                                       daily_machining_batch_status VARCHAR(50) NOT NULL,
                                       operation_date DATE NOT NULL,
                                       start_date_time TIMESTAMP NOT NULL,
                                       end_date_time TIMESTAMP NOT NULL,
                                       completed_pieces_count INTEGER NOT NULL,
                                       rejected_pieces_count INTEGER NOT NULL,
                                       rework_pieces_count INTEGER NOT NULL,
                                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       deleted_at TIMESTAMP,
                                       deleted BOOLEAN DEFAULT FALSE,
                                       CONSTRAINT fk_daily_machining_batch FOREIGN KEY (machining_batch_id) REFERENCES machining_batch(id)
);

CREATE INDEX idx_machining_batch_id_daily_machining_batch ON daily_machining_batch(machining_batch_id) WHERE deleted = false;

