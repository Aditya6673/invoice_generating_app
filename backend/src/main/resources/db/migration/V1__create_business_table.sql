CREATE TABLE business (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    address TEXT,
    gstin VARCHAR(15),
    state_code VARCHAR(2),
    logo_url TEXT,
    invoice_prefix VARCHAR(10) NOT NULL DEFAULT 'INV',
    bank_name TEXT,
    bank_account TEXT,
    bank_ifsc VARCHAR(11),
    default_payment_terms INTEGER DEFAULT 30,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);
