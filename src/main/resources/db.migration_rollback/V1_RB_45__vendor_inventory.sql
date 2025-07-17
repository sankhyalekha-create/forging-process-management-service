-- Rollback Migration: Drop Vendor Inventory System
-- Version: V1_RB_45
-- Description: Rollback vendor inventory tracking system - removes vendor_inventory table and related objects

BEGIN;

-- Drop indexes for transaction tables first (in reverse order of creation)
DROP INDEX IF EXISTS idx_vendor_inventory_transaction_items_created_at;
DROP INDEX IF EXISTS idx_vendor_inventory_transaction_items_heat_number;
DROP INDEX IF EXISTS idx_vendor_inventory_transaction_items_heat_id;
DROP INDEX IF EXISTS idx_vendor_inventory_transaction_items_transaction_id;

DROP INDEX IF EXISTS idx_vendor_inventory_transactions_created_at;
DROP INDEX IF EXISTS idx_vendor_inventory_transactions_date_time;
DROP INDEX IF EXISTS idx_vendor_inventory_transactions_type;
DROP INDEX IF EXISTS idx_vendor_inventory_transactions_vendor_id;
DROP INDEX IF EXISTS idx_vendor_inventory_transactions_tenant_id;

-- Drop transaction tables (in reverse order of dependencies)
DROP TABLE IF EXISTS vendor_inventory_transaction_items;
DROP TABLE IF EXISTS vendor_inventory_transactions;

-- Drop transaction sequences
DROP SEQUENCE IF EXISTS vendor_inventory_transaction_items_sequence;
DROP SEQUENCE IF EXISTS vendor_inventory_transactions_sequence;

-- Drop indexes for vendor_inventory table (in reverse order of creation)
DROP INDEX IF EXISTS idx_vendor_inventory_available_pieces;
DROP INDEX IF EXISTS idx_vendor_inventory_available_quantity;
DROP INDEX IF EXISTS idx_vendor_inventory_created_at;
DROP INDEX IF EXISTS idx_vendor_inventory_heat_number;
DROP INDEX IF EXISTS idx_vendor_inventory_raw_material_product_id;
DROP INDEX IF EXISTS idx_vendor_inventory_original_heat_id;
DROP INDEX IF EXISTS idx_vendor_inventory_vendor_id;

-- Drop the vendor_inventory table (this will also drop all constraints)
DROP TABLE IF EXISTS vendor_inventory;

-- Drop the vendor_inventory sequence
DROP SEQUENCE IF EXISTS vendor_inventory_sequence;

COMMIT; 