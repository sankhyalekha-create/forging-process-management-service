-- Create equipment_group table
CREATE TABLE equipment_group (
  id BIGINT NOT NULL,
  group_name VARCHAR(255) NOT NULL,
  group_description TEXT,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6),
  deleted_at TIMESTAMP(6),
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  tenant_id BIGINT NOT NULL,
  CONSTRAINT pk_equipment_group PRIMARY KEY (id),
  CONSTRAINT fk_equipment_group_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

-- Create equipment_group_gauge junction table
CREATE TABLE equipment_group_gauge (
  id BIGINT NOT NULL,
  equipment_group_id BIGINT NOT NULL,
  gauge_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6),
  deleted_at TIMESTAMP(6),
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT pk_equipment_group_gauge PRIMARY KEY (id),
  CONSTRAINT fk_equipment_group_gauge_group FOREIGN KEY (equipment_group_id) REFERENCES equipment_group (id),
  CONSTRAINT fk_equipment_group_gauge_gauge FOREIGN KEY (gauge_id) REFERENCES gauge (id),
  CONSTRAINT fk_equipment_group_gauge_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

-- Create sequences
CREATE SEQUENCE equipment_group_sequence START 1;
CREATE SEQUENCE equipment_group_gauge_sequence START 1;

-- Create indexes for better performance
CREATE INDEX idx_equipment_group_name ON equipment_group (group_name);
CREATE INDEX idx_equipment_group_gauge_group ON equipment_group_gauge (equipment_group_id);
CREATE INDEX idx_equipment_group_gauge_gauge ON equipment_group_gauge (gauge_id);
CREATE INDEX idx_equipment_group_gauge_tenant ON equipment_group_gauge (tenant_id);

-- Create unique constraint to prevent duplicate gauge assignments within a group and tenant
ALTER TABLE equipment_group_gauge ADD CONSTRAINT uk_equipment_group_gauge_tenant UNIQUE (equipment_group_id, gauge_id, tenant_id);

-- Create partial unique index for active equipment groups with same name within tenant (prevents duplicates)
CREATE UNIQUE INDEX uk_equipment_group_name_tenant_active 
ON equipment_group (group_name, tenant_id) 
WHERE deleted = false;

-- Add comments explaining tenant isolation
COMMENT ON COLUMN equipment_group_gauge.tenant_id IS 'Tenant ID for data isolation - ensures equipment group and gauge belong to same tenant';
