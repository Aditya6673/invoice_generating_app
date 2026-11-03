import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useBilling } from '../context/BillingContext.jsx';
import { Button, Empty, Field, PageHeader, StatCards } from '../components/ui.jsx';
import { formatDate, methods, money, summary } from '../lib/billing.js';
import { exportCsv, exportRows } from '../lib/export.js';

export default function Payments() {
  const { payments, invoices } = useBilling();
  const [search, setSearch] = useState('');
  const stats = summary(invoices);
  const filtered = payments.filter((payment) => { const invoice = invoices.find((row) => row.id === payment.invoiceId); return `${invoice?.invoiceNumber} ${invoice?.customerName} ${payment.reference} ${methods[payment.method]}`.toLowerCase().includes(search.toLowerCase()); }).sort((a, b) => b.paymentDate.localeCompare(a.paymentDate));
  return <><PageHeader title="Payments" subtitle="Track payments and outstanding balances"><Button onClick={() => exportCsv(exportRows(invoices, filtered), 'payments.csv')}>Export CSV</Button></PageHeader><StatCards items={[{ label: 'Total collected', value: money(stats.totalCollected) }, { label: 'Payments recorded', value: payments.filter((payment) => !payment.isReversal).length }, { label: 'Outstanding', value: money(stats.pendingAmount) }, { label: 'Overdue', value: money(stats.overdueAmount), danger: true }]} /><div className="card spaced"><Field label="Search" type="search" placeholder="Customer, invoice or payment reference" value={search} onChange={(event) => setSearch(event.target.value)} /></div><div className="card">{filtered.length ? <div className="table-scroll"><table><thead><tr><th>Invoice</th><th>Customer</th><th>Date</th><th>Method</th><th>Reference</th><th className="r">Amount</th></tr></thead><tbody>{filtered.map((payment) => { const invoice = invoices.find((row) => row.id === payment.invoiceId); return <tr key={payment.id}><td><Link className="table-link" to={`/invoices/${payment.invoiceId}`}>{invoice?.invoiceNumber}</Link></td><td>{invoice?.customerName}</td><td>{formatDate(payment.paymentDate)}</td><td>{methods[payment.method] || payment.method}{payment.isReversal ? ' (reversal)' : ''}</td><td>{payment.reference || '—'}</td><td className="r">{money(payment.amount)}</td></tr>; })}</tbody></table></div> : <Empty title="No payments found" description="Record a payment from an issued invoice to see it here." />}</div></>;
}
