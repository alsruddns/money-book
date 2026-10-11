ALTER TABLE account_email_verifications
    ALTER COLUMN grant_hash TYPE VARCHAR(64)
    USING TRIM(grant_hash)::VARCHAR(64);
