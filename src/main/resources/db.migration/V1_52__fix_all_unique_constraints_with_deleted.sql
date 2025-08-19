-- Migration: Fix All Unique Constraints Including Deleted Column
-- Version: V1_52
-- Description: Replace all unique constraints that include 'deleted' column with partial unique indexes
-- This prevents duplicate key violations during soft delete operations across all entities

BEGIN;

-- ===== RAW MATERIAL =====
-- Already handled in V1_51, but included for completeness
-- ALTER TABLE raw_material DROP CONSTRAINT IF EXISTS unique_raw_material_invoice_tenant_deleted;
-- CREATE UNIQUE INDEX IF NOT EXISTS unique_raw_material_invoice_tenant_active
--     ON raw_material (raw_material_invoice_number, tenant_id) WHERE deleted = false;

-- ===== SUPPLIER =====
ALTER TABLE supplier DROP CONSTRAINT IF EXISTS unique_supplier_name_tenant_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_supplier_name_tenant_active
    ON supplier (supplier_name, tenant_id) WHERE deleted = false;

-- ===== ITEM =====
ALTER TABLE item DROP CONSTRAINT IF EXISTS unique_item_name_tenant_id_deleted;
ALTER TABLE item DROP CONSTRAINT IF EXISTS unique_item_code_tenant_id_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_item_name_tenant_active
    ON item (item_name, tenant_id) WHERE deleted = false;
CREATE UNIQUE INDEX IF NOT EXISTS unique_item_code_tenant_active
    ON item (item_code, tenant_id) WHERE deleted = false;

-- ===== USR =====
ALTER TABLE usr DROP CONSTRAINT IF EXISTS unique_username_tenant_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_username_tenant_active
    ON usr (username, tenant_id) WHERE deleted = false;

-- ===== FORGING LINE =====
ALTER TABLE forging_line DROP CONSTRAINT IF EXISTS uq_forging_line_name_tenant_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_forging_line_name_tenant_active
    ON forging_line (forging_line_name, tenant_id) WHERE deleted = false;

-- ===== FORGE =====
ALTER TABLE forge DROP CONSTRAINT IF EXISTS uq_forge_traceability_number_tenant_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_forge_traceability_number_tenant_active
    ON forge (forge_traceability_number, tenant_id) WHERE deleted = false;

-- ===== FURNACE =====
ALTER TABLE furnace DROP CONSTRAINT IF EXISTS uq_furnace_name_tenant_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_furnace_name_tenant_active
    ON furnace (furnace_name, tenant_id) WHERE deleted = false;

-- ===== HEAT TREATMENT BATCH =====
ALTER TABLE heat_treatment_batch DROP CONSTRAINT IF EXISTS uk_heat_treatment_batch_number_tenant_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_heat_treatment_batch_number_tenant_active
    ON heat_treatment_batch (heat_treatment_batch_number, tenant_id) WHERE deleted = false;

-- ===== MACHINE =====
ALTER TABLE machine DROP CONSTRAINT IF EXISTS uq_machine_name_tenant_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_machine_name_tenant_active
    ON machine (machine_name, tenant_id) WHERE deleted = false;

-- ===== MACHINE SET =====
ALTER TABLE machine_set DROP CONSTRAINT IF EXISTS uq_machine_set_name_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_machine_set_name_active
    ON machine_set (machine_set_name) WHERE deleted = false;

-- ===== MACHINING BATCH =====
ALTER TABLE machining_batch DROP CONSTRAINT IF EXISTS uq_machining_batch_number_tenant_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_machining_batch_number_tenant_active
    ON machining_batch (machining_batch_number, tenant_id) WHERE deleted = false;

-- ===== GAUGE =====
ALTER TABLE gauge DROP CONSTRAINT IF EXISTS uq_gauge_name_tenant_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_gauge_name_tenant_active
    ON gauge (gauge_name, tenant_id) WHERE deleted = false;

