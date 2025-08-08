-- Comprehensive Workflow System Migration
-- Creates all workflow tables with batch-level support and data flow enhancements
-- Includes all workflow-related changes from V1_34 through V1_38
-- Version: V1_34__workflow_related.sql (Consolidated)

-- Create sequence for workflow_template
CREATE SEQUENCE IF NOT EXISTS workflow_template_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Create workflow_template table
CREATE TABLE IF NOT EXISTS workflow_template (
                                                 id BIGINT PRIMARY KEY DEFAULT nextval('workflow_template_sequence'),
    workflow_name VARCHAR(255) NOT NULL,
    workflow_description TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_workflow_template_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT unique_workflow_name_tenant_deleted UNIQUE (workflow_name, tenant_id, deleted)
    );

-- Create sequence for workflow_step
CREATE SEQUENCE IF NOT EXISTS workflow_step_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Create workflow_step table with tree structure support
CREATE TABLE IF NOT EXISTS workflow_step (
                                                 id BIGINT PRIMARY KEY DEFAULT nextval('workflow_step_sequence'),
          workflow_template_id BIGINT NOT NULL,
          operation_type VARCHAR(50) NOT NULL CHECK (operation_type IN ('FORGING', 'HEAT_TREATMENT', 'MACHINING', 'VENDOR', 'QUALITY', 'DISPATCH')),
          step_name VARCHAR(255) NOT NULL,
          step_description TEXT,
          is_optional BOOLEAN NOT NULL DEFAULT FALSE,
          is_parallel BOOLEAN NOT NULL DEFAULT FALSE,
          parent_step_id BIGINT, -- Tree structure: parent-child relationships
          created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
          deleted_at TIMESTAMP WITHOUT TIME ZONE,
          deleted BOOLEAN NOT NULL DEFAULT FALSE,
          CONSTRAINT fk_workflow_step_template FOREIGN KEY (workflow_template_id) REFERENCES workflow_template(id),
          CONSTRAINT fk_workflow_step_parent FOREIGN KEY (parent_step_id) REFERENCES workflow_step(id)
          );

-- Create sequence for item_workflow
CREATE SEQUENCE IF NOT EXISTS item_workflow_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Create item_workflow table with batch support
CREATE TABLE IF NOT EXISTS item_workflow (
                                             id BIGINT PRIMARY KEY DEFAULT nextval('item_workflow_sequence'),
    item_id BIGINT NOT NULL,
    workflow_template_id BIGINT NOT NULL,
    workflow_identifier VARCHAR(255),
    workflow_status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED' CHECK (workflow_status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'ON_HOLD')),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_item_workflow_item FOREIGN KEY (item_id) REFERENCES item(id),
    CONSTRAINT fk_item_workflow_template FOREIGN KEY (workflow_template_id) REFERENCES workflow_template(id)
    );

