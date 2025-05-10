-- Rollback script for additional indexes

-- Drop indexes for heat table
DROP INDEX IF EXISTS idx_heat_id_deleted;

-- Drop indexes for processed_item table
DROP INDEX IF EXISTS idx_processed_item_deleted_created;

-- Drop indexes for forging_line table
DROP INDEX IF EXISTS idx_forging_line_tenant_deleted_created;

-- Drop common query pattern indexes
DROP INDEX IF EXISTS idx_buyer_id_tenant_deleted;
DROP INDEX IF EXISTS idx_item_id_tenant_deleted;

-- Drop LIKE operation indexes
DROP INDEX IF EXISTS idx_buyer_name_lower;
DROP INDEX IF EXISTS idx_item_name_lower;

-- Drop Buyer Entity indexes
DROP INDEX IF EXISTS idx_buyer_entity_shipping;
DROP INDEX IF EXISTS idx_buyer_entity_billing;
DROP INDEX IF EXISTS idx_buyer_entity_tenant_deleted_created;

-- Drop ForgeHeat indexes
DROP INDEX IF EXISTS idx_forge_heat_deleted;

-- Drop Forge indexes
DROP INDEX IF EXISTS idx_forge_status;
DROP INDEX IF EXISTS idx_forge_tenant_deleted_created;

-- Drop Buyer indexes
DROP INDEX IF EXISTS idx_buyer_name_like;
DROP INDEX IF EXISTS idx_buyer_gstin_tenant_deleted;
DROP INDEX IF EXISTS idx_buyer_tenant_deleted_created;

-- Drop Item indexes
DROP INDEX IF EXISTS idx_item_tenant_deleted_created; 