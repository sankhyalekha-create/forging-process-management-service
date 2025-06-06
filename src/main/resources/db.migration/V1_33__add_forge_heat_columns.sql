-- Migration to add heat_quantity_returned column to existing forge_heat table
-- This adds tracking for returned quantities after forge completion

-- Add only the missing column for tracking returned quantities
ALTER TABLE forge_heat 
ADD COLUMN IF NOT EXISTS heat_quantity_returned DOUBLE PRECISION DEFAULT 0.0;

-- Add constraint for the new column
ALTER TABLE forge_heat 
ADD CONSTRAINT chk_forge_heat_returned_quantity_positive 
    CHECK (heat_quantity_returned >= 0.0);

-- Add comment for documentation
COMMENT ON COLUMN forge_heat.heat_quantity_returned IS 'Heat quantity returned to inventory when forge completed';

-- Create index for queries involving returned quantities (for audit reports)
CREATE INDEX IF NOT EXISTS idx_forge_heat_returned_quantity 
    ON forge_heat(heat_quantity_returned) 
    WHERE heat_quantity_returned > 0.0;

-- Example queries for reporting and analytics using the new returned quantity tracking:
/*
-- Material efficiency report: Compare allocated vs used vs returned quantities
SELECT 
    h.heat_number,
    fh.heat_quantity_used as allocated_quantity,
    fh.heat_quantity_returned as returned_quantity,
    (fh.heat_quantity_used - fh.heat_quantity_returned) as net_used_quantity,
    ROUND((fh.heat_quantity_returned / fh.heat_quantity_used * 100), 2) as return_percentage
FROM forge_heat fh
JOIN heat h ON fh.heat_id = h.id
JOIN forge f ON fh.forge_id = f.id
WHERE f.forging_status = 'COMPLETED'
    AND fh.heat_quantity_returned > 0
ORDER BY return_percentage DESC;

-- Allocation accuracy analysis: Find forges with high return rates
SELECT 
    f.forge_traceability_number,
    i.item_name,
    SUM(fh.heat_quantity_used) as total_allocated,
    SUM(fh.heat_quantity_returned) as total_returned,
    ROUND(SUM(fh.heat_quantity_returned) / SUM(fh.heat_quantity_used) * 100, 2) as total_return_percentage
FROM forge f
JOIN forge_heat fh ON f.id = fh.forge_id
JOIN processed_item pi ON f.processed_item_id = pi.id
JOIN item i ON pi.item_id = i.id
WHERE f.forging_status = 'COMPLETED'
GROUP BY f.id, f.forge_traceability_number, i.item_name
HAVING SUM(fh.heat_quantity_returned) > 0
ORDER BY total_return_percentage DESC;

-- Heat inventory audit trail: Track all movements for a specific heat
SELECT 
    'ALLOCATION' as transaction_type,
    f.forge_traceability_number as reference,
    fh.heat_quantity_used as quantity,
    f.apply_at as transaction_date
FROM forge_heat fh
JOIN forge f ON fh.forge_id = f.id
WHERE fh.heat_id = [HEAT_ID]
UNION ALL
SELECT 
    'RETURN' as transaction_type,
    f.forge_traceability_number as reference,
    -fh.heat_quantity_returned as quantity,
    f.end_at as transaction_date
FROM forge_heat fh
JOIN forge f ON fh.forge_id = f.id
WHERE fh.heat_id = [HEAT_ID] AND fh.heat_quantity_returned > 0
ORDER BY transaction_date;
*/ 