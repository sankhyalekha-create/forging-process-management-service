-- Migration: Add Transport Document Number and Date fields to delivery_challan table
-- Description: Adds support for transport document number (LR/RR/AWB/BL) and document date
--              for E-Way Bill generation compliance
-- Author: System
-- Date: 2025-11-27

-- Add transport_document_number column
ALTER TABLE delivery_challan
ADD COLUMN transport_document_number VARCHAR(50);

COMMENT ON COLUMN delivery_challan.transport_document_number IS
'Transport Document Number - LR/RR/AWB/BL Number. For Road: Lorry Receipt (LR) optional, For Rail: Railway Receipt (RR) mandatory, For Air: Airway Bill (AWB) mandatory, For Ship: Bill of Lading (BL) mandatory';

-- Add transport_document_date column
ALTER TABLE delivery_challan
ADD COLUMN transport_document_date DATE;

COMMENT ON COLUMN delivery_challan.transport_document_date IS
'Transport Document Date - Date when the transport document (LR/RR/AWB/BL) was issued';

-- Create index for transport_document_number for faster lookups
CREATE INDEX idx_delivery_challan_transport_doc_no ON delivery_challan(transport_document_number)
WHERE transport_document_number IS NOT NULL;

-- Create index for transport_document_date for reporting and filtering
CREATE INDEX idx_delivery_challan_transport_doc_date ON delivery_challan(transport_document_date)
WHERE transport_document_date IS NOT NULL;

-- Update vehicle_number column comment to clarify its usage based on transport mode
COMMENT ON COLUMN delivery_challan.vehicle_number IS
'Transport identifier - stores vehicle/transport number based on transportation mode: ROAD: Vehicle registration number (e.g., MH12AB1234), RAIL: Railway Receipt Number (RR), AIR: Airway Bill Number (AWB), SHIP: Bill of Lading Number (BL)';

COMMENT ON TABLE delivery_challan IS
'Delivery Challan entity - represents goods movement documents for GST compliance. Supports E-Way Bill generation with transport details (Part A & Part B). Transport document fields enable compliance with different transport modes.';
