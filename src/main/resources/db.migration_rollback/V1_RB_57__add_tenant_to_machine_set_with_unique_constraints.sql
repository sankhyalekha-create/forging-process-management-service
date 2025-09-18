-- Rollback Migration: Remove Tenant Relationship from MachineSet and Restore Original Uniqueness Constraint
-- Version: V1_RB_57
-- Description: Complete rollback of V1_57 - Remove tenant_id foreign key from machine_set table and restore global constraint
-- This rollback script reverses all changes made in V1_57__add_tenant_to_machine_set_with_unique_constraints.sql
-- WARNING: This rollback will cause data loss of tenant associations and may fail if machine sets with same names exist across tenants

BEGIN;

-- ===== VALIDATION BEFORE ROLLBACK =====
-- Check if there are any machine sets with the same name that would conflict after rollback
DO $$
DECLARE
    conflict_count INTEGER;
    total_machine_sets INTEGER;
    tenant_count INTEGER;
BEGIN
    -- Check basic system state
    SELECT COUNT(*) INTO total_machine_sets FROM machine_set WHERE deleted = false;
    SELECT COUNT(DISTINCT tenant_id) INTO tenant_count FROM machine_set WHERE deleted = false;
    
    -- Check for name conflicts across tenants
    SELECT COUNT(*) INTO conflict_count
    FROM (
        SELECT machine_set_name
        FROM machine_set 
        WHERE deleted = false
        GROUP BY machine_set_name
        HAVING COUNT(DISTINCT tenant_id) > 1
    ) conflicts;
    
    RAISE NOTICE 'Rollback validation: % total machine sets, % tenants, % name conflicts', 
                 total_machine_sets, tenant_count, conflict_count;
    
    IF conflict_count > 0 THEN
        RAISE WARNING 'WARNING: % machine set names exist across multiple tenants!', conflict_count;
        RAISE WARNING 'Rollback may cause uniqueness constraint violations.';
        RAISE WARNING 'Consider manually resolving name conflicts before proceeding with rollback.';
        RAISE WARNING 'To see conflicts, run: SELECT machine_set_name, COUNT(DISTINCT tenant_id) as tenant_count FROM machine_set WHERE deleted = false GROUP BY machine_set_name HAVING COUNT(DISTINCT tenant_id) > 1;';
        
        -- Uncomment the line below to prevent rollback when conflicts exist
        -- RAISE EXCEPTION 'Rollback blocked due to potential name conflicts. Please resolve manually first.';
    END IF;
    
    RAISE NOTICE 'Proceeding with rollback...';
END $$;

-- ===== REMOVE TENANT-SCOPED UNIQUE INDEX =====
-- Drop the tenant-scoped unique index created in V1_57
DROP INDEX IF EXISTS unique_machine_set_name_tenant_active;

-- ===== RESTORE GLOBAL UNIQUE CONSTRAINT =====
-- Restore the original global unique constraint that was dropped in V1_57
-- Handle conflicts by renaming duplicate machine sets first
DO $$
DECLARE
    conflict_record RECORD;
    counter INTEGER;
BEGIN
    -- Handle conflicting machine set names by appending tenant-specific suffixes
    FOR conflict_record IN 
        SELECT machine_set_name, tenant_id, id
        FROM machine_set 
        WHERE deleted = false
        AND machine_set_name IN (
            SELECT machine_set_name
            FROM machine_set 
            WHERE deleted = false
            GROUP BY machine_set_name
            HAVING COUNT(DISTINCT tenant_id) > 1
        )
        ORDER BY machine_set_name, tenant_id
    LOOP
        -- Rename to make it unique: original_name_tenant_X
        UPDATE machine_set 
        SET machine_set_name = conflict_record.machine_set_name || '_tenant_' || conflict_record.tenant_id
        WHERE id = conflict_record.id;
        
        RAISE NOTICE 'Renamed machine set ID % from "%" to "%" to resolve conflict', 
                     conflict_record.id, 
                     conflict_record.machine_set_name, 
                     conflict_record.machine_set_name || '_tenant_' || conflict_record.tenant_id;
    END LOOP;
END $$;

-- Now create the global unique constraint (should succeed after renaming conflicts)
CREATE UNIQUE INDEX machine_set_machine_set_name_key
    ON machine_set (machine_set_name) WHERE deleted = false;

-- ===== REMOVE TENANT_ID COLUMN AND RELATED CONSTRAINTS =====
-- Drop the foreign key constraint
ALTER TABLE machine_set DROP CONSTRAINT IF EXISTS fk_machine_set_tenant;

-- Drop the index on tenant_id
DROP INDEX IF EXISTS idx_machine_set_tenant;

-- Remove the NOT NULL constraint from tenant_id
ALTER TABLE machine_set ALTER COLUMN tenant_id DROP NOT NULL;

-- Drop the tenant_id column completely
-- WARNING: This permanently loses all tenant association data
ALTER TABLE machine_set DROP COLUMN IF EXISTS tenant_id;

-- ===== POST-ROLLBACK VALIDATION =====
DO $$
DECLARE
    tenant_column_exists BOOLEAN;
    global_constraint_exists BOOLEAN;
BEGIN
    -- Check that tenant_id column is gone
    SELECT EXISTS(
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'machine_set' 
        AND column_name = 'tenant_id'
    ) INTO tenant_column_exists;
    
    -- Check that global constraint exists
    SELECT EXISTS(
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'machine_set' 
        AND indexname = 'machine_set_machine_set_name_key'
    ) INTO global_constraint_exists;
    
    IF tenant_column_exists THEN
        RAISE WARNING 'WARNING: tenant_id column still exists after rollback';
    END IF;
    
    IF NOT global_constraint_exists THEN
        RAISE WARNING 'WARNING: Global unique constraint was not restored properly';
    END IF;
    
    RAISE NOTICE 'SUCCESS: Rollback V1_RB_57 completed.';
    RAISE NOTICE 'Machine sets now use global uniqueness constraint (machine_set_name must be unique across all tenants).';
    RAISE NOTICE 'Any conflicting machine set names have been automatically renamed with tenant suffixes.';
    RAISE NOTICE 'WARNING: All tenant associations have been permanently lost.';
    RAISE NOTICE 'WARNING: Application code must be reverted to pre-V1_57 version for proper functionality.';
    RAISE NOTICE 'WARNING: Repository methods expecting tenant_id relationships will fail.';
    RAISE NOTICE 'WARNING: You may need to manually review and rename machine sets that were automatically renamed.';
END $$;

-- ===== INSTRUCTIONS FOR MANUAL CLEANUP =====
-- After this rollback, you may need to:
-- 1. Revert application code changes (MachineSet entity, repository, service)
-- 2. Handle any duplicate machine set names manually
-- 3. Update any business logic that depends on tenant-scoped machine sets
-- 4. Consider re-establishing tenant relationships through machine associations

COMMIT;
