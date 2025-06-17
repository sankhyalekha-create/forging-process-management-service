-- Sequence for InspectionHeat Table
CREATE SEQUENCE inspection_heat_sequence START WITH 1 INCREMENT BY 1;

-- Table InspectionHeat

CREATE TABLE inspection_heat (
                                id BIGINT PRIMARY KEY DEFAULT nextval('inspection_heat_sequence'),
                                inspection_batch_id BIGINT NOT NULL,
                                heat_id BIGINT NOT NULL,
                                pieces_used INT NOT NULL,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP,
                                deleted_at TIMESTAMP,
                                deleted BOOLEAN DEFAULT FALSE,
                                CONSTRAINT fk_inspection_heat_inspection_batch FOREIGN KEY (inspection_batch_id) REFERENCES inspection_batch (id),
                                CONSTRAINT fk_inspection_heat_heat FOREIGN KEY (heat_id) REFERENCES heat (id)
);

-- Index for InspectionHeat Table
CREATE INDEX idx_inspection_heat_heat_id ON inspection_heat (heat_id) where deleted=false;
CREATE INDEX idx_inspection_heat_inspection_batch_id ON inspection_heat (inspection_batch_id) where deleted=false; 