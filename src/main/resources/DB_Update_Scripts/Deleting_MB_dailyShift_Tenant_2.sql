UPDATE daily_machining_batch dmb
SET deleted = true, deleted_at = CURRENT_TIMESTAMP
    FROM machining_batch mb
WHERE dmb.machining_batch_id = mb.id
  AND mb.deleted = true
  AND dmb.deleted = false;