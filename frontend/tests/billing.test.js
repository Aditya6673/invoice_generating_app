import test from 'node:test';
import assert from 'node:assert/strict';
import { calculateInvoice, inDateRange, outstanding, paymentStatus, summary } from '../src/lib/billing.js';

test('same-state GST splits into CGST and SGST after discount', () => {
  const result = calculateInvoice([{ description: 'Rental', quantity: 2, unitPrice: 10000, discountAmount: 1000, taxRate: 18 }]);
  assert.equal(result.subtotal, 20000);
  assert.equal(result.cgstTotal, 1710);
  assert.equal(result.sgstTotal, 1710);
  assert.equal(result.igstTotal, 0);
  assert.equal(result.grandTotal, 22420);
});
test('interstate GST uses IGST', () => {
  const result = calculateInvoice([{ quantity: 2, unitPrice: 10000, discountAmount: 0, taxRate: 18 }], true);
  assert.equal(result.cgstTotal, 0);
  assert.equal(result.sgstTotal, 0);
  assert.equal(result.igstTotal, 3600);
  assert.equal(result.grandTotal, 23600);
});
test('small tax amounts round each GST component like the backend', () => {
  const result = calculateInvoice([{ quantity: 1, unitPrice: 0.06, taxRate: 18 }]);
  assert.equal(result.cgstTotal, 0.01);
  assert.equal(result.sgstTotal, 0.01);
  assert.equal(result.grandTotal, 0.08);
});
test('gross amounts round per line before the subtotal', () => {
  const result = calculateInvoice([{ quantity: 1.5, unitPrice: 0.03, taxRate: 0 }, { quantity: 1.5, unitPrice: 0.03, taxRate: 0 }]);
  assert.equal(result.subtotal, 0.1);
  assert.equal(result.grandTotal, 0.1);
});
test('payments derive paid, partial and overdue status', () => {
  const invoice = { grandTotal: 29500, dueDate: '2026-11-15' };
  assert.equal(paymentStatus(invoice, 18000, '2026-11-03'), 'PARTIALLY_PAID');
  assert.equal(paymentStatus(invoice, 29500, '2026-11-03'), 'PAID');
  assert.equal(paymentStatus(invoice, 18000, '2026-11-16'), 'OVERDUE');
});
test('drafts and voided invoices do not inflate receivables or sales', () => {
  const issued = { status: 'OVERDUE', grandTotal: 1000, amountPaid: 300, taxTotal: 100 };
  const invoices = [issued, { status: 'DRAFT', grandTotal: 5000 }, { status: 'VOID', grandTotal: 4000 }];
  assert.equal(outstanding(issued), 700);
  assert.deepEqual(summary(invoices), { invoiceCount: 1, totalSales: 1000, pendingAmount: 700, overdueAmount: 700, totalCollected: 300, taxTotal: 100 });
});
test('report date range includes both endpoints and excludes drafts without issue dates', () => {
  assert.equal(inDateRange({ issueDate: '2026-04-01' }, '2026-04-01', '2026-09-30'), true);
  assert.equal(inDateRange({ issueDate: '2026-09-30' }, '2026-04-01', '2026-09-30'), true);
  assert.equal(inDateRange({ issueDate: '2026-10-01' }, '2026-04-01', '2026-09-30'), false);
  assert.ok(!inDateRange({ issueDate: null }, '2026-04-01', '2026-09-30'));
});
