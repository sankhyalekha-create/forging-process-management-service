-- Rollback Migration: Fix All Unique Constraints Including Deleted Column
-- Version: V1_RB_52
-- Description: Rollback changes from V1_52 - restore all original unique constraints with deleted column
-- This reverts all partial unique indexes back to the problematic unique constraints

BEGIN;

-- ===== RAW MATERIAL =====
-- Note: This is handled by V1_RB_51, but included for completeness
-- DROP INDEX IF EXISTS unique_raw_material_invoice_tenant_active;
-- ALTER TABLE raw_material ADD CONSTRAINT unique_raw_material_invoice_tenant_deleted 
--     UNIQUE (raw_material_invoice_number, tenant_id, deleted);

-- ===== SUPPLIER =====
DROP INDEX IF EXISTS unique_supplier_name_tenant_active;
ALTER TABLE supplier ADD CONSTRAINT unique_supplier_name_tenant_deleted 
    UNIQUE (supplier_name, tenant_id, deleted);

-- ===== ITEM =====
DROP INDEX IF EXISTS unique_item_name_tenant_active;
DROP INDEX IF EXISTS unique_item_code_tenant_active;
ALTER TABLE item ADD CONSTRAINT unique_item_name_tenant_id_deleted 
    UNIQUE (item_name, tenant_id, deleted);
ALTER TABLE item ADD CONSTRAINT unique_item_code_tenant_id_deleted 
    UNIQUE (item_code, tenant_id, deleted);

-- ===== USR =====
DROP INDEX IF EXISTS unique_username_tenant_active;
ALTER TABLE usr ADD CONSTRAINT unique_username_tenant_deleted
    UNIQUE (username, tenant_id, deleted);

-- ===== FORGING LINE =====
DROP INDEX IF EXISTS unique_forging_line_name_tenant_active;
ALTER TABLE forging_line ADD CONSTRAINT uq_forging_line_name_tenant_deleted 
    UNIQUE (forging_line_name, tenant_id, deleted);

-- ===== FORGE =====
DROP INDEX IF EXISTS unique_forge_traceability_number_tenant_active;
ALTER TABLE forge ADD CONSTRAINT uq_forge_traceability_number_tenant_deleted 
    UNIQUE (forge_traceability_number, tenant_id, deleted);

-- ===== FURNACE =====
DROP INDEX IF EXISTS unique_furnace_name_tenant_active;
ALTER TABLE furnace ADD CONSTRAINT uq_furnace_name_tenant_deleted 
    UNIQUE (furnace_name, tenant_id, deleted);

-- ===== HEAT TREATMENT BATCH =====
DROP INDEX IF EXISTS unique_heat_treatment_batch_number_tenant_active;
ALTER TABLE heat_treatment_batch ADD CONSTRAINT uk_heat_treatment_batch_number_tenant_deleted 
    UNIQUE (heat_treatment_batch_number, tenant_id, deleted);

-- ===== MACHINE =====
DROP INDEX IF EXISTS unique_machine_name_tenant_active;
ALTER TABLE machine ADD CONSTRAINT uq_machine_name_tenant_deleted 
    UNIQUE (machine_name, tenant_id, deleted);

-- ===== MACHINE SET =====
DROP INDEX IF EXISTS unique_machine_set_name_active;
ALTER TABLE machine_set ADD CONSTRAINT uq_machine_set_name_deleted 
    UNIQUE (machine_set_name, deleted);

-- ===== MACHINING BATCH =====
DROP INDEX IF EXISTS unique_machining_batch_number_tenant_active;
ALTER TABLE machining_batch ADD CONSTRAINT uq_machining_batch_number_tenant_deleted 
    UNIQUE (machining_batch_number, tenant_id, deleted);

-- ===== GAUGE =====
DROP INDEX IF EXISTS unique_gauge_name_tenant_active;
ALTER TABLE gauge ADD CONSTRAINT uq_gauge_name_tenant_deleted 
    UNIQUE (gauge_name, tenant_id, deleted);

