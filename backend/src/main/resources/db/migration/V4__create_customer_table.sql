CREATE TABLE customer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES business(id),
    name TEXT NOT NULL,
    phone VARCHAR(15),
    email TEXT,
    billing_address TEXT,
    city TEXT,
    state TEXT,
    state_code VARCHAR(2),
    pincode VARCHAR(6),
    gstin VARCHAR(15),
    deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);
CREATE INDEX idx_customer_business ON customer(business_id, deleted);
CREATE INDEX idx_customer_name ON customer(business_id, name);
