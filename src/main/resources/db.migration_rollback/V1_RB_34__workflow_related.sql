-- Comprehensive Rollback for Workflow System Migration
-- Rollback file for V1_34__workflow_related.sql (Consolidated)
-- This file can be used to completely remove the entire workflow system
-- Includes rollback operations from V1_RB_34 through V1_RB_38

-- WARNING: This will permanently delete all workflow data!
-- Make sure to backup your data before running this rollback.

-- Step 1: Drop all indexes first (foreign key dependent indexes)
DROP INDEX IF EXISTS idx_item_workflow_step_completed_at;
DROP INDEX IF EXISTS idx_item_workflow_step_pieces_available;
DROP INDEX IF EXISTS idx_item_workflow_step_status_operation;
DROP INDEX IF EXISTS idx_item_workflow_step_workflow_operation;
DROP INDEX IF EXISTS idx_item_workflow_step_operation_type;
DROP INDEX IF EXISTS idx_item_workflow_step_status;
DROP INDEX IF EXISTS idx_item_workflow_step_workflow;

-- Drop JSONB GIN indexes (from V1_RB_37 and V1_RB_38)
DROP INDEX IF EXISTS idx_item_workflow_step_outcome_data_gin;

DROP INDEX IF EXISTS idx_item_workflow_status_active;
DROP INDEX IF EXISTS idx_item_workflow_workflow_identifier;
DROP INDEX IF EXISTS idx_item_workflow_status;
DROP INDEX IF EXISTS idx_item_workflow_item;

DROP INDEX IF EXISTS idx_workflow_step_operation_type;
DROP INDEX IF EXISTS idx_workflow_step_template_order;

DROP INDEX IF EXISTS idx_workflow_template_default;
DROP INDEX IF EXISTS idx_workflow_template_tenant_active;

-- Drop forge table indexes (from V1_RB_35)
DROP INDEX IF EXISTS idx_forge_shift_workflow_deleted;
DROP INDEX IF EXISTS idx_forge_workflow_deleted;
DROP INDEX IF EXISTS idx_forge_shift_item_workflow_id;
DROP INDEX IF EXISTS idx_forge_item_workflow_id;

-- Drop heat_treatment_batch indexes (from V1_RB_36)
DROP INDEX IF EXISTS idx_heat_treatment_batch_item_workflow_id;
DROP INDEX IF EXISTS idx_heat_treatment_batch_workflow_identifier;

-- Drop processed_item workflow indexes
DROP INDEX IF EXISTS idx_processed_item_workflow_deleted;
DROP INDEX IF EXISTS idx_processed_item_item_workflow_id;
DROP INDEX IF EXISTS idx_processed_item_workflow_identifier;

-- Drop processed_item_heat_treatment_batch workflow indexes
DROP INDEX IF EXISTS idx_processed_item_ht_batch_workflow_deleted;
DROP INDEX IF EXISTS idx_processed_item_ht_batch_item_workflow_id;
DROP INDEX IF EXISTS idx_processed_item_ht_batch_workflow_identifier;

-- Step 2: Drop unique indexes and constraints
DROP INDEX IF EXISTS uk_item_workflow_item_level;

-- Drop named constraints
ALTER TABLE item_workflow DROP CONSTRAINT IF EXISTS uk_item_workflow_identifier;

-- Step 3: Drop foreign key constraints
ALTER TABLE item_workflow_step DROP CONSTRAINT IF EXISTS fk_item_workflow_step_workflow_step;
ALTER TABLE item_workflow_step DROP CONSTRAINT IF EXISTS fk_item_workflow_step_item_workflow;

ALTER TABLE item_workflow DROP CONSTRAINT IF EXISTS fk_item_workflow_template;
ALTER TABLE item_workflow DROP CONSTRAINT IF EXISTS fk_item_workflow_item;

ALTER TABLE workflow_step DROP CONSTRAINT IF EXISTS fk_workflow_step_template;

ALTER TABLE workflow_template DROP CONSTRAINT IF EXISTS fk_workflow_template_tenant;

-- Drop foreign key constraints for forge tables (if they were enabled)
-- ALTER TABLE forge_shift 
-- DROP CONSTRAINT IF EXISTS fk_forge_shift_item_workflow;
-- 
-- ALTER TABLE forge 
-- DROP CONSTRAINT IF EXISTS fk_forge_item_workflow;

-- Step 4: Drop unique constraints on tables
ALTER TABLE workflow_template DROP CONSTRAINT IF EXISTS unique_workflow_name_tenant_deleted;
ALTER TABLE workflow_step DROP CONSTRAINT IF EXISTS unique_step_order_template;
ALTER TABLE item_workflow_step DROP CONSTRAINT IF EXISTS unique_item_workflow_step;


-- Remove workflow columns from processed_item table
ALTER TABLE processed_item 
DROP COLUMN IF EXISTS item_workflow_id;

