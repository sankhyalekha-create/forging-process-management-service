-- Drop the index for processed_item_id in processed_item_heat_treatment_batch table
DROP INDEX IF EXISTS idx_processed_item_heat_treatment_batch_processed_item_id;

-- Drop the index for heat_treatment_batch_id in processed_item_heat_treatment_batch table
DROP INDEX IF EXISTS idx_processed_item_heat_treatment_batch_heat_treatment_batch_id;

-- Drop the table processed_item_heat_treatment_batch
DROP TABLE IF EXISTS processed_item_heat_treatment_batch;

-- Drop the sequence for processed item heat treatment batch ID generation
DROP SEQUENCE IF EXISTS processed_item_ht_batch_sequence;

-- Drop the index for heat_treatment_batch_number in heat_treatment_batch table
DROP INDEX IF EXISTS idx_heat_treatment_batch_number;

-- Drop the index for furnace_id in heat_treatment_batch table
DROP INDEX IF EXISTS idx_heat_treatment_batch_furnace_id;

-- Drop the table heat_treatment_batch
DROP TABLE IF EXISTS heat_treatment_batch;

-- Drop the sequence for heat treatment batch ID generation
DROP SEQUENCE IF EXISTS heat_treatment_batch_sequence;

-- Drop the index for furnace_name and tenant_id in furnace table
DROP INDEX IF EXISTS idx_furnace_name_tenant_id;

-- Drop the table furnace
DROP TABLE IF EXISTS furnace;

-- Drop the sequence for furnace ID generation
DROP SEQUENCE IF EXISTS furnace_sequence;
