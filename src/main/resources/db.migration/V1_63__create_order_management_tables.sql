-- Migration Script: V1_63__create_order_management_tables.sql
-- Description: Create complete Order Management system tables including tenant settings
-- Author: System Generated
-- Date: 2024-10-04
-- Version: Consolidated migration for development phase (includes V1_63 and V1_64)

-- Create orders table
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    po_number VARCHAR(100) NOT NULL,
    order_date DATE NOT NULL,
    buyer_id BIGINT NOT NULL,
    expected_processing_days INTEGER,
    user_defined_eta_days INTEGER,
    order_status VARCHAR(50) NOT NULL DEFAULT 'RECEIVED',
    notes VARCHAR(2000),
    priority INTEGER NOT NULL DEFAULT 1,
    tenant_id BIGINT NOT NULL,
    actual_start_date DATE,
    actual_completion_date DATE,
    actual_duration_days INTEGER,
    has_inventory_shortage BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Foreign key constraints
    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id) REFERENCES buyer(id),
    CONSTRAINT fk_orders_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    
    -- Check constraints
    CONSTRAINT chk_orders_priority CHECK (priority >= 1),
    CONSTRAINT chk_orders_processing_days CHECK (expected_processing_days IS NULL OR expected_processing_days > 0),
    CONSTRAINT chk_orders_eta_days CHECK (user_defined_eta_days IS NULL OR user_defined_eta_days > 0),
    CONSTRAINT chk_orders_actual_duration CHECK (actual_duration_days IS NULL OR actual_duration_days > 0),
    CONSTRAINT chk_orders_status CHECK (order_status IN ('RECEIVED', 'PLANNING', 'IN_PROGRESS', 'COMPLETED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT chk_orders_dates CHECK (
        (actual_start_date IS NULL AND actual_completion_date IS NULL) OR
        (actual_start_date IS NOT NULL AND actual_completion_date IS NULL) OR
        (actual_start_date IS NOT NULL AND actual_completion_date IS NOT NULL AND actual_completion_date >= actual_start_date)
    )
);

-- Create indexes for orders table
CREATE UNIQUE INDEX idx_order_po_number_tenant_id ON orders(po_number, tenant_id) WHERE deleted = FALSE;
CREATE INDEX idx_order_tenant_id ON orders(tenant_id);
CREATE INDEX idx_order_status_tenant_id ON orders(order_status, tenant_id);
CREATE INDEX idx_order_date_tenant_id ON orders(order_date, tenant_id);
CREATE INDEX idx_order_buyer_id ON orders(buyer_id);
CREATE INDEX idx_order_priority_tenant_id ON orders(priority, tenant_id);
CREATE INDEX idx_order_expected_completion ON orders(order_date, expected_processing_days, user_defined_eta_days) WHERE deleted = FALSE;
CREATE INDEX idx_orders_inventory_shortage ON orders(tenant_id, has_inventory_shortage) WHERE deleted = FALSE;

-- Create order_items table
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    work_type VARCHAR(50) NOT NULL DEFAULT 'WITH_MATERIAL',
    unit_price DECIMAL(10,2),
    material_cost_per_unit DECIMAL(10,2),
    job_work_cost_per_unit DECIMAL(10,2),
    special_instructions VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_item FOREIGN KEY (item_id) REFERENCES item(id),
    
    -- Check constraints
    CONSTRAINT chk_order_items_quantity CHECK (quantity >= 1),
    CONSTRAINT chk_order_items_unit_price CHECK (unit_price IS NULL OR unit_price >= 0),
    CONSTRAINT chk_order_items_material_cost CHECK (material_cost_per_unit IS NULL OR material_cost_per_unit >= 0),
    CONSTRAINT chk_order_items_job_work_cost CHECK (job_work_cost_per_unit IS NULL OR job_work_cost_per_unit >= 0),
    CONSTRAINT chk_order_items_work_type CHECK (work_type IN ('JOB_WORK_ONLY', 'WITH_MATERIAL'))
);

-- Create indexes for order_items table
CREATE INDEX idx_order_item_order_id ON order_items(order_id);
CREATE INDEX idx_order_item_item_id ON order_items(item_id);

-- Create order_item_workflows table
CREATE TABLE order_item_workflows (
    id BIGINT PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    item_workflow_id BIGINT NOT NULL,
    planned_duration_days INTEGER,
    actual_start_date DATE,
    actual_completion_date DATE,
    actual_duration_days INTEGER,
    notes VARCHAR(1000),
    priority INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_order_item_workflows_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_workflows_item_workflow FOREIGN KEY (item_workflow_id) REFERENCES item_workflow(id),
    
    -- Check constraints
    CONSTRAINT chk_order_item_workflows_planned_duration CHECK (planned_duration_days IS NULL OR planned_duration_days >= 1),
    CONSTRAINT chk_order_item_workflows_actual_duration CHECK (actual_duration_days IS NULL OR actual_duration_days >= 1),
    CONSTRAINT chk_order_item_workflows_priority CHECK (priority >= 1),
    CONSTRAINT chk_order_item_workflows_dates CHECK (
        (actual_start_date IS NULL AND actual_completion_date IS NULL) OR
        (actual_start_date IS NOT NULL AND actual_completion_date IS NULL) OR
        (actual_start_date IS NOT NULL AND actual_completion_date IS NOT NULL AND actual_completion_date >= actual_start_date)
    )
);

