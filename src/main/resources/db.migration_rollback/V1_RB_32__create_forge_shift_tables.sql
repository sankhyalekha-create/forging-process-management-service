-- IMPORTANT: This script will permanently delete all forge shift data!
-- Ensure you have proper backups before executing this rollback.

-- Step 1: Drop indexes for forge_shift_heat table
DROP INDEX IF EXISTS idx_forge_shift_heat_heat_deleted_created;
DROP INDEX IF EXISTS idx_forge_shift_heat_created_at;
DROP INDEX IF EXISTS idx_forge_shift_heat_deleted;
DROP INDEX IF EXISTS idx_forge_shift_heat_heat_id;
DROP INDEX IF EXISTS idx_forge_shift_heat_shift_id_deleted;

-- Step 2: Drop indexes for forge_shift table
DROP INDEX IF EXISTS idx_forge_shift_forge_deleted_created;
DROP INDEX IF EXISTS idx_forge_shift_created_at;
DROP INDEX IF EXISTS idx_forge_shift_end_date_time;
DROP INDEX IF EXISTS idx_forge_shift_start_date_time;
DROP INDEX IF EXISTS idx_forge_shift_deleted;
DROP INDEX IF EXISTS idx_forge_shift_forge_id_deleted_end_time;
DROP INDEX IF EXISTS idx_forge_shift_forge_id_deleted;

-- Step 3: Drop foreign key constraints (if needed for some database systems)
-- ALTER TABLE forge_shift_heat DROP CONSTRAINT IF EXISTS fk_forge_shift_heat_heat;
-- ALTER TABLE forge_shift_heat DROP CONSTRAINT IF EXISTS fk_forge_shift_heat_forge_shift;
-- ALTER TABLE forge_shift DROP CONSTRAINT IF EXISTS fk_forge_shift_forge;

-- Step 4: Drop tables (CASCADE will also drop dependent objects)
-- Note: forge_shift_heat is dropped first due to foreign key dependency
DROP TABLE IF EXISTS forge_shift_heat CASCADE;
DROP TABLE IF EXISTS forge_shift CASCADE;

-- Step 5: Drop sequences
DROP SEQUENCE IF EXISTS forge_shift_heat_sequence;
DROP SEQUENCE IF EXISTS forge_shift_sequence;

-- Step 6: Revoke permissions (uncomment and adjust as per your application user)
-- REVOKE ALL PRIVILEGES ON forge_shift FROM your_app_user;
-- REVOKE ALL PRIVILEGES ON forge_shift_heat FROM your_app_user;
-- REVOKE ALL PRIVILEGES ON forge_shift_sequence FROM your_app_user;
-- REVOKE ALL PRIVILEGES ON forge_shift_heat_sequence FROM your_app_user;

-- Step 7: Verification queries (uncomment to verify rollback)
-- These should return 0 rows or errors indicating objects don't exist

-- SELECT COUNT(*) FROM information_schema.tables 
-- WHERE table_name IN ('forge_shift', 'forge_shift_heat');

-- SELECT COUNT(*) FROM information_schema.sequences 
-- WHERE sequence_name IN ('forge_shift_sequence', 'forge_shift_heat_sequence');

-- SELECT COUNT(*) FROM pg_indexes 
-- WHERE indexname LIKE 'idx_forge_shift%';

COMMIT;

-- =====================================================
-- Rollback completed successfully
-- All forge_shift related tables, indexes, and sequences have been removed
-- ===================================================== 