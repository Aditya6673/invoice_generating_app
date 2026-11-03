import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { outstanding, paymentStatus, round } from '../lib/billing.js';
import { fetchAll, getSession, request, setSession } from '../lib/api.js';

const BillingContext = createContext(null);
const emptyData = () => ({ invoices: [], customers: [], payments: [], business: {}, members: [] });
const upsert = (rows, item) => rows.some((row) => row.id === item.id) ? rows.map((row) => row.id === item.id ? item : row) : [item, ...rows];

export function BillingProvider({ children }) {
  const [session, updateSession] = useState(getSession);
  const [data, setData] = useState(emptyData);
  const [loading, setLoading] = useState(!!session);
  const [error, setError] = useState('');
  const [toast, setToast] = useState(null);
  const loadVersion = useRef(0);
  const notify = useCallback((message, type = 'success') => setToast({ message, type, id: Date.now() }), []);
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), 5000);
    return () => clearTimeout(timer);
  }, [toast]);
  const signOut = useCallback(() => {
    loadVersion.current++;
    setSession(null);
    updateSession(null);
    setData(emptyData());
    setError('');
    setLoading(false);
  }, []);
  useEffect(() => {
    const expired = () => { signOut(); notify('Your session has expired. Please sign in again.', 'error'); };
    window.addEventListener('billing-session-expired', expired);
    return () => window.removeEventListener('billing-session-expired', expired);
  }, [signOut, notify]);
  const refresh = useCallback(async () => {
    if (!session) return;
    const version = ++loadVersion.current;
    setLoading(true);
    setError('');
    try {
      const [customers, invoices, business, members] = await Promise.all([
        fetchAll('/customers'), fetchAll('/invoices'), request('/business/profile'),
        session.user?.role === 'OWNER' ? request('/business/members') : Promise.resolve([]),
      ]);
      const issued = invoices.filter((invoice) => !['DRAFT', 'VOID'].includes(invoice.status));
      const payments = [];
      for (let index = 0; index < issued.length; index += 6) {
        const batch = await Promise.all(issued.slice(index, index + 6).map((invoice) => request(`/invoices/${invoice.id}/payments`)));
        payments.push(...batch.flat());
      }
      if (version === loadVersion.current) setData({ customers, invoices: invoices.map((invoice) => ({ ...invoice, customerName: invoice.customerName || customers.find((customer) => customer.id === invoice.customerId)?.name || 'Customer' })), business, members, payments });
    } catch (failure) { if (version === loadVersion.current) setError(failure.message); }
    finally { if (version === loadVersion.current) setLoading(false); }
  }, [session]);
  useEffect(() => { refresh(); }, [refresh]);
  async function authenticate(values, register = false) {
    const auth = await request(register ? '/auth/register' : '/auth/login', { method: 'POST', body: values, retry: false });
    setSession(auth);
    setData(emptyData());
    setLoading(true);
    updateSession(auth);
  }
  async function saveCustomer(values, id) {
    const item = await request(`/customers${id ? `/${id}` : ''}`, { method: id ? 'PUT' : 'POST', body: values });
    setData((previous) => ({ ...previous, customers: upsert(previous.customers, item) }));
    return item;
  }
  async function saveInvoice(values, id, issue = false) {
    let item = await request(`/invoices${id ? `/${id}` : ''}`, { method: id ? 'PUT' : 'POST', body: values });
    item = { ...item, customerName: item.customerName || data.customers.find((customer) => customer.id === item.customerId)?.name || 'Customer' };
    setData((previous) => ({ ...previous, invoices: upsert(previous.invoices, item) }));
    if (issue) {
      try {
        const issued = await request(`/invoices/${item.id}/issue`, { method: 'POST' });
        item = { ...item, ...issued };
      } catch (failure) {
        const error = new Error(`Draft saved, but issuing failed: ${failure.message}`);
        error.draftId = item.id;
        throw error;
      }
      setData((previous) => ({ ...previous, invoices: upsert(previous.invoices, item) }));
    }
    return item;
  }
  async function recordPayment(invoiceId, values) {
    const invoice = data.invoices.find((row) => row.id === invoiceId);
    if (!invoice || ['DRAFT', 'VOID'].includes(invoice.status)) throw new Error('Payments can only be recorded for issued invoices.');
    if (!Number.isFinite(Number(values.amount)) || Number(values.amount) <= 0 || round(values.amount) > outstanding(invoice)) throw new Error('Enter a payment greater than zero and no higher than the outstanding amount.');
    const item = await request(`/invoices/${invoiceId}/payments`, { method: 'POST', body: values });
    const amountPaid = round(Number(invoice.amountPaid || 0) + Number(item.amount));
    const updated = { ...invoice, amountPaid, status: paymentStatus(invoice, amountPaid) };
    setData((previous) => ({ ...previous, invoices: upsert(previous.invoices, updated), payments: [item, ...previous.payments] }));
    try {
      const confirmed = await request(`/invoices/${invoiceId}`);
      setData((previous) => ({ ...previous, invoices: upsert(previous.invoices, confirmed) }));
    } catch { notify('Payment saved. Refresh the page to confirm the latest balance.', 'error'); }
    return item;
  }
  async function saveBusiness(values) {
    const saved = await request('/business/profile', { method: 'PUT', body: { ...values, gstin: values.gstin || null } });
    setData((previous) => ({ ...previous, business: saved }));
  }
  async function invite(values) {
    const item = await request('/business/members', { method: 'POST', body: values });
    setData((previous) => ({ ...previous, members: upsert(previous.members, item) }));
  }
  async function sendReminder(id) {
    await request(`/invoices/${id}/reminders`, { method: 'POST' });
    notify('Payment reminder sent.');
  }
  return <BillingContext.Provider value={{ ...data, authenticated: !!session, role: session?.user?.role || '', user: session?.user || null, loading, error, refresh, toast, notify, dismissToast: () => setToast(null), authenticate, signOut, saveCustomer, saveInvoice, recordPayment, saveBusiness, invite, sendReminder }}>{children}</BillingContext.Provider>;
}
export const useBilling = () => useContext(BillingContext);
