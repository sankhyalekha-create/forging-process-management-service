-- Create sequence for Operator entity
CREATE SEQUENCE operator_sequence START WITH 1 INCREMENT BY 1;

-- Create Operator table (Base Entity)
CREATE TABLE operator (
                          id BIGINT PRIMARY KEY DEFAULT nextval('operator_sequence'),
                          full_name VARCHAR(255) NOT NULL,
                          address TEXT NOT NULL,
                          aadhaar_number VARCHAR(20) NOT NULL,
                          tenant_id BIGINT NOT NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          deleted_at TIMESTAMP NULL,
                          deleted BOOLEAN DEFAULT FALSE
);

-- Create a partial unique index on aadhaar_number where deleted is false
CREATE UNIQUE INDEX unique_aadhaar_not_deleted
    ON operator (aadhaar_number)
    WHERE deleted = FALSE;

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
    id BIGINT PRIMARY KEY REFERENCES operator(id) ON DELETE CASCADE
);

-- Updating MachiningBatch table to include MachineOperator
ALTER TABLE daily_machining_batch ADD COLUMN machine_operator_id BIGINT REFERENCES machine_operator(id) ON DELETE SET NULL;

-- Index for faster lookup
CREATE INDEX idx_daily_machining_batch_machine_operator ON daily_machining_batch (machine_operator_id);
