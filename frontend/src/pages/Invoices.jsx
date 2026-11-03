import { useState } from 'react';
import { useBilling } from '../context/BillingContext.jsx';
import { Button, ButtonLink, Field, InvoiceTable, PageHeader } from '../components/ui.jsx';
import { statusLabels } from '../lib/billing.js';

export default function Invoices() {
  const { invoices } = useBilling();
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState('ALL');
  const filters = ['ALL', 'DRAFT', 'ISSUED', 'PARTIALLY_PAID', 'PAID', 'OVERDUE'];
  const filtered = invoices.filter((invoice) => (filter === 'ALL' || invoice.status === filter || (filter === 'ISSUED' && invoice.status === 'UNPAID')) && `${invoice.customerName} ${invoice.invoiceNumber || 'Draft'}`.toLowerCase().includes(search.toLowerCase()));
  const year = new Date().getMonth() >= 3 ? new Date().getFullYear() : new Date().getFullYear() - 1;
  return <><PageHeader title="Invoices" subtitle={`Financial year ${year}-${String(year + 1).slice(-2)}`}><ButtonLink to="/invoices/new" primary>New invoice</ButtonLink></PageHeader><div className="card filter-card"><Field label="Search" placeholder="Customer or invoice number" type="search" value={search} onChange={(event) => setSearch(event.target.value)} /><div className="filter-buttons" aria-label="Filter invoices by status">{filters.map((status) => <Button key={status} primary={filter === status} aria-pressed={filter === status} onClick={() => setFilter(status)}>{status === 'ALL' ? 'All' : statusLabels[status]}</Button>)}</div></div><div className="card"><InvoiceTable key={`${search}-${filter}`} invoices={filtered} /></div></>;
}