-- Create sequence for item_workflow_step
CREATE SEQUENCE IF NOT EXISTS item_workflow_step_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Create item_workflow_step table with data flow enhancements and related entity IDs
CREATE TABLE IF NOT EXISTS item_workflow_step (
                                                  id BIGINT PRIMARY KEY DEFAULT nextval('item_workflow_step_sequence'),
    item_workflow_id BIGINT NOT NULL,
    workflow_step_id BIGINT NOT NULL,
    parent_item_workflow_step_id BIGINT, -- Tree structure: parent ItemWorkflowStep
    operation_type VARCHAR(50) NOT NULL CHECK (operation_type IN ('FORGING', 'HEAT_TREATMENT', 'MACHINING', 'VENDOR', 'QUALITY', 'DISPATCH')),
    step_status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (step_status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED', 'FAILED')),
    started_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    operation_reference_id BIGINT,
    notes TEXT,
    operation_outcome_data JSONB,
    related_entity_ids JSONB DEFAULT NULL,
    initial_pieces_count INT,
    pieces_available_for_next INT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_item_workflow_step_item_workflow FOREIGN KEY (item_workflow_id) REFERENCES item_workflow(id),
    CONSTRAINT fk_item_workflow_step_workflow_step FOREIGN KEY (workflow_step_id) REFERENCES workflow_step(id),
    CONSTRAINT fk_item_workflow_step_parent FOREIGN KEY (parent_item_workflow_step_id) REFERENCES item_workflow_step(id),
    CONSTRAINT unique_item_workflow_step UNIQUE (item_workflow_id, workflow_step_id, deleted)
    );

-- Add constraints for workflow-level workflows
-- Ensure that each item can have only one workflow per workflow identifier combination
ALTER TABLE item_workflow ADD CONSTRAINT uk_item_workflow_identifier
    UNIQUE (item_id, workflow_identifier, deleted)
    DEFERRABLE INITIALLY DEFERRED;

-- Ensure that each item can have only one item-level workflow (where workflow identifier is null)
CREATE UNIQUE INDEX uk_item_workflow_item_level
    ON item_workflow (item_id, deleted)
    WHERE workflow_identifier IS NULL;

-- Add workflow fields to processed_item table (per-item workflow tracking)
ALTER TABLE processed_item 
ADD COLUMN IF NOT EXISTS workflow_identifier VARCHAR(255);

ALTER TABLE processed_item 
ADD COLUMN IF NOT EXISTS item_workflow_id BIGINT;

-- Add workflow fields to processed_item_heat_treatment_batch table (per-processed-item workflow tracking)
ALTER TABLE processed_item_heat_treatment_batch 
ADD COLUMN IF NOT EXISTS workflow_identifier VARCHAR(255);

ALTER TABLE processed_item_heat_treatment_batch 
ADD COLUMN IF NOT EXISTS item_workflow_id BIGINT;

-- Create performance indexes for workflow_template
CREATE INDEX IF NOT EXISTS idx_workflow_template_tenant_active ON workflow_template(tenant_id, is_active, deleted);
CREATE INDEX IF NOT EXISTS idx_workflow_template_default ON workflow_template(tenant_id, is_default, deleted) WHERE is_default = true;

-- Create performance indexes for workflow_step (tree structure)
-- Removed idx_workflow_step_template_order as step_order is no longer used in tree structure
CREATE INDEX IF NOT EXISTS idx_workflow_step_operation_type ON workflow_step(operation_type, deleted);
CREATE INDEX IF NOT EXISTS idx_workflow_step_parent ON workflow_step(parent_step_id) WHERE parent_step_id IS NOT NULL AND deleted = false;
CREATE INDEX IF NOT EXISTS idx_workflow_step_template_parent ON workflow_step(workflow_template_id, parent_step_id) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_workflow_step_root ON workflow_step(workflow_template_id) WHERE parent_step_id IS NULL AND deleted = false;

-- Create performance indexes for item_workflow
CREATE INDEX IF NOT EXISTS idx_item_workflow_item ON item_workflow(item_id, deleted);
CREATE INDEX IF NOT EXISTS idx_item_workflow_status ON item_workflow(workflow_status, deleted);
CREATE INDEX IF NOT EXISTS idx_item_workflow_workflow_identifier
    ON item_workflow (workflow_identifier)
    WHERE workflow_identifier IS NOT NULL AND deleted = false;
CREATE INDEX IF NOT EXISTS idx_item_workflow_status_active
    ON item_workflow (workflow_status, deleted)
    WHERE deleted = false;

-- Create performance indexes for item_workflow_step
CREATE INDEX IF NOT EXISTS idx_item_workflow_step_workflow ON item_workflow_step(item_workflow_id, deleted);
CREATE INDEX IF NOT EXISTS idx_item_workflow_step_status ON item_workflow_step(step_status, deleted);
CREATE INDEX IF NOT EXISTS idx_item_workflow_step_operation_type ON item_workflow_step(operation_type);
CREATE INDEX IF NOT EXISTS idx_item_workflow_step_workflow_operation ON item_workflow_step(item_workflow_id, operation_type);
CREATE INDEX IF NOT EXISTS idx_item_workflow_step_status_operation ON item_workflow_step(step_status, operation_type);
CREATE INDEX IF NOT EXISTS idx_item_workflow_step_pieces_available ON item_workflow_step(pieces_available_for_next);
CREATE INDEX IF NOT EXISTS idx_item_workflow_step_completed_at ON item_workflow_step(completed_at);

-- Create indexes for ItemWorkflowStep tree structure performance
CREATE INDEX IF NOT EXISTS idx_item_workflow_step_parent ON item_workflow_step(parent_item_workflow_step_id) WHERE parent_item_workflow_step_id IS NOT NULL AND deleted = false;
CREATE INDEX IF NOT EXISTS idx_item_workflow_step_workflow_parent ON item_workflow_step(item_workflow_id, parent_item_workflow_step_id) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_item_workflow_step_root ON item_workflow_step(item_workflow_id) WHERE parent_item_workflow_step_id IS NULL AND deleted = false;

-- Add GIN indexes for JSONB operations (from V1_37 and V1_38)
CREATE INDEX IF NOT EXISTS idx_item_workflow_step_outcome_data_gin 
ON item_workflow_step USING GIN (operation_outcome_data);

-- Create indexes for processed_item workflow integration
CREATE INDEX IF NOT EXISTS idx_processed_item_workflow_identifier 
ON processed_item(workflow_identifier);

CREATE INDEX IF NOT EXISTS idx_processed_item_item_workflow_id 
ON processed_item(item_workflow_id);

-- Create indexes for processed_item_heat_treatment_batch workflow integration
CREATE INDEX IF NOT EXISTS idx_processed_item_ht_batch_workflow_identifier 
ON processed_item_heat_treatment_batch(workflow_identifier);

CREATE INDEX IF NOT EXISTS idx_processed_item_ht_batch_item_workflow_id 
ON processed_item_heat_treatment_batch(item_workflow_id);

-- Add composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_processed_item_workflow_deleted 
ON processed_item(workflow_identifier, deleted);

CREATE INDEX IF NOT EXISTS idx_processed_item_ht_batch_workflow_deleted 
ON processed_item_heat_treatment_batch(workflow_identifier, deleted);

-- Add comprehensive comments for documentation
COMMENT ON TABLE workflow_template IS 'Workflow templates defining the sequence of operations for manufacturing processes';
COMMENT ON TABLE workflow_step IS 'Individual steps within a workflow template, defining operations in a tree structure with parent-child relationships';
COMMENT ON TABLE item_workflow IS 'Workflow instances tracking item progress through manufacturing operations. Supports both item-level and batch-level workflows.';
COMMENT ON TABLE item_workflow_step IS 'Individual step instances tracking progress through specific workflow steps with operational data flow';

COMMENT ON COLUMN item_workflow.workflow_identifier IS 'Universal workflow identifier for workflow tracking across all operations (e.g., forge traceability number, heat treatment batch number, etc.). NULL for item-level workflows that track overall item progress.';

COMMENT ON COLUMN item_workflow_step.operation_type IS 'Operation type enum stored directly for efficient querying without joins to workflow_step table';
COMMENT ON COLUMN item_workflow_step.operation_outcome_data IS 'Optimized JSONB storage for workflow flow control data with query capabilities';
COMMENT ON COLUMN item_workflow_step.related_entity_ids IS 'JSON array of related entity IDs specific to the operation type (processed_item.id for FORGING, processed_itemHeatTreatmentBatch.id for HEAT_TREATMENT, etc.)';
COMMENT ON COLUMN item_workflow_step.pieces_available_for_next IS 'Number of pieces available for the next operation (key workflow flow field)';
COMMENT ON COLUMN item_workflow_step.initial_pieces_count IS 'Original pieces produced by this operation (audit trail)';

COMMENT ON COLUMN workflow_step.parent_step_id IS 'Parent workflow step ID for tree-based workflow structure. NULL for root steps';
COMMENT ON COLUMN item_workflow_step.parent_item_workflow_step_id IS 'Parent item workflow step ID for tree-based execution structure. NULL for root steps';


-- Add comments for processed_item table workflow columns
COMMENT ON COLUMN processed_item.workflow_identifier IS 'Workflow identifier for this specific processed item (enables per-item workflow tracking)';
COMMENT ON COLUMN processed_item.item_workflow_id IS 'Item workflow ID for this specific processed item (enables per-item workflow tracking)';

-- Add comments for processed_item_heat_treatment_batch table workflow columns
COMMENT ON COLUMN processed_item_heat_treatment_batch.workflow_identifier IS 'Workflow identifier for this specific processed item heat treatment batch (enables per-batch-item workflow tracking)';
COMMENT ON COLUMN processed_item_heat_treatment_batch.item_workflow_id IS 'Item workflow ID for this specific processed item heat treatment batch (enables per-batch-item workflow tracking)';

COMMENT ON INDEX uk_item_workflow_item_level IS 'Ensures each item can have only one item-level workflow (where workflow identifier is NULL)';

-- Populate operation_type column from workflow_step table (for any existing data)
-- This ensures data consistency if tables already exist with data
UPDATE item_workflow_step 
SET operation_type = ws.operation_type 
FROM workflow_step ws 
WHERE item_workflow_step.workflow_step_id = ws.id 
AND item_workflow_step.operation_type IS NULL;

-- Handle existing operation_outcome_data conversion to JSONB (from V1_37)
-- This handles databases that might have the column as TEXT
DO $$
BEGIN
    -- Check if operation_outcome_data column exists and is of TEXT type
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'item_workflow_step' 
        AND column_name = 'operation_outcome_data' 
        AND data_type = 'text'
    ) THEN
        -- Convert TEXT column to JSONB
        -- First, ensure all TEXT data is valid JSON (replace invalid entries with empty JSON)
        UPDATE item_workflow_step 
        SET operation_outcome_data = '{}' 
        WHERE operation_outcome_data IS NOT NULL 
        AND operation_outcome_data != '' 
        AND NOT (operation_outcome_data::text ~ '^[\s]*[{\[].*[}\]][\s]*$');
        
        -- Convert the column type from TEXT to JSONB
        ALTER TABLE item_workflow_step 
        ALTER COLUMN operation_outcome_data TYPE JSONB USING operation_outcome_data::JSONB;
        
        RAISE NOTICE 'Successfully converted operation_outcome_data from TEXT to JSONB';
    END IF;
    
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'operation_outcome_data column conversion handled: %', SQLERRM;
END $$;

