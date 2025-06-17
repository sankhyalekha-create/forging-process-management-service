-- Sequence for DispatchHeat Table
CREATE SEQUENCE dispatch_heat_sequence START WITH 1 INCREMENT BY 1;

-- Table DispatchHeat

CREATE TABLE dispatch_heat (
                                id BIGINT PRIMARY KEY DEFAULT nextval('dispatch_heat_sequence'),
                                dispatch_batch_id BIGINT NOT NULL,
                                heat_id BIGINT NOT NULL,
                                pieces_used INT NOT NULL,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP,
                                deleted_at TIMESTAMP,
                                deleted BOOLEAN DEFAULT FALSE,
                                CONSTRAINT fk_dispatch_heat_dispatch_batch FOREIGN KEY (dispatch_batch_id) REFERENCES dispatch_batch (id),
                                CONSTRAINT fk_dispatch_heat_heat FOREIGN KEY (heat_id) REFERENCES heat (id)
);

-- Index for DispatchHeat Table
CREATE INDEX idx_dispatch_heat_heat_id ON dispatch_heat (heat_id) where deleted=false;
CREATE INDEX idx_dispatch_heat_dispatch_batch_id ON dispatch_heat (dispatch_batch_id) where deleted=false; 