import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useBilling } from '../context/BillingContext.jsx';
import { Button, ButtonLink, Empty, ErrorMessage, Field, InvoiceTable, PageHeader, SelectField, StatCards } from '../components/ui.jsx';
import { compactMoney, money, outstanding, states, summary } from '../lib/billing.js';

function CustomerForm({ customer, onClose }) {
  const { saveCustomer, notify } = useBilling();
  const [values, setValues] = useState({ name: '', email: '', phone: '', gstin: '', billingAddress: '', city: '', stateCode: '27', pincode: '', ...customer });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const change = (key) => (event) => setValues({ ...values, [key]: event.target.value });
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError('');
    try { await saveCustomer({ ...values, state: states[values.stateCode] || values.state }, customer?.id); notify(customer ? 'Customer updated.' : 'Customer added.'); onClose(); }
    catch (failure) { setError(failure.message); }
    finally { setBusy(false); }
  }
  return <dialog className="modal" open aria-labelledby="customer-form-title" onCancel={onClose}><form onSubmit={submit}><div className="section-heading"><h2 id="customer-form-title">{customer ? 'Edit customer' : 'Add customer'}</h2><button type="button" className="close-button" onClick={onClose} aria-label="Close customer form">×</button></div><ErrorMessage message={error} /><div className="form-grid"><Field label="Customer name" value={values.name} onChange={change('name')} required autoFocus className="full" /><Field label="Email" type="email" value={values.email} onChange={change('email')} /><Field label="Phone" type="tel" maxLength={15} value={values.phone} onChange={change('phone')} /><Field label="GSTIN (optional)" maxLength={15} pattern="[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][0-9A-Z]Z[0-9A-Z]" title="Enter a valid 15-character GSTIN" value={values.gstin} onChange={(event) => setValues({ ...values, gstin: event.target.value.toUpperCase() })} /><SelectField label="State" options={Object.entries(states).map(([value, label]) => ({ value, label: `${label} (${value})` }))} value={values.stateCode} onChange={change('stateCode')} /><Field label="Billing address" className="full" value={values.billingAddress} onChange={change('billingAddress')} /><Field label="City" value={values.city} onChange={change('city')} /><Field label="PIN code" maxLength={6} pattern="[0-9]{6}|" value={values.pincode} onChange={change('pincode')} /></div><div className="form-actions"><Button type="button" disabled={busy} onClick={onClose}>Cancel</Button><Button primary disabled={busy}>{busy ? 'Saving…' : 'Save customer'}</Button></div></form></dialog>;
}
export default function Customers() {
  const { customers, invoices } = useBilling();
  const [search, setSearch] = useState('');
  const [showForm, setShowForm] = useState(false);
  const filtered = customers.filter((customer) => `${customer.name} ${customer.gstin} ${customer.phone} ${customer.email}`.toLowerCase().includes(search.toLowerCase()));
  return <><PageHeader title="Customers" subtitle="Saved customers and what they owe"><Button primary onClick={() => setShowForm(true)}>Add customer</Button></PageHeader><div className="card spaced"><Field label="Search" type="search" placeholder="Search by name, GSTIN, email or phone" value={search} onChange={(event) => setSearch(event.target.value)} /></div><div className="card">{filtered.length ? <div className="table-scroll"><table><thead><tr><th>Customer</th><th>GSTIN</th><th>Phone</th><th className="r">Invoices</th><th className="r">Outstanding</th></tr></thead><tbody>{filtered.map((customer) => {
    const history = invoices.filter((invoice) => invoice.customerId === customer.id);
    return <tr key={customer.id}><td><Link className="table-link" to={`/customers/${customer.id}`}>{customer.name}</Link></td><td>{customer.gstin || 'Not registered'}</td><td>{customer.phone || '—'}</td><td className="r">{history.length}</td><td className="r">{money(history.reduce((sum, invoice) => sum + outstanding(invoice), 0))}</td></tr>;
  })}</tbody></table></div> : <Empty title={search ? 'No customers found' : 'No customers yet'} description={search ? 'Try a different name, email or GSTIN.' : 'Add your first customer to start creating invoices.'} />}</div>{showForm && <div className="modal-backdrop" onClick={(event) => { if (event.target === event.currentTarget) setShowForm(false); }}><CustomerForm onClose={() => setShowForm(false)} /></div>}</>;
}
export function CustomerDetail() {
  const { id } = useParams();
  const { customers, invoices } = useBilling();
  const [editing, setEditing] = useState(false);
  const customer = customers.find((row) => row.id === id);
  if (!customer) return <div className="card"><Empty title="Customer not found" description="Return to your customers to select a current record."><ButtonLink to="/customers">Back to customers</ButtonLink></Empty></div>;
  const history = invoices.filter((invoice) => invoice.customerId === id);
  const stats = summary(history);
  const details = [['GSTIN', customer.gstin || 'Not registered'], ['State', `${customer.state || states[customer.stateCode] || '—'} (${customer.stateCode || '—'})`], ['Email', customer.email || '—'], ['Phone', customer.phone || '—'], ['Address', [customer.billingAddress, customer.city, customer.pincode].filter(Boolean).join(', ') || '—']];
  return <><Link className="back-link" to="/customers">← Customers</Link><PageHeader title={customer.name} subtitle="Customer profile"><Button onClick={() => setEditing(true)}>Edit</Button><ButtonLink primary to={`/invoices/new?customer=${id}`}>New invoice</ButtonLink></PageHeader><div className="customer-layout"><section className="card"><h2>Details</h2><dl className="detail-list">{details.map(([label, value]) => <div className="sr" key={label}><dt className="muted">{label}</dt><dd>{value}</dd></div>)}</dl></section><div><StatCards className="three-stats" items={[{ label: 'Total billed', value: compactMoney(stats.totalSales) }, { label: 'Outstanding', value: money(stats.pendingAmount), danger: true }, { label: 'Invoices', value: history.length }]} /><section className="card"><h2 className="table-heading">Invoice history</h2><InvoiceTable compact invoices={history} /></section></div></div>{editing && <div className="modal-backdrop"><CustomerForm customer={customer} onClose={() => setEditing(false)} /></div>}</>;
}
