DELETE FROM processed_item_machining_batch pimb
WHERE pimb.machining_batch_id=7 and pimb.deleted=true;

DELETE FROM daily_machining_batch dmb
WHERE dmb.machining_batch_id=7;

DELETE FROM machining_batch mb
WHERE mb.id=7 and mb.deleted=true;

DELETE FROM processed_item_machining_batch pimb
WHERE pimb.machining_batch_id=9 and pimb.deleted=true;


DELETE FROM daily_machining_batch dmb
WHERE dmb.machining_batch_id=9;

DELETE FROM machining_batch mb
WHERE mb.id=9 and mb.deleted=true;