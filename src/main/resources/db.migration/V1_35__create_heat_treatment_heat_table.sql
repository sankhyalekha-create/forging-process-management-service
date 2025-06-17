-- Migration: Create heat_treatment_heat table
-- Description: Add support for tracking heat consumption in heat treatment batches
-- Author: System
-- Date: 2024-12-20

-- Create sequence for heat_treatment_heat table
CREATE SEQUENCE IF NOT EXISTS heat_treatment_heat_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Create heat_treatment_heat table
CREATE TABLE heat_treatment_heat (
    id BIGINT NOT NULL DEFAULT nextval('heat_treatment_heat_sequence'),
    heat_treatment_batch_id BIGINT NOT NULL,
    heat_id BIGINT NOT NULL,
    pieces_used INTEGER NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT pk_heat_treatment_heat PRIMARY KEY (id),
    CONSTRAINT fk_heat_treatment_heat_batch FOREIGN KEY (heat_treatment_batch_id) 
        REFERENCES heat_treatment_batch(id) ON DELETE CASCADE,
    CONSTRAINT fk_heat_treatment_heat_heat FOREIGN KEY (heat_id) 
        REFERENCES heat(id) ON DELETE RESTRICT,
    CONSTRAINT chk_heat_treatment_heat_pieces_used_positive CHECK (pieces_used > 0)
);

-- Create indexes for better query performance
CREATE INDEX idx_heat_treatment_heat_batch_id ON heat_treatment_heat(heat_treatment_batch_id);
CREATE INDEX idx_heat_treatment_heat_heat_id ON heat_treatment_heat(heat_id);
CREATE INDEX idx_heat_treatment_heat_deleted ON heat_treatment_heat(deleted);
CREATE INDEX idx_heat_treatment_heat_created_at ON heat_treatment_heat(created_at);

-- Create unique constraint to prevent duplicate heat usage in same batch
CREATE UNIQUE INDEX uk_heat_treatment_heat_batch_heat_deleted 
    ON heat_treatment_heat(heat_treatment_batch_id, heat_id, deleted) 
    WHERE deleted = FALSE;

-- Add comments for documentation
COMMENT ON TABLE heat_treatment_heat IS 'Tracks heat consumption for heat treatment batches, linking specific heats with quantities used';
COMMENT ON COLUMN heat_treatment_heat.id IS 'Primary key for heat treatment heat record';
COMMENT ON COLUMN heat_treatment_heat.heat_treatment_batch_id IS 'Foreign key reference to heat_treatment_batch table';
COMMENT ON COLUMN heat_treatment_heat.heat_id IS 'Foreign key reference to heat table';
COMMENT ON COLUMN heat_treatment_heat.pieces_used IS 'Number of pieces consumed from the heat for this heat treatment batch';
COMMENT ON COLUMN heat_treatment_heat.created_at IS 'Timestamp when the record was created';
COMMENT ON COLUMN heat_treatment_heat.updated_at IS 'Timestamp when the record was last updated (also used as version for optimistic locking)';
COMMENT ON COLUMN heat_treatment_heat.deleted_at IS 'Timestamp when the record was soft deleted';
COMMENT ON COLUMN heat_treatment_heat.deleted IS 'Soft delete flag';

-- Grant necessary permissions (adjust schema/user as needed)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON heat_treatment_heat TO forging_app_user;
-- GRANT USAGE, SELECT ON heat_treatment_heat_sequence TO forging_app_user; 