-- Operational: track last send attempt so email retries can back off
-- instead of hammering SMTP every scheduler tick.
ALTER TABLE email_logs
    ADD COLUMN IF NOT EXISTS last_attempted_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_email_logs_retry
    ON email_logs (status, attempt_count, last_attempted_at);
