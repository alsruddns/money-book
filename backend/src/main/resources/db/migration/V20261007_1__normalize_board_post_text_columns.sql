-- Earlier production schemas may have created these text fields as BYTEA.
-- Convert only mismatched columns; current VARCHAR installations are unchanged.
DO $$
DECLARE
    column_name TEXT;
BEGIN
    FOR column_name IN
        SELECT c.column_name
        FROM information_schema.columns c
        WHERE c.table_schema = current_schema()
          AND c.table_name = 'board_posts'
          AND c.column_name IN ('title', 'content')
          AND c.data_type = 'bytea'
    LOOP
        EXECUTE format(
            'ALTER TABLE board_posts ALTER COLUMN %I TYPE VARCHAR USING convert_from(%I, ''UTF8'')',
            column_name, column_name
        );
    END LOOP;
END $$;
