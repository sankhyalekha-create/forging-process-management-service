CREATE SEQUENCE tenant_sequence START 1 INCREMENT BY 50;

CREATE TABLE Tenant (
                        id BIGINT NOT NULL PRIMARY KEY,
                        tenant_name VARCHAR(255) NOT NULL,
                        tenant_org_id VARCHAR(255) NOT NULL UNIQUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        deleted_at TIMESTAMP,
                        deleted BOOLEAN DEFAULT FALSE,
                        CONSTRAINT tenant_version_check CHECK (deleted IS NOT NULL)
);

CREATE INDEX idx_tenant_name ON tenant (tenant_name);
CREATE INDEX idx_tenant_org_id ON tenant (tenant_org_id);
