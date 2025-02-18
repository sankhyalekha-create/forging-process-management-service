-- Create sequence for Operator entity
CREATE SEQUENCE operator_sequence START WITH 1 INCREMENT BY 1;

-- Create Operator table (Base Entity)
CREATE TABLE operator (
                          id BIGINT PRIMARY KEY DEFAULT nextval('operator_sequence'),
                          full_name VARCHAR(255) NOT NULL,
                          address TEXT NOT NULL,
                          aadhaar_number VARCHAR(20) NOT NULL UNIQUE,
                          tenant_id BIGINT NOT NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          deleted_at TIMESTAMP NULL,
                          deleted BOOLEAN DEFAULT FALSE
);

-- Indexes for Operator table
CREATE INDEX idx_operator_tenant ON operator (tenant_id);
CREATE INDEX idx_operator_aadhaar ON operator (aadhaar_number);

-- Create previous tenant association table
CREATE TABLE operator_previous_tenants (
                                           operator_id BIGINT NOT NULL REFERENCES operator(id) ON DELETE CASCADE,
                                           previous_tenant_id BIGINT NOT NULL
);

-- Create sequence for MachineOperator entity
CREATE SEQUENCE machine_operator_sequence START WITH 1 INCREMENT BY 1;

-- Create MachineOperator table
CREATE TABLE machine_operator (
                                  id BIGINT PRIMARY KEY DEFAULT nextval('machine_operator_sequence'),
                                  machining_batch_id BIGINT UNIQUE REFERENCES machining_batch(id) ON DELETE SET NULL
) INHERITS (operator);

-- Indexes for MachineOperator
CREATE INDEX idx_machine_operator_machining_batch ON machine_operator (machining_batch_id);

-- Updating MachiningBatch table to include MachineOperator
ALTER TABLE machining_batch ADD COLUMN machine_operator_id BIGINT UNIQUE REFERENCES machine_operator(id) ON DELETE SET NULL;
CREATE INDEX idx_machining_batch_machine_operator ON machining_batch (machine_operator_id);
