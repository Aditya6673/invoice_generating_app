CREATE TABLE product (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES business(id),
    name TEXT NOT NULL,
    hsn_sac_code VARCHAR(8),
    default_price NUMERIC(14,2) NOT NULL DEFAULT 0,
    default_tax_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    unit VARCHAR(10) NOT NULL DEFAULT 'PCS',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);
CREATE INDEX idx_product_business ON product(business_id, active);
