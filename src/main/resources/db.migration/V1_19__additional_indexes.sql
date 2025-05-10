-- Additional indexes to optimize the database queries

-- Optimizing common filters in repositories
-- These indexes will speed up queries that filter by tenantId and deleted=false,
-- and those that sort by createdAt in descending order

-- Indexes for Item table
CREATE INDEX IF NOT EXISTS idx_item_tenant_deleted_created ON item (tenant_id, deleted, created_at DESC);

-- Indexes for Buyer table
CREATE INDEX IF NOT EXISTS idx_buyer_tenant_deleted_created ON buyer (tenant_id, deleted, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_buyer_gstin_tenant_deleted ON buyer (gstin_uin, tenant_id, deleted);
CREATE INDEX IF NOT EXISTS idx_buyer_name_like ON buyer (tenant_id, deleted) INCLUDE (buyer_name);

-- Indexes for Forge table
CREATE INDEX IF NOT EXISTS idx_forge_tenant_deleted_created ON forge (tenant_id, deleted, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_forge_status ON forge (forging_status, tenant_id) WHERE deleted = false;

-- Indexes for ForgeHeat table
CREATE INDEX IF NOT EXISTS idx_forge_heat_deleted ON forge_heat (deleted);

-- Buyer Entity indexes
CREATE INDEX IF NOT EXISTS idx_buyer_entity_tenant_deleted_created ON buyer_entity (buyer_id, deleted, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_buyer_entity_billing ON buyer_entity (is_billing_entity) WHERE deleted = false AND is_billing_entity = true;
CREATE INDEX IF NOT EXISTS idx_buyer_entity_shipping ON buyer_entity (is_shipping_entity) WHERE deleted = false AND is_shipping_entity = true;

-- Additional indexes for search queries where LIKE operations might be used
CREATE INDEX IF NOT EXISTS idx_item_name_lower ON item (lower(item_name)) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_buyer_name_lower ON buyer (lower(buyer_name)) WHERE deleted = false;

-- Common query pattern indexes (based on repository methods)
CREATE INDEX IF NOT EXISTS idx_item_id_tenant_deleted ON item (id, tenant_id, deleted);
CREATE INDEX IF NOT EXISTS idx_buyer_id_tenant_deleted ON buyer (id, tenant_id, deleted);

-- Joint index for common patterns in most repositories
-- This helps with queries like: findByTenantIdAndDeletedFalseOrderByCreatedAtDesc
-- which appears in multiple repositories
CREATE INDEX IF NOT EXISTS idx_forging_line_tenant_deleted_created 
ON forging_line (tenant_id, deleted, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_processed_item_deleted_created 
ON processed_item (deleted, created_at DESC);

-- Indexes for any table with existence checks
CREATE INDEX IF NOT EXISTS idx_heat_id_deleted ON heat (id, deleted); 