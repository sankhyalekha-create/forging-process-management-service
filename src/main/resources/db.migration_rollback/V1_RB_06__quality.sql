-- Rollback Script

-- Drop Indexes for GaugeInspectionReport Table
DROP INDEX IF EXISTS idx_gir_finished_pieces_count;
DROP INDEX IF EXISTS idx_gir_rejected_pieces_count;
DROP INDEX IF EXISTS idx_gir_rework_pieces_count;

-- Drop GaugeInspectionReport Table
DROP TABLE IF EXISTS gauge_inspection_report;

-- Drop Indexes for ProcessedItemInspectionBatch Table
DROP INDEX IF EXISTS idx_piib_item_status;
DROP INDEX IF EXISTS idx_piib_finished_pieces_count;
DROP INDEX IF EXISTS idx_piib_reject_pieces_count;
DROP INDEX IF EXISTS idx_piib_rework_pieces_count;

-- Drop ProcessedItemInspectionBatch Table
DROP TABLE IF EXISTS processed_item_inspection_batch;

-- Drop Indexes for InspectionBatch Table
DROP INDEX IF EXISTS idx_inspection_batch_status;
DROP INDEX IF EXISTS idx_inspection_batch_number;


-- Drop InspectionBatch Table
DROP TABLE IF EXISTS inspection_batch;

-- Drop Indexes for Gauge Table
DROP INDEX IF EXISTS idx_gauge_name;
DROP INDEX IF EXISTS idx_gauge_name_tenant_id;

-- Drop Gauge Table
DROP TABLE IF EXISTS gauge;

-- Drop Sequences
DROP SEQUENCE IF EXISTS gauge_sequence;
DROP SEQUENCE IF EXISTS inspection_batch_sequence;
DROP SEQUENCE IF EXISTS processed_item_inspection_batch_sequence;
DROP SEQUENCE IF EXISTS gauge_inspection_report_sequence;
