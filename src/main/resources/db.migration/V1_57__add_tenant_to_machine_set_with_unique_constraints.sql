-- Migration: Add Tenant Relationship to MachineSet and Fix Uniqueness Constraints
-- Version: V1_57
-- Description: Complete migration to add tenant_id foreign key to machine_set table and ensure proper tenant-scoped uniqueness
-- This migration combines:
-- 1. Adding tenant_id column and populating it from machine/daily_machining_batch relationships
-- 2. Removing all conflicting global unique constraints
-- 3. Creating tenant-scoped unique constraint for machine_set_name + tenant_id combination
-- This allows same machine set names across different tenants and reuse after deletion within a tenant

BEGIN;

-- ===== ADD TENANT_ID COLUMN TO MACHINE_SET =====
-- First add the column as nullable
ALTER TABLE machine_set ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

-- Create index on tenant_id for performance
CREATE INDEX IF NOT EXISTS idx_machine_set_tenant ON machine_set (tenant_id);

-- Add foreign key constraint
ALTER TABLE machine_set ADD CONSTRAINT fk_machine_set_tenant 
    FOREIGN KEY (tenant_id) REFERENCES tenant(id);

-- ===== POPULATE TENANT_ID FROM ASSOCIATED MACHINES =====
-- Update existing machine sets with tenant_id based on their associated machines
-- This assumes that all machines in a machine set belong to the same tenant
UPDATE machine_set 
SET tenant_id = (
    SELECT DISTINCT m.tenant_id 
    FROM machine_set_machine msm 
    JOIN machine m ON msm.machine_id = m.id 
    WHERE msm.machine_set_id = machine_set.id 
    AND m.deleted = false
    LIMIT 1
)
WHERE tenant_id IS NULL 
AND id IN (
    SELECT DISTINCT msm.machine_set_id 
    FROM machine_set_machine msm 
    JOIN machine m ON msm.machine_id = m.id 
    WHERE m.deleted = false
);

-- Handle machine sets without any associated machines (edge case)
-- First, try to populate tenant_id from daily_machining_batch relationships
-- Some machine sets might not have machines but are referenced by daily_machining_batch
UPDATE machine_set 
SET tenant_id = (
    SELECT DISTINCT mb.tenant_id
    FROM daily_machining_batch dmb
    JOIN machining_batch mb ON dmb.machining_batch_id = mb.id
    WHERE dmb.machine_set_id = machine_set.id
    AND dmb.deleted = false
    AND mb.deleted = false
    LIMIT 1
)
WHERE tenant_id IS NULL 
AND id IN (
    SELECT DISTINCT dmb.machine_set_id 
    FROM daily_machining_batch dmb 
    WHERE dmb.deleted = false 
    AND dmb.machine_set_id IS NOT NULL
);

-- For any remaining machine sets without machines and without daily_machining_batch references,
-- assign them to the first available tenant (safest approach to avoid foreign key violations)
UPDATE machine_set 
SET tenant_id = (
    SELECT id FROM tenant 
    WHERE deleted = false 
    ORDER BY id 
    LIMIT 1
)
WHERE tenant_id IS NULL;

-- Final safety check - if there are still NULL values, handle appropriately
DO $$
DECLARE
    null_count INTEGER;
    tenant_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO null_count FROM machine_set WHERE tenant_id IS NULL;
    SELECT COUNT(*) INTO tenant_count FROM tenant WHERE deleted = false;
    
    IF tenant_count = 0 THEN
        RAISE EXCEPTION 'Cannot proceed: No active tenants found in the system. Please ensure at least one tenant exists before running this migration.';
    END IF;
    
    IF null_count > 0 THEN
        RAISE EXCEPTION 'Cannot proceed: % machine_set records still have NULL tenant_id after population attempts.', null_count;
    END IF;
    
    RAISE NOTICE 'Tenant population successful: All machine_set records have been assigned tenant_id values.';
END $$;

-- Make tenant_id NOT NULL after population and validation
ALTER TABLE machine_set ALTER COLUMN tenant_id SET NOT NULL;

-- ===== REMOVE ALL PROBLEMATIC GLOBAL UNIQUE CONSTRAINTS =====