-- ===== INSPECTION BATCH =====
ALTER TABLE inspection_batch DROP CONSTRAINT IF EXISTS uq_inspection_batch_number_tenant_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_inspection_batch_number_tenant_active
    ON inspection_batch (inspection_batch_number, tenant_id) WHERE deleted = false;

-- ===== DISPATCH BATCH =====
ALTER TABLE dispatch_batch DROP CONSTRAINT IF EXISTS uq_dispatch_batch_number_tenant_deleted;
ALTER TABLE dispatch_batch DROP CONSTRAINT IF EXISTS uq_invoice_number_tenant_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_dispatch_batch_number_tenant_active
    ON dispatch_batch (dispatch_batch_number, tenant_id) WHERE deleted = false;
CREATE UNIQUE INDEX IF NOT EXISTS unique_invoice_number_tenant_active
    ON dispatch_batch (invoice_number, tenant_id) WHERE deleted = false AND invoice_number IS NOT NULL;

-- ===== BUYER =====
ALTER TABLE buyer DROP CONSTRAINT IF EXISTS unique_buyer_name_tenant_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_buyer_name_tenant_active
    ON buyer (buyer_name, tenant_id) WHERE deleted = false;

-- ===== BUYER ENTITY =====
ALTER TABLE buyer_entity DROP CONSTRAINT IF EXISTS unique_entity_name_buyer_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_buyer_entity_name_active
    ON buyer_entity (buyer_entity_name, buyer_id) WHERE deleted = false;

-- ===== DAILY MACHINING BATCH =====
ALTER TABLE daily_machining_batch DROP CONSTRAINT IF EXISTS uq_daily_machining_batch_number_machining_batch_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_daily_machining_batch_number_active
    ON daily_machining_batch (daily_machining_batch_number, machining_batch_id) WHERE deleted = false;

-- ===== WORKFLOW =====
ALTER TABLE workflow_template DROP CONSTRAINT IF EXISTS unique_workflow_name_tenant_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_workflow_name_tenant_active
    ON workflow_template (workflow_name, tenant_id) WHERE deleted = false;

-- ===== ITEM WORKFLOW STEP =====
ALTER TABLE item_workflow_step DROP CONSTRAINT IF EXISTS unique_item_workflow_step;
CREATE UNIQUE INDEX IF NOT EXISTS unique_item_workflow_step_active
    ON item_workflow_step (item_workflow_id, workflow_step_id) WHERE deleted = false;

-- ===== VENDOR =====
ALTER TABLE vendor DROP CONSTRAINT IF EXISTS unique_vendor_name_tenant_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_vendor_name_tenant_active
    ON vendor (vendor_name, tenant_id) WHERE deleted = false;

-- ===== VENDOR ENTITY =====
ALTER TABLE vendor_entity DROP CONSTRAINT IF EXISTS unique_entity_name_vendor_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_vendor_entity_name_active
    ON vendor_entity (vendor_entity_name, vendor_id) WHERE deleted = false;

-- ===== VENDOR DISPATCH BATCH =====
ALTER TABLE vendor_dispatch_batch DROP CONSTRAINT IF EXISTS unique_vendor_dispatch_batch_number_tenant_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_vendor_dispatch_batch_number_tenant_active
    ON vendor_dispatch_batch (vendor_dispatch_batch_number, tenant_id) WHERE deleted = false;

-- ===== VENDOR RECEIVE BATCH =====
ALTER TABLE vendor_receive_batch DROP CONSTRAINT IF EXISTS unique_vendor_receive_batch_number_tenant_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_vendor_receive_batch_number_tenant_active
    ON vendor_receive_batch (vendor_receive_batch_number, tenant_id) WHERE deleted = false;

-- ===== VENDOR INVENTORY =====
ALTER TABLE vendor_inventory DROP CONSTRAINT IF EXISTS uk_vendor_inventory_vendor_heat_deleted;
CREATE UNIQUE INDEX IF NOT EXISTS unique_vendor_inventory_vendor_heat_active
    ON vendor_inventory (vendor_id, original_heat_id) WHERE deleted = false;

COMMIT;
