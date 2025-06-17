-- ================================================================
-- ROLLBACK PHASE 1: ProcessedItemHeatTreatmentBatch
-- ================================================================
DO $$
BEGIN
    -- Add processed_item_id column if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'processed_item_heat_treatment_batch'
          AND column_name = 'processed_item_id'
    ) THEN
        ALTER TABLE processed_item_heat_treatment_batch
        ADD COLUMN processed_item_id BIGINT;
    END IF;

    -- Populate processed_item_id from item_id
    UPDATE processed_item_heat_treatment_batch pihtb
    SET processed_item_id = (
        SELECT pi.id
        FROM processed_item pi
        WHERE pi.item_id = pihtb.item_id
        LIMIT 1
    )
    WHERE pihtb.item_id IS NOT NULL;

    -- Re-add original foreign key
    BEGIN
        ALTER TABLE processed_item_heat_treatment_batch
        ADD CONSTRAINT fk_processed_item_heat_treatment_batch_processed_item
        FOREIGN KEY (processed_item_id) REFERENCES processed_item(id);
    EXCEPTION
        WHEN duplicate_object THEN
            RAISE NOTICE 'Constraint already exists.';
    END;

    -- Drop new FK, index, and column
    BEGIN
        ALTER TABLE processed_item_heat_treatment_batch DROP CONSTRAINT IF EXISTS fk_processed_item_heat_treatment_batch_item;
    END;
    BEGIN
        DROP INDEX IF EXISTS idx_processed_item_heat_treatment_batch_item_id;
    END;
    BEGIN
        ALTER TABLE processed_item_heat_treatment_batch DROP COLUMN IF EXISTS item_id;
    END;
END;
$$;


-- ================================================================
-- ROLLBACK PHASE 2: ProcessedItemMachiningBatch
-- ================================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'processed_item_machining_batch'
          AND column_name = 'processed_item_id'
    ) THEN
        ALTER TABLE processed_item_machining_batch
        ADD COLUMN processed_item_id BIGINT;
    END IF;

    UPDATE processed_item_machining_batch pimb
    SET processed_item_id = (
        SELECT pi.id
        FROM processed_item pi
        WHERE pi.item_id = pimb.item_id
        LIMIT 1
    )
    WHERE pimb.item_id IS NOT NULL;

    BEGIN
        ALTER TABLE processed_item_machining_batch
        ADD CONSTRAINT fk_processed_item_machining_batch_processed_item
        FOREIGN KEY (processed_item_id) REFERENCES processed_item(id);
    EXCEPTION
        WHEN duplicate_object THEN
            RAISE NOTICE 'Constraint already exists.';
    END;

    BEGIN
        ALTER TABLE processed_item_machining_batch DROP CONSTRAINT IF EXISTS fk_processed_item_machining_batch_item;
    END;
    BEGIN
        DROP INDEX IF EXISTS idx_processed_item_machining_batch_item_id;
    END;
    BEGIN
        ALTER TABLE processed_item_machining_batch DROP COLUMN IF EXISTS item_id;
        ALTER TABLE processed_item_machining_batch DROP COLUMN IF EXISTS workflow_identifier;
        ALTER TABLE processed_item_machining_batch DROP COLUMN IF EXISTS item_workflow_id;
    END;
END;
$$;


-- ================================================================
-- ROLLBACK PHASE 3: ProcessedItemInspectionBatch
-- ================================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'processed_item_inspection_batch'
          AND column_name = 'processed_item_id'
    ) THEN
        ALTER TABLE processed_item_inspection_batch
        ADD COLUMN processed_item_id BIGINT;
    END IF;

    UPDATE processed_item_inspection_batch piib
    SET processed_item_id = (
        SELECT pi.id
        FROM processed_item pi
        WHERE pi.item_id = piib.item_id
        LIMIT 1
    )
    WHERE piib.item_id IS NOT NULL;

    BEGIN
        ALTER TABLE processed_item_inspection_batch
        ADD CONSTRAINT fk_processed_item_inspection_batch_processed_item
        FOREIGN KEY (processed_item_id) REFERENCES processed_item(id);
    EXCEPTION
        WHEN duplicate_object THEN
            RAISE NOTICE 'Constraint already exists.';
    END;

    BEGIN
        ALTER TABLE processed_item_inspection_batch DROP CONSTRAINT IF EXISTS fk_processed_item_inspection_batch_item;
    END;
    BEGIN
        DROP INDEX IF EXISTS idx_processed_item_inspection_batch_item_id;
    END;
    BEGIN
        ALTER TABLE processed_item_inspection_batch DROP COLUMN IF EXISTS item_id;
        ALTER TABLE processed_item_inspection_batch DROP COLUMN IF EXISTS workflow_identifier;
        ALTER TABLE processed_item_inspection_batch DROP COLUMN IF EXISTS item_workflow_id;
    END;
END;
$$;


-- ================================================================
-- ROLLBACK PHASE 4: ProcessedItemDispatchBatch
-- ================================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'processed_item_dispatch_batch'
          AND column_name = 'processed_item_id'
    ) THEN
        ALTER TABLE processed_item_dispatch_batch
        ADD COLUMN processed_item_id BIGINT;
    END IF;

    UPDATE processed_item_dispatch_batch pidb
    SET processed_item_id = (
        SELECT pi.id
        FROM processed_item pi
        WHERE pi.item_id = pidb.item_id
        LIMIT 1
    )
    WHERE pidb.item_id IS NOT NULL;

    BEGIN
        ALTER TABLE processed_item_dispatch_batch
        ADD CONSTRAINT fk_processed_item_dispatch_batch_processed_item
        FOREIGN KEY (processed_item_id) REFERENCES processed_item(id);
    EXCEPTION
        WHEN duplicate_object THEN
            RAISE NOTICE 'Constraint already exists.';
    END;

    BEGIN
        ALTER TABLE processed_item_dispatch_batch DROP CONSTRAINT IF EXISTS fk_processed_item_dispatch_batch_item;
    END;
    BEGIN
        DROP INDEX IF EXISTS idx_processed_item_dispatch_batch_item_id;
    END;
    BEGIN
        ALTER TABLE processed_item_dispatch_batch DROP COLUMN IF EXISTS item_id;
        ALTER TABLE processed_item_dispatch_batch DROP COLUMN IF EXISTS workflow_identifier;
        ALTER TABLE processed_item_dispatch_batch DROP COLUMN IF EXISTS item_workflow_id;
    END;
END;
$$;
