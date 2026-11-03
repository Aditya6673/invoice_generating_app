import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useBilling } from '../context/BillingContext.jsx';
import { Button, ButtonLink, Empty, ErrorMessage, Field, PageHeader, SelectField, Status } from '../components/ui.jsx';
import { formatDate, methods, money, outstanding, today } from '../lib/billing.js';
import { downloadPdf } from '../lib/export.js';

function PaymentForm({ invoice }) {
  const { recordPayment, notify } = useBilling();
  const remaining = outstanding(invoice);
  const [values, setValues] = useState({ amount: remaining, paymentDate: today(), method: 'UPI', reference: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  if (!remaining || ['DRAFT', 'VOID', 'VOIDED'].includes(invoice.status)) return null;
  const change = (key) => (event) => setValues({ ...values, [key]: event.target.value });
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError('');
    try { await recordPayment(invoice.id, { ...values, amount: Number(values.amount) }); notify('Payment recorded.'); setValues({ ...values, amount: Math.max(0, remaining - Number(values.amount)), reference: '' }); }
    catch (failure) { setError(failure.message); }
    finally { setBusy(false); }
  }
  return <form className="card" onSubmit={submit}><h2 className="form-heading">Record payment</h2><ErrorMessage message={error} /><div className="form-grid"><Field label="Amount (₹)" className="full" type="number" min="0.01" max={remaining} step="0.01" required value={values.amount} onChange={change('amount')} /><Field label="Date" type="date" max={today()} required value={values.paymentDate} onChange={change('paymentDate')} /><SelectField label="Method" value={values.method} onChange={change('method')} options={Object.entries(methods).map(([value, label]) => ({ value, label }))} /><Field label="Reference" className="full" value={values.reference} onChange={change('reference')} /></div><Button className="full-button" primary disabled={busy}>{busy ? 'Recording…' : 'Record payment'}</Button></form>;
}
export default function InvoiceDetail() {
  const { id } = useParams();
  const { invoices, business, payments, role, sendReminder, notify } = useBilling();
  const [busy, setBusy] = useState('');
  const invoice = invoices.find((row) => row.id === id);
  if (!invoice) return <div className="card"><Empty title="Invoice not found" description="Return to your invoices to select a current record."><ButtonLink to="/invoices">Back to invoices</ButtonLink></Empty></div>;
  const paymentHistory = payments.filter((payment) => payment.invoiceId === id);
  const tax = (key) => invoice.lines.reduce((sum, line) => sum + Number(line[key] || 0), 0);
  async function act(action, fn) { setBusy(action); try { await fn(); } catch (failure) { notify(failure.message, 'error'); } finally { setBusy(''); } }
  return <><Link className="back-link" to="/invoices">← Invoices</Link><PageHeader title={invoice.invoiceNumber ? `Invoice ${invoice.invoiceNumber}` : 'Draft invoice'} subtitle={invoice.customerName}>{invoice.status === 'DRAFT' ? <ButtonLink primary to={`/invoices/${id}/edit`}>Edit draft</ButtonLink> : <><Button disabled={!!busy} onClick={() => act('pdf', () => downloadPdf(invoice))}>{busy === 'pdf' ? 'Preparing…' : 'Download PDF'}</Button><Button onClick={() => window.print()}>Print</Button>{role !== 'STAFF' && outstanding(invoice) > 0 && <Button primary disabled={!!busy} onClick={() => act('reminder', () => sendReminder(id))}>{busy === 'reminder' ? 'Sending…' : 'Send reminder'}</Button>}</>}</PageHeader><div className="invoice-layout"><article className="card invoice-paper"><div className="invoice-paper-header"><div><div className="invoice-word">INVOICE</div><div className="muted">{invoice.invoiceNumber || 'Draft'}</div></div><Status status={invoice.status} /></div><div className="billing-parties"><div><div className="muted">From</div><div className="semibold">{business.name || 'Your business'}</div><div className="muted">GSTIN {business.gstin || 'Not registered'}</div></div><div><div className="muted">Bill to</div><Link className="table-link" to={`/customers/${invoice.customerId}`}>{invoice.customerName}</Link><div className="muted">Issued {formatDate(invoice.issueDate)} · Due {formatDate(invoice.dueDate)}</div></div></div><div className="table-scroll"><table className="compact-table"><thead><tr><th>Description</th><th className="r">Qty</th><th className="r">Price</th><th className="r">Amount</th></tr></thead><tbody>{invoice.lines.map((line, index) => <tr key={line.id || index}><td>{line.description}</td><td className="r">{line.quantity}</td><td className="r">{money(line.unitPrice)}</td><td className="r">{money(line.taxableAmount)}</td></tr>)}</tbody></table></div><div className="paper-totals"><div className="sr"><span className="muted">Subtotal</span><span>{money(invoice.subtotal)}</span></div>{Number(invoice.discountTotal) > 0 && <div className="sr"><span className="muted">Discount</span><span>−{money(invoice.discountTotal)}</span></div>}{tax('igstAmount') > 0 ? <div className="sr"><span className="muted">IGST</span><span>{money(tax('igstAmount'))}</span></div> : <><div className="sr"><span className="muted">CGST</span><span>{money(tax('cgstAmount'))}</span></div><div className="sr"><span className="muted">SGST</span><span>{money(tax('sgstAmount'))}</span></div></>}<div className="sr grand-total"><span>Total</span><span>{money(invoice.grandTotal)}</span></div></div>{invoice.notes && <div className="invoice-notes"><div className="muted">Notes and payment terms</div><p>{invoice.notes}</p></div>}</article><div className="stack payment-column"><section className="card"><h2>Payments</h2><div className="sr"><span className="muted">Paid</span><span>{money(invoice.amountPaid)}</span></div><div className="sr"><span className="muted">Outstanding</span><strong className="danger-text">{money(outstanding(invoice))}</strong></div>{paymentHistory.length ? paymentHistory.map((payment) => <div className="row" key={payment.id}><span>{formatDate(payment.paymentDate)} · {methods[payment.method] || payment.method}{payment.isReversal ? ' (reversal)' : ''}</span><span>{money(payment.amount)}</span></div>) : <p className="muted">No payments recorded yet.</p>}</section><PaymentForm key={invoice.id} invoice={invoice} /></div></div></>;
}
