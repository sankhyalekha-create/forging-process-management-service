-- Migration: Add ORDER and ITEM_WORKFLOW entity types to document_link
-- Description: Extend document management to support Order and ItemWorkflow entities

-- Drop existing constraint
ALTER TABLE document_link DROP CONSTRAINT IF EXISTS document_link_entity_type_check;

-- Add new constraint with ORDER and ITEM_WORKFLOW entity types
ALTER TABLE document_link ADD CONSTRAINT document_link_entity_type_check 
    CHECK (entity_type IN (
        'RAW_MATERIAL', 'ITEM', 'PRODUCT',
        'ORDER', 'ITEM_WORKFLOW',
        'FORGE', 'MACHINING_BATCH', 'HEAT_TREATMENT_BATCH', 'INSPECTION_BATCH', 'DISPATCH_BATCH',
        'FORGING_LINE', 'MACHINE', 'MACHINE_SET', 'FURNACE', 'INSPECTION_EQUIPMENT', 'OPERATOR',
        'VENDOR_DISPATCH_BATCH', 'VENDOR_RECEIVE_BATCH', 'BUYER', 'SUPPLIER',
        'TENANT', 'OTHER'
    ));

-- Add comment
COMMENT ON CONSTRAINT document_link_entity_type_check ON document_link IS 'Updated to include ORDER and ITEM_WORKFLOW entity types for order management document support';

COMMIT;