-- Remove the main problematic constraint causing duplicate key errors
ALTER TABLE machine_set DROP CONSTRAINT IF EXISTS machine_set_machine_set_name_key;
DROP INDEX IF EXISTS machine_set_machine_set_name_key;

-- Remove other global unique constraints that conflict with tenant-scoped uniqueness
DROP INDEX IF EXISTS unique_machine_set_name_active;
DROP INDEX IF EXISTS uq_machine_set_name_deleted;
DROP INDEX IF EXISTS machine_set_name_key;
DROP INDEX IF EXISTS unique_machine_set_name;
DROP INDEX IF EXISTS uq_machine_set_name;

-- Dynamic removal of any remaining unique constraints on machine_set_name
DO $$
DECLARE
    constraint_record RECORD;
BEGIN
    -- Find and remove any remaining unique constraints involving only machine_set_name
    FOR constraint_record IN 
        SELECT conname, contype
        FROM pg_constraint 
        JOIN pg_class ON pg_constraint.conrelid = pg_class.oid 
        WHERE pg_class.relname = 'machine_set' 
        AND contype = 'u'
        AND array_length(conkey, 1) = 1  -- Only single-column constraints
        AND EXISTS (
            SELECT 1 
            FROM pg_attribute 
            WHERE attrelid = pg_class.oid 
            AND attnum = conkey[1]
            AND attname = 'machine_set_name'
        )
    LOOP
        EXECUTE 'ALTER TABLE machine_set DROP CONSTRAINT IF EXISTS ' || constraint_record.conname;
        RAISE NOTICE 'Removed constraint: %', constraint_record.conname;
    END LOOP;
END $$;

-- ===== CREATE TENANT-SCOPED UNIQUE CONSTRAINT =====

-- Drop and recreate the tenant-scoped unique index to ensure it's correct
DROP INDEX IF EXISTS unique_machine_set_name_tenant_active;

-- Create the correct tenant-scoped unique index for active (non-deleted) machine sets
-- This allows same machine_set_name across different tenants
-- and allows reusing names after deletion within the same tenant
CREATE UNIQUE INDEX unique_machine_set_name_tenant_active
    ON machine_set (machine_set_name, tenant_id) 
    WHERE deleted = false;

-- ===== FINAL VALIDATION =====

DO $$
DECLARE
    global_constraints INTEGER;
    tenant_index_exists BOOLEAN;
    tenant_column_exists BOOLEAN;
BEGIN
    -- Check that tenant_id column exists and is NOT NULL
    SELECT EXISTS(
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'machine_set' 
        AND column_name = 'tenant_id'
        AND is_nullable = 'NO'
    ) INTO tenant_column_exists;
    
    -- Check that no global unique constraints remain on machine_set_name
    SELECT COUNT(*) INTO global_constraints
    FROM pg_constraint 
    JOIN pg_class ON pg_constraint.conrelid = pg_class.oid 
    WHERE pg_class.relname = 'machine_set' 
    AND contype = 'u'
    AND array_length(conkey, 1) = 1  -- Single-column constraints
    AND EXISTS (
        SELECT 1 
        FROM pg_attribute 
        WHERE attrelid = pg_class.oid 
        AND attnum = conkey[1]
        AND attname = 'machine_set_name'
    );
    
    -- Check that tenant-scoped index exists
    SELECT EXISTS(
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'machine_set' 
        AND indexname = 'unique_machine_set_name_tenant_active'
    ) INTO tenant_index_exists;
    
    IF NOT tenant_column_exists THEN
        RAISE EXCEPTION 'Failed to create tenant_id column with NOT NULL constraint';
    END IF;
    
    IF global_constraints > 0 THEN
        RAISE WARNING 'Warning: % global unique constraints still exist on machine_set_name', global_constraints;
    END IF;
    
    IF NOT tenant_index_exists THEN
        RAISE EXCEPTION 'Failed to create tenant-scoped unique index';
    END IF;
    
    RAISE NOTICE 'SUCCESS: Migration V1_57 completed successfully!';
    RAISE NOTICE 'Machine sets now support tenant-scoped uniqueness.';
    RAISE NOTICE 'You can now create machine sets with the same name across different tenants.';
    RAISE NOTICE 'Same machine set names can be reused after deletion within the same tenant.';
END $$;

COMMIT;
