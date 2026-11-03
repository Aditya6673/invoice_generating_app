import { randomUUID } from 'node:crypto';
import { calculateInvoice, dateAfter, paymentStatus, summary, today } from '../src/lib/billing.js';

export function createFixtures() {
  const customers = [{ id: 'c1', name: 'Sharma Traders', gstin: '', phone: '9876543210', email: 'accounts@sharma.example', billingAddress: '12 Market Road', state: 'Maharashtra', stateCode: '27', city: 'Mumbai', pincode: '400001' }, { id: 'c2', name: 'Bluepeak Studio', gstin: '', phone: '9123456780', email: 'accounts@bluepeak.example', state: 'Karnataka', stateCode: '29' }];
  const makeInvoice = (id, customer, status, number, amount, paid = 0) => ({ id, customerId: customer.id, customerName: customer.name, invoiceNumber: `INV/2026-27/${number}`, status, issueDate: today(), dueDate: status === 'OVERDUE' ? '2026-01-01' : dateAfter(15), amountPaid: paid, notes: '', currency: 'INR', ...calculateInvoice([{ description: 'Professional services', hsnSacCode: '998313', quantity: 1, unitPrice: amount, discountAmount: 0, taxRate: 18 }], customer.stateCode !== '27') });
  const invoices = [makeInvoice('i104', customers[0], 'OVERDUE', '0104', 18000), makeInvoice('i98', customers[1], 'OVERDUE', '0098', 42500), makeInvoice('i115', customers[0], 'PARTIALLY_PAID', '0115', 25000, 18000)];
  return { customers, invoices, payments: [{ id: 'p1', invoiceId: 'i115', amount: 18000, paymentDate: today(), method: 'UPI', reference: 'TEST-1' }], business: { name: 'Test Business', gstin: '', stateCode: '27', address: '24 Business Park', invoicePrefix: 'INV', defaultPaymentTerms: 15 }, members: [{ id: 'm1', userName: 'Test Owner', userEmail: 'owner@example.com', role: 'OWNER' }] };
}
export function handleFixtureRequest(state, pathname, method, body, search) {
  if (pathname === '/customers' && method === 'GET') return { content: state.customers, totalPages: 1 };
  if (pathname === '/customers' && method === 'POST') { const customer = { ...body, id: randomUUID() }; state.customers.unshift(customer); return customer; }
  if (pathname.startsWith('/customers/') && method === 'PUT') { const id = pathname.split('/')[2]; const customer = { ...body, id }; state.customers = state.customers.map((row) => row.id === id ? customer : row); return customer; }
  if (pathname === '/invoices' && method === 'GET') return { content: state.invoices, totalPages: 1 };
  if ((pathname === '/invoices' && method === 'POST') || (/^\/invoices\/[^/]+$/.test(pathname) && method === 'PUT')) {
    const customer = state.customers.find((row) => row.id === body.customerId);
    const invoice = { ...body, ...calculateInvoice(body.lines, customer.stateCode !== state.business.stateCode), customerName: customer.name, id: method === 'POST' ? randomUUID() : pathname.split('/')[2], invoiceNumber: null, status: 'DRAFT', issueDate: null, amountPaid: 0 };
    state.invoices = [invoice, ...state.invoices.filter((row) => row.id !== invoice.id)]; return invoice;
  }
  if (/^\/invoices\/[^/]+$/.test(pathname) && method === 'GET') return state.invoices.find((row) => row.id === pathname.split('/')[2]);
  if (pathname.endsWith('/issue') && method === 'POST') { const invoice = state.invoices.find((row) => row.id === pathname.split('/')[2]); invoice.status = 'ISSUED'; invoice.invoiceNumber = `INV/2026-27/${String(state.invoices.length + 121).padStart(4, '0')}`; invoice.issueDate = today(); return invoice; }
  if (pathname.endsWith('/payments')) {
    const invoiceId = pathname.split('/')[2];
    if (method === 'GET') return state.payments.filter((row) => row.invoiceId === invoiceId);
    const payment = { ...body, id: randomUUID(), invoiceId };
    state.payments.unshift(payment);
    const invoice = state.invoices.find((row) => row.id === invoiceId); invoice.amountPaid += body.amount; invoice.status = paymentStatus(invoice, invoice.amountPaid); return payment;
  }
  if (pathname === '/business/profile') { if (method === 'PUT') state.business = { ...state.business, ...body }; return state.business; }
  if (pathname === '/business/members') { if (method === 'POST') state.members.push({ ...body, id: randomUUID(), userName: body.name, userEmail: body.email }); return method === 'POST' ? state.members.at(-1) : state.members; }
  if (pathname === '/reports/summary') return summary(state.invoices.filter((row) => (!search.get('from') || row.issueDate >= search.get('from')) && (!search.get('to') || row.issueDate <= search.get('to'))));
  if (pathname === '/reports/top-customers') return [];
  if (pathname.endsWith('/reminders')) return { id: randomUUID() };
  throw new Error(`Unhandled fixture request: ${method} ${pathname}`);
}
