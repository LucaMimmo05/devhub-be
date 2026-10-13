-- Deferred account deletion (roadmap P1.7): DELETE /users/me only schedules the deletion,
-- an hourly job purges accounts whose grace period has passed, and every request is recorded.
ALTER TABLE users ADD COLUMN IF NOT EXISTS deletion_requested_at timestamp(6) with time zone;

-- No FK to users: rows must outlive the account they describe. Ids and dates only, no personal data.
CREATE TABLE IF NOT EXISTS account_deletions (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    requested_at timestamp(6) with time zone NOT NULL,
    cancelled_at timestamp(6) with time zone,
    purged_at timestamp(6) with time zone,
    source varchar(20) NOT NULL
);

CREATE INDEX IF NOT EXISTS account_deletions_user_id_idx ON account_deletions (user_id);
