DROP INDEX IF EXISTS idx_raw_material_heat_receiving_report_number;
DROP INDEX IF EXISTS idx_raw_material_heat_certificate_number;
DROP INDEX IF EXISTS idx_raw_material_heat_number;

DROP TABLE raw_material_heat;
DROP SEQUENCE raw_material_heat_sequence;

DROP INDEX IF EXISTS idx_raw_material_invoice_number;
DROP INDEX IF EXISTS idx_raw_material_input_code;
DROP INDEX IF EXISTS idx_raw_material_hsn_code;
DROP INDEX IF EXISTS idx_raw_material_tenant_id;
DROP SEQUENCE raw_material_sequence;

DROP TABLE raw_material;

DROP TABLE Tenant;
DROP SEQUENCE tenant_sequence;
