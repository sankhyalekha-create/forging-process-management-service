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

-- Sequence for MachiningBatch
CREATE SEQUENCE machining_batch_sequence START WITH 1 INCREMENT BY 1;

-- Sequence for MachiningBatchPieceDetail
CREATE SEQUENCE piece_detail_sequence START WITH 1 INCREMENT BY 1;

-- Table: machining_batch
CREATE TABLE machining_batch (
                                 id BIGINT DEFAULT nextval('machining_batch_sequence') PRIMARY KEY,
                                 machining_batch_number VARCHAR(255) NOT NULL UNIQUE,
                                 processed_item_id BIGINT NOT NULL,
                                 machine_set BIGINT NOT NULL,
                                 machining_batch_status VARCHAR(50) NOT NULL,
                                 machining_batch_pieces_count INT,
                                 actual_machining_batch_pieces_count INT,
                                 reject_machining_batch_pieces_count INT,
                                 rework_pieces_count INT NOT NULL,
                                 start_at TIMESTAMP,
                                 end_at TIMESTAMP,
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 deleted_at TIMESTAMP,
                                 deleted BOOLEAN DEFAULT FALSE,
                                 CONSTRAINT fk_processed_item FOREIGN KEY (processed_item_id) REFERENCES processed_item(id) ON DELETE CASCADE,
                                 CONSTRAINT fk_machine_set FOREIGN KEY (machine_set) REFERENCES machine_set(id) ON DELETE CASCADE
);

-- Table: machining_batch_piece_detail
CREATE TABLE machining_batch_piece_detail (
                                              id BIGINT DEFAULT nextval('piece_detail_sequence') PRIMARY KEY,
                                              machining_batch_id BIGINT NOT NULL,
                                              piece_status VARCHAR(50) NOT NULL,
                                              quantity INT NOT NULL,
                                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              deleted_at TIMESTAMP,
                                              deleted BOOLEAN DEFAULT FALSE,
                                              CONSTRAINT fk_machining_batch FOREIGN KEY (machining_batch_id) REFERENCES machining_batch(id) ON DELETE CASCADE
);

-- Updated table: processed_item
ALTER TABLE processed_item
    ADD COLUMN machining_batch_id BIGINT,
ADD CONSTRAINT fk_machining_batch_in_processed_item FOREIGN KEY (machining_batch_id) REFERENCES machining_batch(id) ON DELETE SET NULL;



-- Index on machining_batch.machining_batch_status
CREATE INDEX idx_machining_batch_status ON machining_batch (machining_batch_status);

-- Index on machining_batch.machining_batch_number
CREATE INDEX idx_machining_batch_number ON machining_batch (machining_batch_number);

-- Index on machining_batch_piece_detail.piece_status
CREATE INDEX idx_piece_status ON machining_batch_piece_detail (piece_status);

-- Index on processed_item.machining_batch_id
CREATE INDEX idx_processed_item_machining_batch_id ON processed_item (machining_batch_id);