-- Create indexes for order_item_workflows table
CREATE INDEX idx_order_item_workflow_order_item_id ON order_item_workflows(order_item_id);
CREATE UNIQUE INDEX idx_order_item_workflow_item_workflow_id ON order_item_workflows(item_workflow_id);
CREATE INDEX idx_order_item_workflow_priority ON order_item_workflows(priority);

-- Create tenant_order_settings table for storing tenant-level order management preferences
CREATE TABLE IF NOT EXISTS tenant_order_settings (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL UNIQUE,
    
    -- Highlighting Settings
    warning_days INTEGER NOT NULL DEFAULT 3 CHECK (warning_days >= 1 AND warning_days <= 14),
    enable_highlighting BOOLEAN NOT NULL DEFAULT TRUE,
    overdue_color VARCHAR(20) DEFAULT '#ffebee',
    warning_color VARCHAR(20) DEFAULT '#fff8e1',
    completed_color VARCHAR(20) DEFAULT '#e8f5e9',
    
    -- Display Settings
    auto_refresh_interval INTEGER NOT NULL DEFAULT 30 CHECK (auto_refresh_interval >= 10 AND auto_refresh_interval <= 300),
    enable_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    show_completed_orders BOOLEAN NOT NULL DEFAULT TRUE,
    default_priority INTEGER NOT NULL DEFAULT 3 CHECK (default_priority >= 1 AND default_priority <= 5),
    
    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_tenant_order_settings_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

-- Create index for faster lookups on tenant_order_settings
CREATE INDEX idx_tenant_order_settings_tenant ON tenant_order_settings(tenant_id);

-- Create sequences for auto-generated IDs
CREATE SEQUENCE IF NOT EXISTS order_sequence START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS order_item_sequence START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS order_item_workflow_sequence START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS tenant_order_settings_sequence START WITH 1 INCREMENT BY 1;

-- Add comments for documentation
COMMENT ON TABLE orders IS 'Main orders table storing customer orders with buyer information';
COMMENT ON TABLE order_items IS 'Items within each order with quantity and pricing information';
COMMENT ON TABLE order_item_workflows IS 'Workflow tracking for each order item with timeline management';
COMMENT ON TABLE tenant_order_settings IS 'Stores tenant-level preferences for order management interface';

COMMENT ON COLUMN orders.po_number IS 'Purchase Order Number - must be unique per tenant';
COMMENT ON COLUMN orders.expected_processing_days IS 'System calculated expected processing days based on workflows';
COMMENT ON COLUMN orders.user_defined_eta_days IS 'User override for ETA calculation - takes priority over calculated ETA';
COMMENT ON COLUMN orders.priority IS 'Order priority: 1=highest, higher numbers=lower priority';
COMMENT ON COLUMN orders.has_inventory_shortage IS 'Flag indicating if order has material inventory shortage. Set during order creation based on available raw material stock';
COMMENT ON COLUMN order_items.work_type IS 'Type of work: JOB_WORK_ONLY (only processing charges) or WITH_MATERIAL (material + processing charges)';
COMMENT ON COLUMN order_items.material_cost_per_unit IS 'Material cost per unit (applicable for WITH_MATERIAL work type)';
COMMENT ON COLUMN order_items.job_work_cost_per_unit IS 'Job work (processing) cost per unit';
COMMENT ON COLUMN order_items.unit_price IS 'Total unit price based on work type (calculated from material_cost + job_work_cost)';
COMMENT ON COLUMN order_item_workflows.planned_duration_days IS 'Planned duration for this workflow in days';
COMMENT ON COLUMN order_item_workflows.priority IS 'Workflow priority: 1=highest, higher numbers=lower priority';

COMMENT ON COLUMN tenant_order_settings.warning_days IS 'Number of days before deadline to show warning (1-14)';
COMMENT ON COLUMN tenant_order_settings.enable_highlighting IS 'Enable/disable order highlighting based on deadlines';
COMMENT ON COLUMN tenant_order_settings.overdue_color IS 'CSS color code for overdue order highlights';
COMMENT ON COLUMN tenant_order_settings.warning_color IS 'CSS color code for warning order highlights';
COMMENT ON COLUMN tenant_order_settings.completed_color IS 'CSS color code for completed order highlights';
COMMENT ON COLUMN tenant_order_settings.auto_refresh_interval IS 'Auto refresh interval in seconds (10-300)';
COMMENT ON COLUMN tenant_order_settings.enable_notifications IS 'Enable/disable browser notifications for orders';
COMMENT ON COLUMN tenant_order_settings.show_completed_orders IS 'Include completed orders in main list';
COMMENT ON COLUMN tenant_order_settings.default_priority IS 'Default priority for new orders (1=highest, 5=lowest)';

-- Development Phase Notes:
-- - delivery_address field intentionally omitted - use buyer's shipping entity address
-- - Consolidated migration combining table creation and settings
-- - Designed for optimal performance with proper indexing strategy
-- - Supports complete order lifecycle from receipt to delivery
-- - Includes tenant-level settings for UI customization
-- - Includes work_type and cost breakdown fields for order items (V1_66 merged)
-- - Includes inventory shortage tracking (V1_67 merged)

COMMIT;
