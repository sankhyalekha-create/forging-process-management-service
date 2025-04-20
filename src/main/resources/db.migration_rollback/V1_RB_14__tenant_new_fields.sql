-- Rollback for V1_14: Remove isInternal and isActive columns from tenant table
ALTER TABLE tenant DROP COLUMN is_internal;
ALTER TABLE tenant DROP COLUMN is_active; 