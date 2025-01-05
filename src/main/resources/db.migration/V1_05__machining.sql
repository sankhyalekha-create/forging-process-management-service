-- Create sequence for Machine
CREATE SEQUENCE machine_sequence START WITH 1 INCREMENT BY 1;

-- Create Machine table
CREATE TABLE machine (
                         id BIGINT NOT NULL PRIMARY KEY,
                         machine_name VARCHAR(255) NOT NULL,
                         machine_location VARCHAR(255),
                         machine_details TEXT,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         deleted_at TIMESTAMP,
                         deleted BOOLEAN DEFAULT FALSE,
                         tenant_id BIGINT NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
                         CONSTRAINT fk_machine_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
                         CONSTRAINT uq_machine_name_tenant UNIQUE (machine_name, tenant_id)
);

-- Create index for machine_name
CREATE INDEX idx_machine_name ON machine (machine_name);

-- Create sequence for MachineSet
CREATE SEQUENCE machine_set_sequence START WITH 1 INCREMENT BY 1;

-- Create MachineSet table
CREATE TABLE machine_set (
                             id BIGINT NOT NULL PRIMARY KEY,
                             machine_set_name VARCHAR(255) NOT NULL UNIQUE,
                             machine_set_description TEXT,
                             machine_set_status VARCHAR(50) NOT NULL,
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             deleted_at TIMESTAMP,
                             deleted BOOLEAN DEFAULT FALSE
);

-- Create index for machine_set_name
CREATE INDEX idx_machine_set_name ON machine_set (machine_set_name);

-- Create MachineSet-Machine join table
CREATE TABLE machine_set_machine (
                                     machine_set_id BIGINT NOT NULL,
                                     machine_id BIGINT NOT NULL,
                                     PRIMARY KEY (machine_set_id, machine_id),
                                     CONSTRAINT fk_machine_set FOREIGN KEY (machine_set_id) REFERENCES machine_set (id),
                                     CONSTRAINT fk_machine FOREIGN KEY (machine_id) REFERENCES machine (id)
);

-- Create sequences
CREATE SEQUENCE machining_batch_sequence START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE daily_machining_batch_detail_sequence START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- Create table for MachiningBatch
CREATE TABLE machining_batch (
                                 id BIGINT PRIMARY KEY DEFAULT nextval('machining_batch_sequence'),
                                 machining_batch_number VARCHAR(255) NOT NULL UNIQUE,
                                 processed_item_id BIGINT NOT NULL,
                                 machine_set BIGINT NOT NULL,
                                 machining_batch_status VARCHAR(50) NOT NULL,
                                 machining_batch_type VARCHAR(50) NOT NULL,
                                 applied_machining_batch_pieces_count INTEGER,
                                 actual_machining_batch_pieces_count INTEGER,
                                 reject_machining_batch_pieces_count INTEGER,
                                 rework_pieces_count INTEGER,
                                 start_at TIMESTAMP,
                                 end_at TIMESTAMP,
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 deleted_at TIMESTAMP,
                                 deleted BOOLEAN DEFAULT FALSE,
                                 CONSTRAINT fk_processed_item FOREIGN KEY (processed_item_id) REFERENCES processed_item(id),
                                 CONSTRAINT fk_machine_set FOREIGN KEY (machine_set) REFERENCES machine_set(id)
);

-- Create indexes for MachiningBatch
CREATE INDEX idx_machining_batch_number ON machining_batch (machining_batch_number);
CREATE INDEX idx_machine_set ON machining_batch (machine_set);

-- Create table for DailyMachiningBatchDetail
CREATE TABLE daily_machining_batch_detail (
                                              id BIGINT PRIMARY KEY DEFAULT nextval('daily_machining_batch_detail_sequence'),
                                              machining_batch_id BIGINT NOT NULL,
                                              operation_date DATE NOT NULL,
                                              start_date_time TIMESTAMP NOT NULL,
                                              end_date_time TIMESTAMP NOT NULL,
                                              completed_pieces_count INTEGER NOT NULL,
                                              rejected_pieces_count INTEGER NOT NULL,
                                              rework_pieces_count INTEGER NOT NULL,
                                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              deleted_at TIMESTAMP,
                                              deleted BOOLEAN DEFAULT FALSE,
                                              CONSTRAINT fk_machining_batch FOREIGN KEY (machining_batch_id) REFERENCES machining_batch(id)
);

-- Create indexes for DailyMachiningBatchDetail
CREATE INDEX idx_machining_batch_id ON daily_machining_batch_detail (machining_batch_id);
CREATE INDEX idx_operation_date ON daily_machining_batch_detail (operation_date);

-- Update ProcessedItem table to include new columns
ALTER TABLE processed_item
    ADD COLUMN machining_batch_id BIGINT,
    ADD COLUMN initial_machining_batch_pieces_count INTEGER,
    ADD COLUMN available_machining_batch_pieces_count INTEGER,
    ADD COLUMN initial_rework_machining_batch_pieces_count INTEGER,
    ADD COLUMN available_rework_machining_batch_pieces_count INTEGER,
    ADD CONSTRAINT fk_machining_batch FOREIGN KEY (machining_batch_id) REFERENCES machining_batch(id);

-- Create indexes for ProcessedItem
CREATE INDEX idx_machining_batch_id_processed_item ON processed_item (machining_batch_id);
CREATE INDEX idx_available_machining_batch_pieces_count_processed_item ON processed_item (available_machining_batch_pieces_count);
