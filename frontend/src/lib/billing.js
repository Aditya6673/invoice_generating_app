export const round = (value) => Math.round((Number(value) + Number.EPSILON) * 100) / 100;
export const money = (value = 0) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(Number(value)).replace(/\.00$/, '');
export const compactMoney = (value = 0) => Number(value) >= 100000 ? `₹${(Number(value) / 100000).toFixed(1)}L` : money(value);
export const today = () => {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};
export const formatDate = (value) => value ? new Date(`${value.slice(0, 10)}T12:00:00`).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) : '—';
export const dateAfter = (days) => {
  const date = new Date(`${today()}T12:00:00`);
  date.setDate(date.getDate() + Number(days));
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};
export const statusLabels = { DRAFT: 'Draft', ISSUED: 'Unpaid', UNPAID: 'Unpaid', PARTIALLY_PAID: 'Partly paid', PAID: 'Paid', OVERDUE: 'Overdue', VOID: 'Void', VOIDED: 'Void' };
export const methods = { UPI: 'UPI', BANK_TRANSFER: 'Bank transfer', CASH: 'Cash', CREDIT_CARD: 'Credit card', CHEQUE: 'Cheque', OTHER: 'Other' };
export const states = { '27': 'Maharashtra', '29': 'Karnataka', '07': 'Delhi', '24': 'Gujarat', '33': 'Tamil Nadu', '36': 'Telangana', '09': 'Uttar Pradesh', '19': 'West Bengal', '06': 'Haryana', '08': 'Rajasthan', '32': 'Kerala', '03': 'Punjab', '10': 'Bihar', '23': 'Madhya Pradesh', '21': 'Odisha', '20': 'Jharkhand', '22': 'Chhattisgarh', '18': 'Assam', '37': 'Andhra Pradesh', '30': 'Goa', '05': 'Uttarakhand', '02': 'Himachal Pradesh', '01': 'Jammu and Kashmir', '04': 'Chandigarh', '11': 'Sikkim', '12': 'Arunachal Pradesh', '13': 'Nagaland', '14': 'Manipur', '15': 'Mizoram', '16': 'Tripura', '17': 'Meghalaya', '26': 'Dadra and Nagar Haveli and Daman and Diu', '31': 'Lakshadweep', '34': 'Puducherry', '35': 'Andaman and Nicobar Islands', '38': 'Ladakh' };

export function calculateInvoice(lines, interstate = false) {
  const calculated = lines.map((line) => {
    const gross = round(Number(line.quantity) * Number(line.unitPrice));
    const discountAmount = round(Math.min(gross, Math.max(0, Number(line.discountAmount) || 0)));
    const taxableAmount = round(gross - discountAmount);
    const tax = round(taxableAmount * Number(line.taxRate) / 100);
    const cgstAmount = interstate ? 0 : round(taxableAmount * Number(line.taxRate) / 200);
    const sgstAmount = cgstAmount;
    const igstAmount = interstate ? tax : 0;
    return { ...line, discountAmount, taxableAmount, cgstAmount, sgstAmount, igstAmount, lineTotal: round(taxableAmount + cgstAmount + sgstAmount + igstAmount) };
  });
  const sum = (key) => round(calculated.reduce((total, line) => total + Number(line[key]), 0));
  const subtotal = round(lines.reduce((total, line) => total + round(Number(line.quantity) * Number(line.unitPrice)), 0));
  const discountTotal = sum('discountAmount');
  const cgstTotal = sum('cgstAmount');
  const sgstTotal = sum('sgstAmount');
  const igstTotal = sum('igstAmount');
  const taxTotal = round(cgstTotal + sgstTotal + igstTotal);
  return { lines: calculated, subtotal, discountTotal, cgstTotal, sgstTotal, igstTotal, taxTotal, grandTotal: round(subtotal - discountTotal + taxTotal) };
}

export function outstanding(invoice) {
  return ['DRAFT', 'VOID', 'VOIDED'].includes(invoice.status) ? 0 : Math.max(0, round(Number(invoice.grandTotal) - Number(invoice.amountPaid || 0)));
}
export function paymentStatus(invoice, paid, asOf = today()) {
  if (round(paid) >= round(invoice.grandTotal)) return 'PAID';
  if (invoice.dueDate && invoice.dueDate < asOf) return 'OVERDUE';
  return Number(paid) > 0 ? 'PARTIALLY_PAID' : 'ISSUED';
}
export function summary(invoices) {
  const issued = invoices.filter((invoice) => !['DRAFT', 'VOID', 'VOIDED'].includes(invoice.status));
  return {
    invoiceCount: issued.length,
    totalSales: round(issued.reduce((sum, invoice) => sum + Number(invoice.grandTotal), 0)),
    pendingAmount: round(issued.reduce((sum, invoice) => sum + outstanding(invoice), 0)),
    overdueAmount: round(issued.filter((invoice) => invoice.status === 'OVERDUE').reduce((sum, invoice) => sum + outstanding(invoice), 0)),
    totalCollected: round(issued.reduce((sum, invoice) => sum + Number(invoice.amountPaid || 0), 0)),
    taxTotal: round(issued.reduce((sum, invoice) => sum + Number(invoice.taxTotal || 0), 0)),
  };
}
export function inDateRange(invoice, from, to) {
  return invoice.issueDate && (!from || invoice.issueDate >= from) && (!to || invoice.issueDate <= to);
}
