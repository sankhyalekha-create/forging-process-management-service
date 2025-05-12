-- Add new columns to operator table
ALTER TABLE operator ADD COLUMN date_of_birth DATE;
ALTER TABLE operator ADD COLUMN date_of_joining DATE;
ALTER TABLE operator ADD COLUMN date_of_leaving DATE;
ALTER TABLE operator ADD COLUMN hourly_wages DECIMAL(10, 2);

-- Set default values for existing operators
-- Using '2000-01-01' as a default date of birth
-- Using created_at date as the date of joining
-- Setting a default hourly wage of 100
UPDATE operator 
SET date_of_birth = '2000-01-01',
    date_of_joining = CAST(created_at AS DATE),
    hourly_wages = 100.00
WHERE date_of_birth IS NULL;

-- Add a comment explaining the purpose of these fields
COMMENT ON COLUMN operator.date_of_birth IS 'Date of birth of the operator';
COMMENT ON COLUMN operator.date_of_joining IS 'Date when the operator joined the company';
COMMENT ON COLUMN operator.date_of_leaving IS 'Date when the operator left the company, NULL for active operators';
COMMENT ON COLUMN operator.hourly_wages IS 'Hourly wages paid to the operator in the local currency'; 