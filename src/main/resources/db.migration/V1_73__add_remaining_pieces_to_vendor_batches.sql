-- Add remaining_pieces column to vendor_dispatch_batch table
-- This field stores the number of pieces not part of any full package when useUniformPackaging=false
ALTER TABLE vendor_dispatch_batch ADD COLUMN remaining_pieces INTEGER;

-- Add useUniformPackaging and remaining_pieces columns to vendor_receive_batch table
-- useUniformPackaging tracks if the batch uses uniform packaging
-- remaining_pieces stores the number of pieces not part of any full package when useUniformPackaging=false
ALTER TABLE vendor_receive_batch ADD COLUMN use_uniform_packaging BOOLEAN;
ALTER TABLE vendor_receive_batch ADD COLUMN remaining_pieces INTEGER;

-- Add comments for documentation
COMMENT ON COLUMN vendor_dispatch_batch.remaining_pieces IS 'Number of pieces not part of any full package (only when use_uniform_packaging=false, otherwise null)';
COMMENT ON COLUMN vendor_receive_batch.use_uniform_packaging IS 'Whether the batch uses uniform packaging for all packages';
COMMENT ON COLUMN vendor_receive_batch.remaining_pieces IS 'Number of pieces not part of any full package (only when use_uniform_packaging=false, otherwise null)';
