-- Rollback Migration: Remove Transport Document Number and Date fields from delivery_challan table
-- Description: Rolls back the addition of transport document number (LR/RR/AWB/BL) and document date
--              fields that were added for E-Way Bill generation compliance
-- Original Migration: V1_82__update_trans_details_in_delivery_challan.sql
-- Author: System
-- Date: 2025-11-27

-- Drop indexes first (must be done before dropping columns)
DROP INDEX IF EXISTS idx_delivery_challan_transport_doc_no;
DROP INDEX IF EXISTS idx_delivery_challan_transport_doc_date;

-- Drop transport_document_date column
ALTER TABLE delivery_challan
DROP COLUMN IF EXISTS transport_document_date;

-- Drop transport_document_number column
ALTER TABLE delivery_challan
DROP COLUMN IF EXISTS transport_document_number;

-- Revert vehicle_number column comment to original
COMMENT ON COLUMN delivery_challan.vehicle_number IS
'Vehicle number for transportation';

-- Revert table comment to original (without transport document details)
COMMENT ON TABLE delivery_challan IS
'Delivery Challan entity - represents goods movement documents for GST compliance. Supports E-Way Bill generation.';
