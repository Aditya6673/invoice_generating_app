CREATE TABLE business_reminder_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL UNIQUE REFERENCES business(id),
    enabled BOOLEAN NOT NULL DEFAULT true,
    before_due_days INTEGER NOT NULL DEFAULT 3,
    overdue_intervals TEXT NOT NULL DEFAULT '1,7,15',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE reminder_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoice(id),
    milestone VARCHAR(30) NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    result VARCHAR(20) NOT NULL DEFAULT 'SENT',
    error_message TEXT
);

CREATE INDEX idx_reminder_log_invoice ON reminder_log(invoice_id, milestone);
