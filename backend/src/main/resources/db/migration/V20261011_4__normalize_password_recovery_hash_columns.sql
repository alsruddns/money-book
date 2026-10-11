ALTER TABLE password_recovery_codes
    ALTER COLUMN code_hash TYPE VARCHAR(64)
    USING TRIM(code_hash)::VARCHAR(64);
