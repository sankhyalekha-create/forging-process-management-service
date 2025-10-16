-- Rollback: Remove ORDER and ITEM_WORKFLOW entity types from document_link
-- Description: Rollback document management support for Order and ItemWorkflow entities

-- Drop existing constraint
ALTER TABLE document_link DROP CONSTRAINT IF EXISTS document_link_entity_type_check;

-- Restore original constraint without ORDER and ITEM_WORKFLOW
ALTER TABLE document_link ADD CONSTRAINT document_link_entity_type_check 
    CHECK (entity_type IN (
        'RAW_MATERIAL', 'ITEM', 'PRODUCT',
        'FORGE', 'MACHINING_BATCH', 'HEAT_TREATMENT_BATCH', 'INSPECTION_BATCH', 'DISPATCH_BATCH',
        'FORGING_LINE', 'MACHINE', 'MACHINE_SET', 'FURNACE', 'INSPECTION_EQUIPMENT', 'OPERATOR',
        'VENDOR_DISPATCH_BATCH', 'VENDOR_RECEIVE_BATCH', 'BUYER', 'SUPPLIER',
        'TENANT', 'OTHER'
    ));

-- Note: Any ORDER or ITEM_WORKFLOW document links will need to be manually removed before running this rollback
-- Run this query before rollback if needed:
-- DELETE FROM document_link WHERE entity_type IN ('ORDER', 'ITEM_WORKFLOW');

COMMIT;

