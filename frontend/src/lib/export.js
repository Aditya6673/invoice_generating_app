import { request } from './api.js';
import { methods } from './billing.js';

export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url; link.download = filename;
  document.body.appendChild(link); link.click(); link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
export async function downloadPdf(invoice) {
  const filename = `${(invoice.invoiceNumber || 'invoice').replaceAll('/', '-')}.pdf`;
  downloadBlob(await request(`/invoices/${invoice.id}/pdf`, { blob: true }), filename);
}
const safeCell = (value) => /^[=+\-@\t\r\n]/.test(String(value)) ? `'${value}` : String(value ?? '');
export function exportRows(invoices, payments) {
  return [['Invoice', 'Customer', 'Date', 'Method', 'Reference', 'Amount'], ...payments.map((payment) => {
    const invoice = invoices.find((row) => row.id === payment.invoiceId);
    return [invoice?.invoiceNumber || '', invoice?.customerName || '', payment.paymentDate, methods[payment.method] || payment.method, payment.reference || '', Number(payment.amount)];
  })];
}
export function exportCsv(rows, filename = 'payments.csv') {
  const csv = '\uFEFF' + rows.map((row) => row.map((value) => `"${safeCell(value).replaceAll('"', '""')}"`).join(',')).join('\r\n');
  downloadBlob(new Blob([csv], { type: 'text/csv;charset=utf-8' }), filename);
}
