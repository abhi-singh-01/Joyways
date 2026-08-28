-- Phase 7: Notifications and Email Logging

CREATE TABLE notifications (
    id                   BIGSERIAL PRIMARY KEY,
    recipient_user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type                 VARCHAR(50) NOT NULL,
    title                VARCHAR(255) NOT NULL,
    message              TEXT,
    related_entity_type  VARCHAR(50),
    related_entity_id    BIGINT,
    is_read              BOOLEAN NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read_at              TIMESTAMPTZ
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_user_id);
CREATE INDEX idx_notifications_is_read ON notifications(recipient_user_id, is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

CREATE TABLE email_logs (
    id                   BIGSERIAL PRIMARY KEY,
    recipient            VARCHAR(255) NOT NULL,
    subject              VARCHAR(500) NOT NULL,
    template             VARCHAR(100) NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    related_entity_type  VARCHAR(50),
    related_entity_id    BIGINT,
    attempt_count        INTEGER NOT NULL DEFAULT 0,
    sent_at              TIMESTAMPTZ,
    failure_reason       TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_email_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX idx_email_logs_status ON email_logs(status);
CREATE INDEX idx_email_logs_related ON email_logs(related_entity_type, related_entity_id);
CREATE INDEX idx_email_logs_template_recipient ON email_logs(template, recipient, related_entity_type, related_entity_id);