ALTER TABLE processed_item 
DROP COLUMN IF EXISTS workflow_identifier;

-- Remove workflow columns from processed_item_heat_treatment_batch table
ALTER TABLE processed_item_heat_treatment_batch 
DROP COLUMN IF EXISTS item_workflow_id;

ALTER TABLE processed_item_heat_treatment_batch 
DROP COLUMN IF EXISTS workflow_identifier;

-- Step 6: Handle operation_outcome_data conversion back to TEXT (from V1_RB_37)
-- This handles the case where the column was converted from TEXT to JSONB
DO $$
BEGIN
    -- Check if operation_outcome_data column exists and is of JSONB type
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'item_workflow_step' 
        AND column_name = 'operation_outcome_data' 
        AND data_type = 'jsonb'
    ) THEN
        -- Convert the column type from JSONB back to TEXT
        ALTER TABLE item_workflow_step 
        ALTER COLUMN operation_outcome_data TYPE TEXT USING operation_outcome_data::TEXT;
        
        RAISE NOTICE 'Successfully converted operation_outcome_data from JSONB back to TEXT';
    END IF;
    
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'operation_outcome_data column conversion handled: %', SQLERRM;
END $$;

-- Step 7: Drop tables in reverse order of dependencies
DROP TABLE IF EXISTS item_workflow_step CASCADE;
DROP TABLE IF EXISTS item_workflow CASCADE;
DROP TABLE IF EXISTS workflow_step CASCADE;
DROP TABLE IF EXISTS workflow_template CASCADE;

-- Step 8: Drop sequences
DROP SEQUENCE IF EXISTS item_workflow_step_sequence;
DROP SEQUENCE IF EXISTS item_workflow_sequence;
DROP SEQUENCE IF EXISTS workflow_step_sequence;
DROP SEQUENCE IF EXISTS workflow_template_sequence;

-- Step 9: Validation to ensure complete cleanup
DO $$
BEGIN
    -- Verify all tables are dropped
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'item_workflow_step') THEN
        RAISE EXCEPTION 'Rollback failed: item_workflow_step table still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'item_workflow') THEN
        RAISE EXCEPTION 'Rollback failed: item_workflow table still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'workflow_step') THEN
        RAISE EXCEPTION 'Rollback failed: workflow_step table still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'workflow_template') THEN
        RAISE EXCEPTION 'Rollback failed: workflow_template table still exists';
    END IF;
    
    -- Verify all sequences are dropped
    IF EXISTS (SELECT 1 FROM information_schema.sequences WHERE sequence_name = 'item_workflow_step_sequence') THEN
        RAISE EXCEPTION 'Rollback failed: item_workflow_step_sequence still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.sequences WHERE sequence_name = 'item_workflow_sequence') THEN
        RAISE EXCEPTION 'Rollback failed: item_workflow_sequence still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.sequences WHERE sequence_name = 'workflow_step_sequence') THEN
        RAISE EXCEPTION 'Rollback failed: workflow_step_sequence still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.sequences WHERE sequence_name = 'workflow_template_sequence') THEN
        RAISE EXCEPTION 'Rollback failed: workflow_template_sequence still exists';
    END IF;
    
    -- Verify workflow integration columns are removed from heat_treatment_batch table
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'heat_treatment_batch' AND column_name = 'workflow_identifier') THEN
        RAISE EXCEPTION 'Rollback failed: heat_treatment_batch.workflow_identifier column still exists';
    END IF;

    -- Verify workflow integration columns are removed from processed_item table
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'processeditem' AND column_name = 'workflow_identifier') THEN
        RAISE EXCEPTION 'Rollback failed: processed_item.workflow_identifier column still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'processeditem' AND column_name = 'item_workflow_id') THEN
        RAISE EXCEPTION 'Rollback failed: processed_item.item_workflow_id column still exists';
    END IF;
    
    -- Verify workflow integration columns are removed from processed_item_heat_treatment_batch table
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'processed_item_heat_treatment_batch' AND column_name = 'workflow_identifier') THEN
        RAISE EXCEPTION 'Rollback failed: processed_item_heat_treatment_batch.workflow_identifier column still exists';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'processed_item_heat_treatment_batch' AND column_name = 'item_workflow_id') THEN
        RAISE EXCEPTION 'Rollback failed: processed_item_heat_treatment_batch.item_workflow_id column still exists';
    END IF;
    
    RAISE NOTICE 'Comprehensive rollback completed successfully: All workflow system components and integrations removed';
END $$;

-- Optional: Clean up any orphaned data references
-- Note: You may need to manually clean up any foreign key references 
-- from other tables that point to the workflow tables

COMMENT ON SCHEMA public IS 'Comprehensive workflow system rollback completed. All workflow tables, sequences, indexes, constraints, and integration columns have been removed.'; 