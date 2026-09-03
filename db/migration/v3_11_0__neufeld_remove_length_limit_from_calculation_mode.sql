DO $$
DECLARE
    column_name_ text;
BEGIN
    FOREACH column_name_ IN ARRAY ARRAY['code', 'description'] LOOP
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'pensen'
              AND table_name = 'calculation_mode'
              AND column_name = column_name_
              AND character_maximum_length IS NOT NULL
        ) THEN
            EXECUTE format(
                'ALTER TABLE pensen.calculation_mode ALTER COLUMN %I TYPE text',
                column_name_
            );
            RAISE NOTICE 'pensen.calculation_mode.%: Laengenbeschraenkung entfernt', column_name_;
        END IF;
    END LOOP;
END
$$;
