import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useBilling } from '../context/BillingContext.jsx';
import { ButtonLink, Empty, MonthlyChart, PageHeader, StatCards, Status } from '../components/ui.jsx';
import { compactMoney, formatDate, inDateRange, money, outstanding, summary, today } from '../lib/billing.js';

export default function Dashboard() {
  const { invoices } = useBilling();
  const [period, setPeriod] = useState('year');
  const year = new Date().getMonth() >= 3 ? new Date().getFullYear() : new Date().getFullYear() - 1;
  const from = period === 'month' ? `${today().slice(0, 7)}-01` : `${year}-04-01`;
  const filtered = invoices.filter((invoice) => inDateRange(invoice, from, today()));
  const stats = summary(filtered);
  const months = [];
  for (let date = new Date(`${from}T12:00:00`); date.toISOString().slice(0, 10) <= today(); date.setMonth(date.getMonth() + 1)) {
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
    months.push({ month: date.toLocaleDateString('en', { month: 'short' }), total: filtered.filter((invoice) => invoice.issueDate.startsWith(key) && !['DRAFT', 'VOID'].includes(invoice.status)).reduce((sum, invoice) => sum + Number(invoice.grandTotal), 0) });
  }
  const attention = invoices.filter((invoice) => outstanding(invoice) > 0).sort((a, b) => (a.status === 'OVERDUE' ? 0 : 1) - (b.status === 'OVERDUE' ? 0 : 1) || (a.dueDate || '').localeCompare(b.dueDate || '')).slice(0, 4);
  return <><PageHeader title="Dashboard" subtitle={`${formatDate(from)} to ${formatDate(today())}`}><select className="btn period-select" aria-label="Dashboard period" value={period} onChange={(event) => setPeriod(event.target.value)}><option value="year">This financial year</option><option value="month">This month</option></select><ButtonLink to="/invoices/new" primary>New invoice</ButtonLink></PageHeader><StatCards items={[{ label: 'Total sales', value: compactMoney(stats.totalSales) }, { label: 'Invoices issued', value: stats.invoiceCount }, { label: 'Pending', value: compactMoney(stats.pendingAmount) }, { label: 'Overdue', value: compactMoney(stats.overdueAmount), danger: true }]} /><div className="g2"><section className="card"><h2>Monthly sales (₹ lakh)</h2><MonthlyChart sales={months} /></section><section className="card"><h2>Needs attention</h2><div className="attention-list">{attention.length ? attention.map((invoice) => {
    const days = invoice.dueDate ? Math.round((new Date(`${today()}T12:00:00`) - new Date(`${invoice.dueDate}T12:00:00`)) / 86400000) : null;
    return <Link key={invoice.id} className="row attention-row" to={`/invoices/${invoice.id}`}><div><div className="semibold">{invoice.customerName}</div><div className="muted attention-meta">{invoice.invoiceNumber} · {days === null ? 'No due date' : days > 0 ? `${days} days late` : days === 0 ? 'due today' : `due in ${Math.abs(days)} days`}</div></div><div className="r"><div className="semibold attention-amount">{money(outstanding(invoice))}</div><Status status={invoice.status} /></div></Link>;
  }) : <Empty title="You're all caught up" description="There are no outstanding invoices." />}</div></section></div></>;
}
