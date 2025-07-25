-- Add active status column to heat table
-- This enables customers to mark heats as active/inactive to manage inventory with low quantities

-- Add the active column with default value true for existing data
ALTER TABLE heat ADD COLUMN active BOOLEAN NOT NULL DEFAULT true;

-- Add comment to document the purpose
COMMENT ON COLUMN heat.active IS 'Indicates if the heat is active and should be included in regular inventory lists. Inactive heats are filtered out from standard inventory views but can be viewed separately.';

-- Create index for better performance on active status queries
CREATE INDEX idx_heat_active ON heat(active) WHERE deleted = false;

-- Create composite index for tenant queries with active status
CREATE INDEX idx_heat_tenant_active ON heat(id) 
  INCLUDE (active) 
  WHERE deleted = false;

-- Update existing heats to be active by default (redundant but explicit)
UPDATE heat SET active = true WHERE active IS NULL;

-- Add constraint to ensure active column is not null
-- (Already covered by NOT NULL constraint above, but good for documentation)
-- ALTER TABLE heat ALTER COLUMN active SET NOT NULL;