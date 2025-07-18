-- Rollback script for V1_46: Remove quality check completion fields from vendor_receive_batch table
-- Version: V1_RB_46
-- Description: Rollback quality check completion fields for VendorReceiveBatch entity

-- Remove all check constraints first (in reverse order of creation)
ALTER TABLE vendor_receive_batch
DROP CONSTRAINT IF EXISTS chk_locked_batch_quality_completed;

ALTER TABLE vendor_receive_batch
DROP CONSTRAINT IF EXISTS chk_quality_completion_consistency;

ALTER TABLE vendor_receive_batch
DROP CONSTRAINT IF EXISTS chk_final_tenant_rejects_non_negative;

ALTER TABLE vendor_receive_batch
DROP CONSTRAINT IF EXISTS chk_final_vendor_rejects_non_negative;

-- Remove indexes (in reverse order of creation)
DROP INDEX IF EXISTS idx_vendor_receive_batch_status_quality;

DROP INDEX IF EXISTS idx_vendor_receive_batch_quality_pending;

DROP INDEX IF EXISTS idx_vendor_receive_batch_is_locked;

DROP INDEX IF EXISTS idx_vendor_receive_batch_quality_completed_at;

-- Remove the new columns (in reverse order of addition)
ALTER TABLE vendor_receive_batch
DROP COLUMN IF EXISTS is_locked;

ALTER TABLE vendor_receive_batch
DROP COLUMN IF EXISTS quality_check_remarks;

ALTER TABLE vendor_receive_batch
DROP COLUMN IF EXISTS final_tenant_rejects_count;

ALTER TABLE vendor_receive_batch
DROP COLUMN IF EXISTS final_vendor_rejects_count;

ALTER TABLE vendor_receive_batch
DROP COLUMN IF EXISTS quality_check_completed_at;

COMMIT; 