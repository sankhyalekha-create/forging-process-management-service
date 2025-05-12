-- Remove comments from the columns
COMMENT ON COLUMN operator.date_of_birth IS NULL;
COMMENT ON COLUMN operator.date_of_joining IS NULL;
COMMENT ON COLUMN operator.date_of_leaving IS NULL;
COMMENT ON COLUMN operator.hourly_wages IS NULL;

-- Remove the columns added by the migration
ALTER TABLE operator DROP COLUMN IF EXISTS date_of_birth;
ALTER TABLE operator DROP COLUMN IF EXISTS date_of_joining;
ALTER TABLE operator DROP COLUMN IF EXISTS date_of_leaving;
ALTER TABLE operator DROP COLUMN IF EXISTS hourly_wages; 