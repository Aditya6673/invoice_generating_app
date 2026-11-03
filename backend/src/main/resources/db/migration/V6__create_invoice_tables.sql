CREATE TABLE invoice (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES business(id),
    customer_id UUID NOT NULL REFERENCES customer(id),
    invoice_number VARCHAR(50),
    issue_date DATE,
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    
    -- Totals
    subtotal NUMERIC(14,2) NOT NULL DEFAULT 0,
    discount_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    tax_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    grand_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    amount_paid NUMERIC(14,2) NOT NULL DEFAULT 0,
    
    -- Customer snapshot (frozen at issue time)
    customer_name TEXT,
    customer_address TEXT,
    customer_state TEXT,
    customer_state_code VARCHAR(2),
    customer_gstin VARCHAR(15),
    
    notes TEXT,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID
);

CREATE UNIQUE INDEX idx_invoice_number ON invoice(business_id, invoice_number) WHERE invoice_number IS NOT NULL;
CREATE INDEX idx_invoice_business_date ON invoice(business_id, issue_date);
CREATE INDEX idx_invoice_business_status ON invoice(business_id, status);
CREATE INDEX idx_invoice_business_customer ON invoice(business_id, customer_id);

CREATE TABLE invoice_line (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    hsn_sac_code VARCHAR(8),
    quantity NUMERIC(10,2) NOT NULL DEFAULT 1,
    unit_price NUMERIC(14,2) NOT NULL,
    discount_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    tax_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    taxable_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    cgst_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    sgst_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    igst_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    line_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_invoice_line_invoice ON invoice_line(invoice_id);

CREATE TABLE invoice_sequence (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES business(id),
    financial_year VARCHAR(7) NOT NULL,
    last_number INTEGER NOT NULL DEFAULT 0,
    UNIQUE(business_id, financial_year)
);
