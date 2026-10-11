ALTER TABLE account_email_verifications
    ALTER COLUMN code_hash TYPE VARCHAR(64)
    USING TRIM(code_hash)::VARCHAR(64);
