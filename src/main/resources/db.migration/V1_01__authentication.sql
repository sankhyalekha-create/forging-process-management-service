-- Sequence for User ID
CREATE SEQUENCE usr_sequence START WITH 1 INCREMENT BY 1;

-- Main User Table
CREATE TABLE "usr" (
                       id BIGINT PRIMARY KEY DEFAULT nextval('usr_sequence'),
                       username VARCHAR(255) NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       deleted_at TIMESTAMP,
                       deleted BOOLEAN DEFAULT FALSE,
                       tenant_id BIGINT NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
                       CONSTRAINT fk_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE SET NULL,
                       CONSTRAINT unique_username_tenant_deleted UNIQUE (username, tenant_id, deleted)
);

-- Roles Table (for @ElementCollection)
CREATE TABLE usr_roles (
                           usr_id BIGINT NOT NULL,
                           role VARCHAR(255) NOT NULL,
                           PRIMARY KEY (usr_id, role),
                           CONSTRAINT fk_usr_roles FOREIGN KEY (usr_id) REFERENCES "usr"(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_usr_username_tenant_id
    ON usr (username, tenant_id) where deleted=false; -- Index for the unique constraint

CREATE INDEX idx_usr_username ON "usr" (username) where deleted=false;
CREATE INDEX idx_usr_tenant_id ON "usr" (tenant_id) where deleted=false;