-- ===== INSPECTION BATCH =====
DROP INDEX IF EXISTS unique_inspection_batch_number_tenant_active;
ALTER TABLE inspection_batch ADD CONSTRAINT uq_inspection_batch_number_tenant_deleted 
    UNIQUE (inspection_batch_number, tenant_id, deleted);

-- ===== DISPATCH BATCH =====
DROP INDEX IF EXISTS unique_dispatch_batch_number_tenant_active;
DROP INDEX IF EXISTS unique_invoice_number_tenant_active;
ALTER TABLE dispatch_batch ADD CONSTRAINT uq_dispatch_batch_number_tenant_deleted 
    UNIQUE (dispatch_batch_number, tenant_id, deleted);
ALTER TABLE dispatch_batch ADD CONSTRAINT uq_invoice_number_tenant_deleted 
    UNIQUE (invoice_number, tenant_id, deleted);

-- ===== BUYER =====
DROP INDEX IF EXISTS unique_buyer_name_tenant_active;
ALTER TABLE buyer ADD CONSTRAINT unique_buyer_name_tenant_deleted 
    UNIQUE (buyer_name, tenant_id, deleted);

-- ===== BUYER ENTITY =====
DROP INDEX IF EXISTS unique_buyer_entity_name_active;
ALTER TABLE buyer_entity ADD CONSTRAINT unique_entity_name_buyer_deleted 
    UNIQUE (buyer_entity_name, buyer_id, deleted);

-- ===== DAILY MACHINING BATCH =====
DROP INDEX IF EXISTS unique_daily_machining_batch_number_active;
ALTER TABLE daily_machining_batch ADD CONSTRAINT uq_daily_machining_batch_number_machining_batch_deleted 
    UNIQUE (daily_machining_batch_number, machining_batch_id, deleted);

-- ===== WORKFLOW =====
DROP INDEX IF EXISTS unique_workflow_name_tenant_active;
ALTER TABLE workflow_template ADD CONSTRAINT unique_workflow_name_tenant_deleted
    UNIQUE (workflow_name, tenant_id, deleted);

-- ===== ITEM WORKFLOW STEP =====
DROP INDEX IF EXISTS unique_item_workflow_step_active;
ALTER TABLE item_workflow_step ADD CONSTRAINT unique_item_workflow_step 
    UNIQUE (item_workflow_id, workflow_step_id, deleted);

-- ===== VENDOR =====
DROP INDEX IF EXISTS unique_vendor_name_tenant_active;
ALTER TABLE vendor ADD CONSTRAINT unique_vendor_name_tenant_deleted 
    UNIQUE (vendor_name, tenant_id, deleted);

-- ===== VENDOR ENTITY =====
DROP INDEX IF EXISTS unique_vendor_entity_name_active;
ALTER TABLE vendor_entity ADD CONSTRAINT unique_entity_name_vendor_deleted 
    UNIQUE (vendor_entity_name, vendor_id, deleted);

-- ===== VENDOR DISPATCH BATCH =====
DROP INDEX IF EXISTS unique_vendor_dispatch_batch_number_tenant_active;
ALTER TABLE vendor_dispatch_batch ADD CONSTRAINT unique_vendor_dispatch_batch_number_tenant_deleted 
    UNIQUE (vendor_dispatch_batch_number, tenant_id, deleted);

-- ===== VENDOR RECEIVE BATCH =====
DROP INDEX IF EXISTS unique_vendor_receive_batch_number_tenant_active;
ALTER TABLE vendor_receive_batch ADD CONSTRAINT unique_vendor_receive_batch_number_tenant_deleted 
    UNIQUE (vendor_receive_batch_number, tenant_id, deleted);

-- ===== VENDOR INVENTORY =====
DROP INDEX IF EXISTS unique_vendor_inventory_vendor_heat_active;
ALTER TABLE vendor_inventory ADD CONSTRAINT uk_vendor_inventory_vendor_heat_deleted 
    UNIQUE (vendor_id, original_heat_id, deleted);

COMMIT;
