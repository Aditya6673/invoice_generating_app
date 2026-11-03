import { useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useBilling } from '../context/BillingContext.jsx';
import { Button, ButtonLink, Empty, ErrorMessage, Field, PageHeader, SelectField, Status } from '../components/ui.jsx';
import { calculateInvoice, dateAfter, money } from '../lib/billing.js';

const newLine = () => ({ key: crypto.randomUUID(), description: '', hsnSacCode: '', quantity: 1, unitPrice: 0, discountAmount: 0, taxRate: 18 });
export default function InvoiceEditor() {
  const { id } = useParams();
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { invoices, customers, business, saveInvoice, notify } = useBilling();
  const existing = invoices.find((row) => row.id === id);
  const [draftId, setDraftId] = useState(id);
  const [values, setValues] = useState({ customerId: existing?.customerId || params.get('customer') || '', dueDate: existing?.dueDate || dateAfter(business.defaultPaymentTerms ?? 30), currency: 'INR', notes: existing?.notes || '', lines: existing?.lines?.map((line) => ({ ...line, key: crypto.randomUUID() })) || [newLine()] });
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');
  if (id && !existing) return <div className="card"><Empty title="Invoice not found"><ButtonLink to="/invoices">Back to invoices</ButtonLink></Empty></div>;
  if (existing && existing.status !== 'DRAFT') return <div className="card"><Empty title="This invoice has already been issued" description="Issued invoices are available from the invoice detail page."><ButtonLink to={`/invoices/${id}`}>View invoice</ButtonLink></Empty></div>;
  const customer = customers.find((row) => row.id === values.customerId);
  const interstate = !!customer && (!customer.stateCode || !business.stateCode || customer.stateCode.trim() !== business.stateCode.trim());
  const totals = calculateInvoice(values.lines, interstate);
  const change = (key) => (event) => setValues({ ...values, [key]: event.target.value });
  function updateLine(index, field, value) { setValues({ ...values, lines: values.lines.map((line, i) => i === index ? { ...line, [field]: value } : line) }); }
  async function submit(event) {
    event.preventDefault();
    const issue = event.nativeEvent.submitter?.value === 'issue';
    setBusy(issue ? 'issue' : 'draft'); setError('');
    try {
      const payload = { ...values, lines: values.lines.map(({ key, ...line }) => ({ description: line.description.trim(), hsnSacCode: line.hsnSacCode, quantity: Number(line.quantity), unitPrice: Number(line.unitPrice), discountAmount: Number(line.discountAmount), taxRate: Number(line.taxRate) })) };
      if (payload.lines.some((line) => line.discountAmount > line.quantity * line.unitPrice)) throw new Error('A line discount cannot be higher than its amount.');
      const invoice = await saveInvoice(payload, draftId, issue);
      notify(issue ? 'Invoice issued.' : 'Draft invoice saved.');
      navigate(`/invoices/${invoice.id}`);
    } catch (failure) { if (failure.draftId) setDraftId(failure.draftId); setError(failure.message); }
    finally { setBusy(''); }
  }
  return <form onSubmit={submit}><Link className="back-link" to="/invoices">← Invoices</Link><PageHeader title={id ? 'Edit draft invoice' : 'New invoice'} subtitle="Create a GST-ready invoice"><ButtonLink to="/invoices">Cancel</ButtonLink><Button type="submit" value="draft" disabled={!!busy}>{busy === 'draft' ? 'Saving…' : 'Save draft'}</Button><Button type="submit" value="issue" primary disabled={!!busy || !customers.length}>{busy === 'issue' ? 'Issuing…' : 'Issue invoice'}</Button></PageHeader><ErrorMessage message={error} /><div className="editor-layout"><div className="stack"><section className="card"><div className="section-heading"><h2>Invoice details</h2><Status status="DRAFT" /></div><div className="form-grid"><SelectField label="Customer" required value={values.customerId} onChange={change('customerId')} options={[{ value: '', label: 'Select a customer' }, ...customers.map((row) => ({ value: row.id, label: row.name }))]} /><Field label="Due date" type="date" required value={values.dueDate} onChange={change('dueDate')} /></div>{customer && <p className="muted customer-hint">{customer.gstin || 'Not registered for GST'} · {customer.state || customer.stateCode}</p>}{!customers.length && <p className="muted">Add a customer before creating an invoice. <Link className="table-link" to="/customers">Go to customers</Link></p>}</section><section className="card"><div className="section-heading"><h2>Line items</h2><Button type="button" onClick={() => setValues({ ...values, lines: [...values.lines, newLine()] })}>Add item</Button></div><div className="line-items">{values.lines.map((line, index) => <div className="line-item" key={line.key}><div className="line-item-top"><span className="muted">Item {index + 1}</span><button className="text-button danger-text" type="button" disabled={values.lines.length === 1} onClick={() => setValues({ ...values, lines: values.lines.filter((_, i) => i !== index) })}>Remove</button></div><div className="form-grid"><Field label="Description" required value={line.description} onChange={(event) => updateLine(index, 'description', event.target.value)} /><Field label="HSN / SAC code" value={line.hsnSacCode} onChange={(event) => updateLine(index, 'hsnSacCode', event.target.value)} /></div><div className="line-numbers"><Field label="Quantity" type="number" min="0.01" step="0.01" required value={line.quantity} onChange={(event) => updateLine(index, 'quantity', event.target.value)} /><Field label="Unit price (₹)" type="number" min="0" step="0.01" required value={line.unitPrice} onChange={(event) => updateLine(index, 'unitPrice', event.target.value)} /><Field label="Discount (₹)" type="number" min="0" step="0.01" required value={line.discountAmount} onChange={(event) => updateLine(index, 'discountAmount', event.target.value)} /><SelectField label="GST rate" value={line.taxRate} onChange={(event) => updateLine(index, 'taxRate', event.target.value)} options={[0, 5, 12, 18, 28].map((value) => ({ value, label: `${value}%` }))} /></div><div className="line-total">Line total <strong>{money(totals.lines[index].lineTotal)}</strong></div></div>)}</div></section><section className="card"><Field label="Notes and payment terms">{(fieldId) => <textarea id={fieldId} className="in textarea" rows={3} value={values.notes} onChange={change('notes')} />}</Field></section></div><section className="card invoice-summary"><h2>Invoice summary</h2><div className="sr"><span className="muted">Subtotal</span><span>{money(totals.subtotal)}</span></div>{totals.discountTotal > 0 && <div className="sr"><span className="muted">Discount</span><span>−{money(totals.discountTotal)}</span></div>}{interstate ? <div className="sr"><span className="muted">IGST</span><span>{money(totals.igstTotal)}</span></div> : <><div className="sr"><span className="muted">CGST</span><span>{money(totals.cgstTotal)}</span></div><div className="sr"><span className="muted">SGST</span><span>{money(totals.sgstTotal)}</span></div></>}<div className="sr grand-total"><span>Total</span><span>{money(totals.grandTotal)}</span></div><p className="muted">{interstate ? 'IGST applies for customers in another state.' : 'CGST and SGST apply for customers in your state.'}</p></section></div></form>;
}
