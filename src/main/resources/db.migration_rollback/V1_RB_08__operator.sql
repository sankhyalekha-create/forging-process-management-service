-- Drop the index on daily_machining_batch
DROP INDEX IF EXISTS idx_daily_machining_batch_machine_operator;

-- Remove the machine_operator_id column from daily_machining_batch
ALTER TABLE daily_machining_batch DROP COLUMN IF EXISTS machine_operator_id;

-- Drop the machine_operator table
DROP TABLE IF EXISTS machine_operator;

-- Drop the sequence for MachineOperator entity
DROP SEQUENCE IF EXISTS machine_operator_sequence;

-- Drop the operator_previous_tenants table
DROP TABLE IF EXISTS operator_previous_tenants;

-- Drop the indexes for Operator table
DROP INDEX IF EXISTS idx_operator_aadhaar;
DROP INDEX IF EXISTS idx_operator_tenant;
DROP INDEX IF EXISTS unique_aadhaar_not_deleted;

-- Drop the operator table
DROP TABLE IF EXISTS operator;

-- Drop the sequence for Operator entity
DROP SEQUENCE IF EXISTS operator_sequence;
