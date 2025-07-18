-- Migration script to add quality check completion fields to vendor_receive_batch table
-- Version: V1_46
-- Description: Add quality check completion fields for VendorReceiveBatch entity

-- Add quality check completion timestamp
ALTER TABLE vendor_receive_batch
    ADD COLUMN quality_check_completed_at TIMESTAMP NULL;

-- Add final vendor rejects count after quality check
ALTER TABLE vendor_receive_batch
    ADD COLUMN final_vendor_rejects_count INT NULL;

-- Add final tenant rejects count after quality check  
ALTER TABLE vendor_receive_batch
    ADD COLUMN final_tenant_rejects_count INT NULL;

-- Add quality check completion remarks
ALTER TABLE vendor_receive_batch
    ADD COLUMN quality_check_remarks TEXT NULL;

-- Add is_locked flag to prevent modifications after quality check completion
ALTER TABLE vendor_receive_batch
    ADD COLUMN is_locked BOOLEAN NOT NULL DEFAULT FALSE;

-- Add comments for the new columns
COMMENT ON COLUMN vendor_receive_batch.quality_check_completed_at IS 'Timestamp when quality check was completed';
COMMENT ON COLUMN vendor_receive_batch.final_vendor_rejects_count IS 'Final count of pieces rejected by vendor after quality check completion';
COMMENT ON COLUMN vendor_receive_batch.final_tenant_rejects_count IS 'Final count of pieces rejected by tenant after quality check completion';
COMMENT ON COLUMN vendor_receive_batch.quality_check_remarks IS 'Remarks and notes from quality check completion process';
COMMENT ON COLUMN vendor_receive_batch.is_locked IS 'Flag to lock batch from further modifications after quality check completion';

-- Create index on quality_check_completed_at for performance
CREATE INDEX idx_vendor_receive_batch_quality_completed_at
    ON vendor_receive_batch(quality_check_completed_at);

-- Create index on is_locked for filtering locked/unlocked batches
CREATE INDEX idx_vendor_receive_batch_is_locked
    ON vendor_receive_batch(is_locked);

-- Create composite index for finding quality check pending batches
CREATE INDEX idx_vendor_receive_batch_quality_pending
    ON vendor_receive_batch(tenant_id, quality_check_required, quality_check_completed, is_locked);

-- Create composite index for status-based queries with new quality check statuses
CREATE INDEX idx_vendor_receive_batch_status_quality
    ON vendor_receive_batch(tenant_id, vendor_receive_batch_status, quality_check_completed) WHERE deleted = false;

-- Add check constraints to ensure data integrity
ALTER TABLE vendor_receive_batch
    ADD CONSTRAINT chk_final_vendor_rejects_non_negative
        CHECK (final_vendor_rejects_count IS NULL OR final_vendor_rejects_count >= 0);

ALTER TABLE vendor_receive_batch
    ADD CONSTRAINT chk_final_tenant_rejects_non_negative
        CHECK (final_tenant_rejects_count IS NULL OR final_tenant_rejects_count >= 0);

-- Add constraint to ensure final rejects are only set when quality check is completed
ALTER TABLE vendor_receive_batch
    ADD CONSTRAINT chk_quality_completion_consistency
        CHECK (
            (quality_check_completed = FALSE AND final_vendor_rejects_count IS NULL AND final_tenant_rejects_count IS NULL AND quality_check_completed_at IS NULL)
                OR
            (quality_check_completed = TRUE AND final_vendor_rejects_count IS NOT NULL AND final_tenant_rejects_count IS NOT NULL)
            );

-- Add constraint to ensure locked batches have completed quality check
ALTER TABLE vendor_receive_batch
    ADD CONSTRAINT chk_locked_batch_quality_completed
        CHECK (is_locked = FALSE OR (is_locked = TRUE AND quality_check_completed = TRUE));

-- Update existing records to set default values for new fields
UPDATE vendor_receive_batch
SET is_locked = FALSE
WHERE is_locked IS NULL;

COMMIT;