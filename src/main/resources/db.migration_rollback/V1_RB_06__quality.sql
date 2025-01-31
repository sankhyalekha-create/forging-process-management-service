-- Rollback Script

-- Drop Indexes for GaugeInspectionReport Table
DROP INDEX IF EXISTS idx_gauge_inspection_report_finished_pieces_count;
DROP INDEX IF EXISTS idx_gauge_inspection_report_rejected_pieces_count;
DROP INDEX IF EXISTS idx_gauge_inspection_report_rework_pieces_count;

-- Drop GaugeInspectionReport Table
DROP TABLE IF EXISTS gauge_inspection_report;

-- Drop Indexes for ProcessedItemInspectionBatch Table
DROP INDEX IF EXISTS idx_processed_item_inspection_batch_item_status;
DROP INDEX IF EXISTS idx_processed_item_inspection_batch_actual_inspection_batch_pieces_count;
DROP INDEX IF EXISTS idx_processed_item_inspection_batch_reject_inspection_batch_pieces_count;
DROP INDEX IF EXISTS idx_processed_item_inspection_batch_rework_pieces_count;

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
