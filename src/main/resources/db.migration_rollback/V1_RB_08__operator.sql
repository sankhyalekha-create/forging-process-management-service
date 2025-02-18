-- Drop indexes
DROP INDEX IF EXISTS idx_machining_batch_machine_operator;
DROP INDEX IF EXISTS idx_machine_operator_machining_batch;
DROP INDEX IF EXISTS idx_operator_aadhaar;
DROP INDEX IF EXISTS idx_operator_tenant;

-- Remove foreign key from machining_batch table
ALTER TABLE machining_batch DROP COLUMN IF EXISTS machine_operator_id;

-- Drop MachineOperator table
DROP TABLE IF EXISTS machine_operator;

-- Drop MachineOperator sequence
DROP SEQUENCE IF EXISTS machine_operator_sequence;

-- Drop operator_previous_tenants table
DROP TABLE IF EXISTS operator_previous_tenants;

-- Drop Operator table
DROP TABLE IF EXISTS operator;

-- Drop Operator sequence
DROP SEQUENCE IF EXISTS operator_sequence;
