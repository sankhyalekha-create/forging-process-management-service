-- Create sequences for auto-generation of IDs
CREATE SEQUENCE IF NOT EXISTS forge_shift_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS forge_shift_heat_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Create forge_shift table
CREATE TABLE IF NOT EXISTS forge_shift (
    id BIGINT NOT NULL DEFAULT nextval('forge_shift_sequence'),
    forge_id BIGINT NOT NULL,
    start_date_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_date_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    actual_forged_pieces_count INTEGER,
    rejected_forge_pieces_count INTEGER DEFAULT 0,
    other_forge_rejections_kg DOUBLE PRECISION DEFAULT 0.0,
    rejection BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT pk_forge_shift PRIMARY KEY (id),
    CONSTRAINT fk_forge_shift_forge FOREIGN KEY (forge_id) REFERENCES forge(id),
    CONSTRAINT chk_forge_shift_start_before_end CHECK (start_date_time < end_date_time),
    CONSTRAINT chk_forge_shift_actual_pieces_positive CHECK (actual_forged_pieces_count IS NULL OR actual_forged_pieces_count >= 0),
    CONSTRAINT chk_forge_shift_rejected_pieces_positive CHECK (rejected_forge_pieces_count >= 0),
    CONSTRAINT chk_forge_shift_other_rejections_positive CHECK (other_forge_rejections_kg >= 0.0)
);

-- Create forge_shift_heat table
CREATE TABLE IF NOT EXISTS forge_shift_heat (
    id BIGINT NOT NULL DEFAULT nextval('forge_shift_heat_sequence'),
    forge_shift_id BIGINT NOT NULL,
    heat_id BIGINT NOT NULL,
    heat_quantity_used DOUBLE PRECISION NOT NULL,
    heat_pieces INTEGER NOT NULL,
    heat_quantity_used_in_rejected_pieces DOUBLE PRECISION DEFAULT 0.0,
    heat_quantity_used_in_other_rejections DOUBLE PRECISION DEFAULT 0.0,
    rejected_pieces INTEGER DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT pk_forge_shift_heat PRIMARY KEY (id),
    CONSTRAINT fk_forge_shift_heat_forge_shift FOREIGN KEY (forge_shift_id) REFERENCES forge_shift(id) ON DELETE CASCADE,
    CONSTRAINT fk_forge_shift_heat_heat FOREIGN KEY (heat_id) REFERENCES heat(id),
    CONSTRAINT chk_forge_shift_heat_quantity_positive CHECK (heat_quantity_used > 0.0),
    CONSTRAINT chk_forge_shift_heat_pieces_positive CHECK (heat_pieces >= 0),
    CONSTRAINT chk_forge_shift_heat_rejected_pieces_positive CHECK (rejected_pieces >= 0),
    CONSTRAINT chk_forge_shift_heat_rejected_quantity_positive CHECK (heat_quantity_used_in_rejected_pieces >= 0.0),
    CONSTRAINT chk_forge_shift_heat_other_rejections_positive CHECK (heat_quantity_used_in_other_rejections >= 0.0)
);

-- Create indexes for forge_shift table
-- Index for queries filtering by forge_id and deleted status
CREATE INDEX IF NOT EXISTS idx_forge_shift_forge_id_deleted 
    ON forge_shift(forge_id, deleted);

-- Index for queries ordering by end_date_time (used in latest shift queries)
CREATE INDEX IF NOT EXISTS idx_forge_shift_forge_id_deleted_end_time 
    ON forge_shift(forge_id, deleted, end_date_time DESC);

-- Index for queries filtering by deleted status
CREATE INDEX IF NOT EXISTS idx_forge_shift_deleted 
    ON forge_shift(deleted);

-- Index for time-based queries
CREATE INDEX IF NOT EXISTS idx_forge_shift_start_date_time 
    ON forge_shift(start_date_time);

CREATE INDEX IF NOT EXISTS idx_forge_shift_end_date_time 
    ON forge_shift(end_date_time);

-- Index for created_at timestamp queries
CREATE INDEX IF NOT EXISTS idx_forge_shift_created_at 
    ON forge_shift(created_at);

-- Composite index for common query patterns
CREATE INDEX IF NOT EXISTS idx_forge_shift_forge_deleted_created 
    ON forge_shift(forge_id, deleted, created_at DESC);

-- Create indexes for forge_shift_heat table
-- Index for queries filtering by forge_shift_id and deleted status
CREATE INDEX IF NOT EXISTS idx_forge_shift_heat_shift_id_deleted 
    ON forge_shift_heat(forge_shift_id, deleted);

-- Index for queries filtering by heat_id
CREATE INDEX IF NOT EXISTS idx_forge_shift_heat_heat_id 
    ON forge_shift_heat(heat_id);

-- Index for queries filtering by deleted status
CREATE INDEX IF NOT EXISTS idx_forge_shift_heat_deleted 
    ON forge_shift_heat(deleted);

-- Index for created_at timestamp queries
CREATE INDEX IF NOT EXISTS idx_forge_shift_heat_created_at 
    ON forge_shift_heat(created_at);

-- Composite index for heat tracking across shifts
CREATE INDEX IF NOT EXISTS idx_forge_shift_heat_heat_deleted_created 
    ON forge_shift_heat(heat_id, deleted, created_at DESC);

-- Add comments for documentation
COMMENT ON TABLE forge_shift IS 'Stores individual forging shifts within a forge process';
COMMENT ON COLUMN forge_shift.forge_id IS 'Reference to the parent forge';
COMMENT ON COLUMN forge_shift.start_date_time IS 'Start date and time of the forge shift';
COMMENT ON COLUMN forge_shift.end_date_time IS 'End date and time of the forge shift';
COMMENT ON COLUMN forge_shift.actual_forged_pieces_count IS 'Number of pieces actually forged in this shift';
COMMENT ON COLUMN forge_shift.rejected_forge_pieces_count IS 'Number of pieces rejected during this shift';
COMMENT ON COLUMN forge_shift.other_forge_rejections_kg IS 'Weight of other rejections in kg during this shift';
COMMENT ON COLUMN forge_shift.rejection IS 'Flag indicating if there were rejections in this shift';

COMMENT ON TABLE forge_shift_heat IS 'Stores heat materials used in individual forge shifts';
COMMENT ON COLUMN forge_shift_heat.forge_shift_id IS 'Reference to the parent forge shift';
COMMENT ON COLUMN forge_shift_heat.heat_id IS 'Reference to the heat material used';
COMMENT ON COLUMN forge_shift_heat.heat_quantity_used IS 'Quantity of heat material consumed in this shift';
COMMENT ON COLUMN forge_shift_heat.heat_pieces IS 'Number of pieces forged (not rejected) from this heat in this shift';
COMMENT ON COLUMN forge_shift_heat.heat_quantity_used_in_rejected_pieces IS 'Heat quantity used in rejected pieces';
COMMENT ON COLUMN forge_shift_heat.heat_quantity_used_in_other_rejections IS 'Heat quantity used in other rejections';
COMMENT ON COLUMN forge_shift_heat.rejected_pieces IS 'Number of rejected pieces from this heat in this shift';

-- Grant permissions (adjust as per your application user)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON forge_shift TO your_app_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON forge_shift_heat TO your_app_user;
-- GRANT USAGE, SELECT ON forge_shift_sequence TO your_app_user;
-- GRANT USAGE, SELECT ON forge_shift_heat_sequence TO your_app_user; 