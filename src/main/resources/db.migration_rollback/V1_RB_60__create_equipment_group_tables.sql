-- Rollback script for V1_60__create_equipment_group_tables.sql
-- This script undoes the creation of equipment group tables and related objects

-- Drop the partial unique index for active equipment groups
DROP INDEX IF EXISTS uk_equipment_group_name_tenant_active;

-- Drop the unique constraint on equipment_group_gauge
ALTER TABLE equipment_group_gauge DROP CONSTRAINT IF EXISTS uk_equipment_group_gauge_tenant;

-- Drop performance indexes
DROP INDEX IF EXISTS idx_equipment_group_gauge_tenant;
DROP INDEX IF EXISTS idx_equipment_group_gauge_gauge;
DROP INDEX IF EXISTS idx_equipment_group_gauge_group;
DROP INDEX IF EXISTS idx_equipment_group_name;

-- Drop foreign key constraints
ALTER TABLE equipment_group_gauge DROP CONSTRAINT IF EXISTS fk_equipment_group_gauge_tenant;
ALTER TABLE equipment_group_gauge DROP CONSTRAINT IF EXISTS fk_equipment_group_gauge_gauge;
ALTER TABLE equipment_group_gauge DROP CONSTRAINT IF EXISTS fk_equipment_group_gauge_group;
ALTER TABLE equipment_group DROP CONSTRAINT IF EXISTS fk_equipment_group_tenant;

-- Drop tables (order matters due to foreign key relationships)
DROP TABLE IF EXISTS equipment_group_gauge;
DROP TABLE IF EXISTS equipment_group;

-- Drop sequences
DROP SEQUENCE IF EXISTS equipment_group_gauge_sequence;
DROP SEQUENCE IF EXISTS equipment_group_sequence;
