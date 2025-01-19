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
