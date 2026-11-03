import { useEffect, useState } from 'react';
import { useBilling } from '../context/BillingContext.jsx';
import { Button, Empty, Field, InvoiceTable, PageHeader, SelectField, StatCards } from '../components/ui.jsx';
import { compactMoney, inDateRange, money, summary, today } from '../lib/billing.js';
import { request } from '../lib/api.js';
import { downloadBlob } from '../lib/export.js';

export default function Reports() {
  const { invoices, notify } = useBilling();
  const year = new Date().getMonth() >= 3 ? new Date().getFullYear() : new Date().getFullYear() - 1;
  const [from, setFrom] = useState(`${year}-04-01`);
  const [to, setTo] = useState(today());
  const [type, setType] = useState('sales');
  const [remote, setRemote] = useState(null);
  const [reportError, setReportError] = useState('');
  const [retry, setRetry] = useState(0);
  const [busy, setBusy] = useState(false);
  const invalid = !!from && !!to && from > to;
  const filtered = invoices.filter((invoice) => inDateRange(invoice, from, to) && !['DRAFT', 'VOID'].includes(invoice.status));
  useEffect(() => {
    let active = true;
    setRemote(null); setReportError('');
    if (!invalid) {
      const query = new URLSearchParams({ ...(from ? { from } : {}), ...(to ? { to } : {}) });
      Promise.all([request(`/reports/summary?${query}`), request(`/reports/top-customers?${query}`)])
        .then(([stats, customers]) => { if (active) setRemote({ stats, customers }); })
        .catch((error) => { if (active) setReportError(error.message); });
    }
    return () => { active = false; };
  }, [from, to, invalid, retry]);
  const taxRows = Object.values(filtered.reduce((result, invoice) => {
    const key = invoice.issueDate.slice(0, 7);
    result[key] ||= { key, month: new Date(`${key}-01T12:00:00`).toLocaleDateString('en', { month: 'short', year: 'numeric' }), taxable: 0, cgst: 0, sgst: 0, igst: 0 };
    result[key].taxable += Number(invoice.subtotal) - Number(invoice.discountTotal || 0);
    for (const line of invoice.lines) { result[key].cgst += Number(line.cgstAmount || 0); result[key].sgst += Number(line.sgstAmount || 0); result[key].igst += Number(line.igstAmount || 0); }
    return result;
  }, {})).sort((a, b) => a.key.localeCompare(b.key));
  async function exportReport(excel) {
    setBusy(true);
    try {
      const query = new URLSearchParams({ type: excel ? 'xlsx' : 'csv', ...(from ? { from } : {}), ...(to ? { to } : {}) });
      downloadBlob(await request(`/reports/export?${query}`, { blob: true }), `invoices.${excel ? 'xlsx' : 'csv'}`);
    } catch (error) { notify(error.message, 'error'); }
    finally { setBusy(false); }
  }
  return <><PageHeader title="Reports" subtitle="Sales, tax and receivables"><Button disabled={busy || invalid} onClick={() => exportReport(false)}>Export CSV</Button><Button primary disabled={busy || invalid} onClick={() => exportReport(true)}>{busy ? 'Exporting…' : 'Export Excel'}</Button></PageHeader>
    <div className="card spaced"><div className="form-grid report-filters"><Field label="From" type="date" value={from} onChange={(event) => setFrom(event.target.value)} /><Field label="To" type="date" min={from} value={to} onChange={(event) => setTo(event.target.value)} /><SelectField label="Report" value={type} onChange={(event) => setType(event.target.value)} options={[{ value: 'sales', label: 'Sales summary' }, { value: 'tax', label: 'Tax summary' }, { value: 'receivables', label: 'Receivables' }]} /></div>{invalid && <p role="alert" className="danger-text">The end date must be on or after the start date.</p>}</div>
    {reportError ? <div className="load-error" role="alert"><span>{reportError}</span><Button onClick={() => setRetry(retry + 1)}>Retry</Button></div> : !remote ? !invalid && <div className="loading" role="status">Loading reports…</div> : <>
      <StatCards items={[{ label: 'Revenue', value: compactMoney(remote.stats.totalSales) }, { label: 'Tax collected', value: compactMoney(summary(filtered).taxTotal) }, { label: 'Outstanding', value: compactMoney(remote.stats.pendingAmount) }, { label: 'Overdue', value: compactMoney(remote.stats.overdueAmount), danger: true }]} />
      {type === 'receivables' ? <div className="card"><InvoiceTable invoices={filtered.filter((invoice) => Number(invoice.amountPaid || 0) < Number(invoice.grandTotal))} /></div> : <div className={type === 'tax' ? 'stack' : 'g2'}><section className="card"><h2 className="table-heading">Tax collected by month</h2>{taxRows.length ? <div className="table-scroll"><table className="compact-table"><thead><tr><th>Month</th><th className="r">Taxable</th><th className="r">CGST</th><th className="r">SGST</th><th className="r">IGST</th></tr></thead><tbody>{taxRows.map((row) => <tr key={row.month}><td>{row.month}</td><td className="r">{compactMoney(row.taxable)}</td><td className="r">{money(row.cgst)}</td><td className="r">{money(row.sgst)}</td><td className="r">{money(row.igst)}</td></tr>)}</tbody></table></div> : <Empty title="No tax data for this period" description="Choose a date range containing issued invoices." />}</section>
      {type !== 'tax' && <section className="card"><h2>Top customers</h2>{remote.customers.length ? remote.customers.map((customer) => <div className="customer-bar" key={customer.customerName}><div className="sr"><span>{customer.customerName}</span><strong>{compactMoney(customer.totalRevenue)}</strong></div><div className="progress-track"><div style={{ width: `${Number(customer.totalRevenue) / Math.max(...remote.customers.map((row) => Number(row.totalRevenue)), 1) * 100}%` }} /></div></div>) : <Empty title="No sales for this period" description="Your top customers will appear here once you issue invoices." />}</section>}</div>}
    </>}
  </>;
}
