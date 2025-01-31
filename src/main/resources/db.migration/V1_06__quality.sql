-- Sequence for Gauge
CREATE SEQUENCE gauge_sequence START WITH 1 INCREMENT BY 1;

-- Gauge Table
CREATE TABLE gauge (
                       id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('gauge_sequence'),
                       gauge_name VARCHAR(255) NOT NULL,
                       gauge_location VARCHAR(255),
                       gauge_details TEXT,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       deleted_at TIMESTAMP,
                       deleted BOOLEAN DEFAULT FALSE,
                       tenant_id BIGINT NOT NULL,
                       CONSTRAINT uq_gauge_name_tenant UNIQUE (gauge_name, tenant_id),
                       CONSTRAINT fk_gauge_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

-- Index for Gauge Table
CREATE INDEX idx_gauge_name ON gauge(gauge_name) WHERE deleted = false;

-- Index on tenant_id for faster lookup
CREATE INDEX idx_gauge_name_tenant_id
    ON gauge (gauge_name, tenant_id) WHERE deleted = false;

-- Create Sequence for InspectionBatch
CREATE SEQUENCE inspection_batch_sequence START WITH 1 INCREMENT BY 1;

-- Create InspectionBatch Table
CREATE TABLE inspection_batch (
                                  id BIGSERIAL PRIMARY KEY,
                                  inspection_batch_number VARCHAR NOT NULL UNIQUE,
                                  input_processed_item_machining_batch_id BIGINT REFERENCES processed_item_machining_batch(id) ON DELETE SET NULL,
                                  inspection_batch_status VARCHAR NOT NULL,
                                  start_at TIMESTAMP,
                                  end_at TIMESTAMP,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  deleted_at TIMESTAMP,
                                  deleted BOOLEAN DEFAULT FALSE,
                                  tenant_id BIGINT NOT NULL,CONSTRAINT uq_inspection_batch_number_tenant UNIQUE (inspection_batch_number, tenant_id),
                                  CONSTRAINT fk_inspection_batch_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

-- Create Sequence for ProcessedItemInspectionBatch
CREATE SEQUENCE processed_item_inspection_batch_sequence START WITH 1 INCREMENT BY 1;

-- Create ProcessedItemInspectionBatch Table
CREATE TABLE processed_item_inspection_batch (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 inspection_batch_id BIGINT NOT NULL UNIQUE REFERENCES inspection_batch(id) ON DELETE CASCADE,
                                                 processed_item_id BIGINT NOT NULL REFERENCES processed_item(id) ON DELETE CASCADE,
                                                 inspection_batch_pieces_count INT NOT NULL,
                                                 available_inspection_batch_pieces_count INT,
                                                 finished_inspection_batch_pieces_count INT,
                                                 reject_inspection_batch_pieces_count INT,
                                                 rework_pieces_count INT,
                                                 available_dispatch_pieces_count INT,
                                                 item_status VARCHAR(50) NOT NULL,
                                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                 deleted_at TIMESTAMP,
                                                 deleted BOOLEAN DEFAULT FALSE
);

-- Create Sequence for GaugeInspectionReport
CREATE SEQUENCE gauge_inspection_report_sequence START WITH 1 INCREMENT BY 1;

-- Create GaugeInspectionReport Table
CREATE TABLE gauge_inspection_report (
                                         id BIGSERIAL PRIMARY KEY,
                                         processed_item_inspection_batch_id BIGINT NOT NULL REFERENCES processed_item_inspection_batch(id) ON DELETE CASCADE,
                                         gauge_id BIGINT NOT NULL REFERENCES gauge(id) ON DELETE CASCADE,
                                         finished_pieces_count INT NOT NULL,
                                         rejected_pieces_count INT NOT NULL,
                                         rework_pieces_count INT NOT NULL,
                                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                         deleted_at TIMESTAMP,
                                         deleted BOOLEAN DEFAULT FALSE
);

-- Create Indexes for InspectionBatch Table
CREATE INDEX idx_inspection_batch_status ON inspection_batch(inspection_batch_status);
CREATE INDEX idx_inspection_batch_number ON inspection_batch(inspection_batch_number);

-- Create Indexes for ProcessedItemInspectionBatch Table
CREATE INDEX idx_processed_item_inspection_batch_item_status ON processed_item_inspection_batch(item_status);
CREATE INDEX idx_processed_item_inspection_batch_finished_inspection_batch_pieces_count
    ON processed_item_inspection_batch(finished_inspection_batch_pieces_count);
CREATE INDEX idx_processed_item_inspection_batch_reject_inspection_batch_pieces_count
    ON processed_item_inspection_batch(reject_inspection_batch_pieces_count);
CREATE INDEX idx_processed_item_inspection_batch_rework_pieces_count
    ON processed_item_inspection_batch(rework_pieces_count);

-- Create Indexes for GaugeInspectionReport Table
CREATE INDEX idx_gauge_inspection_report_finished_pieces_count ON gauge_inspection_report(finished_pieces_count);
CREATE INDEX idx_gauge_inspection_report_rejected_pieces_count ON gauge_inspection_report(rejected_pieces_count);
CREATE INDEX idx_gauge_inspection_report_rework_pieces_count ON gauge_inspection_report(rework_pieces_count);
