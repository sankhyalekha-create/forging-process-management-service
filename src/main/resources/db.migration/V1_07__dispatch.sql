-- Create sequence for DispatchBatch
CREATE SEQUENCE dispatch_batch_sequence
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

-- Create DispatchBatch table
CREATE TABLE dispatch_batch (
                                id BIGINT PRIMARY KEY DEFAULT nextval('dispatch_batch_sequence'),
                                dispatch_batch_number VARCHAR(255) NOT NULL,
                                dispatch_batch_status VARCHAR(50) NOT NULL,
                                dispatch_created_at TIMESTAMP,
                                dispatch_ready_at TIMESTAMP,
                                dispatched_at TIMESTAMP,
                                packagingType VARCHAR(50),
                                packagingQuantity INT,
                                perPackagingQuantity INT,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                deleted_at TIMESTAMP,
                                deleted BOOLEAN DEFAULT FALSE,
                                tenant_id BIGINT NOT NULL,
                                CONSTRAINT uq_dispatch_batch_number_tenant_deleted UNIQUE (dispatch_batch_number, tenant_id, deleted),
                                CONSTRAINT fk_dispatch_batch_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

-- Create index on dispatch_batch_number for faster lookups
CREATE INDEX idx_dispatch_batch_number ON dispatch_batch(dispatch_batch_number);
CREATE INDEX idx_dispatch_batch_status ON dispatch_batch(dispatch_batch_status);
CREATE INDEX idx_dispatched_at ON dispatch_batch(dispatched_at);

-- Create sequence for ProcessedItemDispatchBatch
CREATE SEQUENCE processed_item_dispatch_batch_sequence
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

-- Create ProcessedItemDispatchBatch table
CREATE TABLE processed_item_dispatch_batch (
                                               id BIGINT PRIMARY KEY DEFAULT nextval('processed_item_dispatch_batch_sequence'),
                                               dispatch_batch_id BIGINT NOT NULL,
                                               processed_item_id BIGINT NOT NULL,
                                               total_dispatch_pieces_count INT,
                                               item_status VARCHAR(50) NOT NULL,
                                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                               deleted_at TIMESTAMP,
                                               deleted BOOLEAN DEFAULT FALSE,
                                               CONSTRAINT fk_processed_item_dispatch_batch_dispatch_batch FOREIGN KEY (dispatch_batch_id) REFERENCES dispatch_batch(id) ON DELETE CASCADE,
                                               CONSTRAINT fk_processed_item_dispatch_batch_processed_item FOREIGN KEY (processed_item_id) REFERENCES processed_item(id)
);

CREATE INDEX idx_total_dispatch_pieces_count_processed_item_dispatch_batch ON processed_item_dispatch_batch(total_dispatch_pieces_count);
CREATE INDEX idx_item_status_processed_item_dispatch_batch ON processed_item_dispatch_batch(item_status);

-- Ensure one-to-one relationship between DispatchBatch and ProcessedItemDispatchBatch
ALTER TABLE processed_item_dispatch_batch
    ADD CONSTRAINT uq_processed_item_dispatch_batch_dispatch_batch UNIQUE (dispatch_batch_id);

-- Create sequence for DispatchProcessedItemInspection
CREATE SEQUENCE dispatch_processed_item_inspection_sequence
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

-- Create DispatchProcessedItemInspection table (Join Table)
CREATE TABLE dispatch_processed_item_inspection (
                                                    id BIGINT PRIMARY KEY DEFAULT nextval('dispatch_processed_item_inspection_sequence'),
                                                    dispatch_batch_id BIGINT NOT NULL,
                                                    processed_item_inspection_batch_id BIGINT NOT NULL,
                                                    dispatched_pieces_count INT NOT NULL,
                                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                                    deleted_at TIMESTAMP,
                                                    deleted BOOLEAN DEFAULT FALSE,

    -- Foreign keys
                                                    CONSTRAINT fk_dispatch_processed_item_inspection_dispatch_batch
                                                        FOREIGN KEY (dispatch_batch_id) REFERENCES dispatch_batch(id) ON DELETE CASCADE,

                                                    CONSTRAINT fk_dispatch_processed_item_inspection_processed_item_inspection_batch
                                                        FOREIGN KEY (processed_item_inspection_batch_id) REFERENCES processed_item_inspection_batch(id) ON DELETE CASCADE,

    -- Ensure uniqueness for a particular combination of dispatch_batch and processed_item_inspection_batch
                                                    CONSTRAINT uq_dispatch_processed_item_inspection UNIQUE (dispatch_batch_id, processed_item_inspection_batch_id)
);

-- Indexes for performance optimization
CREATE INDEX idx_dispatch_processed_item_inspection_dispatch_batch
    ON dispatch_processed_item_inspection(dispatch_batch_id);

CREATE INDEX idx_dispatch_processed_item_inspection_processed_item_inspection_batch
    ON dispatch_processed_item_inspection(processed_item_inspection_batch_id);

CREATE INDEX idx_dispatched_pieces_count
    ON dispatch_processed_item_inspection(dispatched_pieces_count);

ALTER TABLE processed_item_inspection_batch ADD COLUMN dispatched_pieces_count INT;
