-- Sequence for MachiningHeat Table
CREATE SEQUENCE machining_heat_sequence START WITH 1 INCREMENT BY 1;

-- Table MachiningHeat

CREATE TABLE machining_heat (
                                id BIGINT PRIMARY KEY DEFAULT nextval('machining_heat_sequence'),
                                machining_batch_id BIGINT NOT NULL,
                                heat_id BIGINT NOT NULL,
                                pieces_used INT NOT NULL,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP,
                                deleted_at TIMESTAMP,
                                deleted BOOLEAN DEFAULT FALSE,
                                CONSTRAINT fk_machining_heat_machining_batch FOREIGN KEY (machining_batch_id) REFERENCES machining_batch (id),
                                CONSTRAINT fk_machining_heat_heat FOREIGN KEY (heat_id) REFERENCES heat (id)
);

-- Index for MachiningHeat Table
CREATE INDEX idx_machining_heat_heat_id ON machining_heat (heat_id) where deleted=false;
CREATE INDEX idx_machining_heat_machining_batch_id ON machining_heat (machining_batch_id) where deleted=false;