-- Validation and completion message
DO $$
BEGIN
    -- Verify all tables exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'workflow_template') THEN
        RAISE EXCEPTION 'Migration failed: workflow_template table not created';
END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'workflow_step') THEN
        RAISE EXCEPTION 'Migration failed: workflow_step table not created';
END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'item_workflow') THEN
        RAISE EXCEPTION 'Migration failed: item_workflow table not created';
END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'item_workflow_step') THEN
        RAISE EXCEPTION 'Migration failed: item_workflow_step table not created';
END IF;
    
    -- Verify workflow support columns exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'item_workflow' AND column_name = 'workflow_identifier') THEN
        RAISE EXCEPTION 'Migration failed: workflow_identifier column not created';
END IF;
    
    -- Verify workflow data flow enhancement columns exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'item_workflow_step' AND column_name = 'operation_outcome_data') THEN
        RAISE EXCEPTION 'Migration failed: operation_outcome_data column not created';
END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'item_workflow_step' AND column_name = 'pieces_available_for_next') THEN
        RAISE EXCEPTION 'Migration failed: pieces_available_for_next column not created';
END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'item_workflow_step' AND column_name = 'operation_type') THEN
        RAISE EXCEPTION 'Migration failed: operation_type column not created';
END IF;
    
    -- Verify related_entity_ids column exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'item_workflow_step' AND column_name = 'related_entity_ids') THEN
        RAISE EXCEPTION 'Migration failed: related_entity_ids column not created';
END IF;

    
    -- Verify processed_item workflow integration columns exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'processed_item' AND column_name = 'workflow_identifier') THEN
        RAISE EXCEPTION 'Migration failed: processed_item.workflow_identifier column not created';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'processed_item' AND column_name = 'item_workflow_id') THEN
        RAISE EXCEPTION 'Migration failed: processed_item.item_workflow_id column not created';
    END IF;
    
    -- Verify processed_item_heat_treatment_batch workflow integration columns exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'processed_item_heat_treatment_batch' AND column_name = 'workflow_identifier') THEN
        RAISE EXCEPTION 'Migration failed: processed_item_heat_treatment_batch.workflow_identifier column not created';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'processed_item_heat_treatment_batch' AND column_name = 'item_workflow_id') THEN
        RAISE EXCEPTION 'Migration failed: processed_item_heat_treatment_batch.item_workflow_id column not created';
    END IF;
    
    RAISE NOTICE 'Migration completed successfully: Comprehensive workflow system with batch support, data flow enhancements, and full integration created';
END $$; 