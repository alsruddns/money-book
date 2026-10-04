-- Preflight before deployment: any returned row must be resolved explicitly before this migration.
-- SELECT lower(trim(verified_email)), count(*) FROM user_auth
-- WHERE verified_email IS NOT NULL
-- GROUP BY lower(trim(verified_email)) HAVING count(*) > 1;

DROP INDEX uq_user_auth_verified_email;

UPDATE user_auth
SET verified_email = lower(trim(verified_email))
WHERE verified_email IS NOT NULL;

CREATE UNIQUE INDEX uq_user_auth_verified_email ON user_auth (verified_email);
