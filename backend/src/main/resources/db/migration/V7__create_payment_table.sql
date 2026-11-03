CREATE TABLE payment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoice(id),
    amount NUMERIC(14,2) NOT NULL,
    payment_date DATE NOT NULL,
    method VARCHAR(20) NOT NULL DEFAULT 'BANK_TRANSFER',
    reference TEXT,
    recorded_by UUID,
    is_reversal BOOLEAN NOT NULL DEFAULT false,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_payment_invoice ON payment(invoice_id);
