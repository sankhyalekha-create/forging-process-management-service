-- Rollback script for V1_73__add_remaining_pieces_to_vendor_batches.sql
-- Remove remaining_pieces column from vendor_dispatch_batch table
ALTER TABLE vendor_dispatch_batch DROP COLUMN IF EXISTS remaining_pieces;

-- Remove useUniformPackaging and remaining_pieces columns from vendor_receive_batch table
ALTER TABLE vendor_receive_batch DROP COLUMN IF EXISTS use_uniform_packaging;
ALTER TABLE vendor_receive_batch DROP COLUMN IF EXISTS remaining_pieces;
