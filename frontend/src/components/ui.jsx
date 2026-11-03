import { useId, useState } from 'react';
import { Link } from 'react-router-dom';
import { formatDate, money, statusLabels } from '../lib/billing.js';

export function Button({ primary, className = '', children, ...props }) {
  return <button className={`btn ${primary ? 'p' : ''} ${className}`} {...props}>{children}</button>;
}
export function ButtonLink({ to, primary, children, ...props }) {
  return <Link to={to} className={`btn ${primary ? 'p' : ''}`} {...props}>{children}</Link>;
}
export function PageHeader({ title, subtitle, children }) {
  return <div className="top"><div><h1>{title}</h1>{subtitle && <div className="muted subtitle">{subtitle}</div>}</div>{children && <div className="actions">{children}</div>}</div>;
}
export function Field({ label, children, className = '', ...props }) {
  const id = useId();
  return <div className={`field ${className}`}><label htmlFor={id}>{label}</label>{children ? children(id) : <input id={id} className="in" {...props} />}</div>;
}
export function SelectField({ label, options, ...props }) {
  return <Field label={label}>{(id) => <select id={id} className="in" {...props}>{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select>}</Field>;
}
export function Status({ status }) {
  const colors = { DRAFT: 'draft', ISSUED: 'unp', UNPAID: 'unp', PARTIALLY_PAID: 'part', PAID: 'paid', OVERDUE: 'over', VOID: 'draft', VOIDED: 'draft' };
  return <span className={`chip ${colors[status] || 'draft'}`}>{statusLabels[status] || status}</span>;
}
export function StatCards({ items, className = '' }) {
  return <div className={`g4 ${className}`}>{items.map(({ label, value, danger }) => <div className="card" key={label}><div className="muted">{label}</div><div className={`big ${danger ? 'danger-text' : ''}`}>{value}</div></div>)}</div>;
}
export function Empty({ title = 'No results found', description = 'Try a different search or filter.', children }) {
  return <div className="empty"><h2>{title}</h2><p className="muted">{description}</p>{children}</div>;
}
export function ErrorMessage({ message }) {
  return message ? <div className="form-error" role="alert">{message}</div> : null;
}
export function InvoiceTable({ invoices, compact = false }) {
  const [page, setPage] = useState(1);
  const pages = Math.max(1, Math.ceil(invoices.length / 10));
  const current = Math.min(page, pages);
  const visible = invoices.slice((current - 1) * 10, current * 10);
  return <>{invoices.length ? <div className="table-scroll"><table className={compact ? 'compact-table' : ''}><thead><tr><th>Invoice</th>{!compact && <th>Customer</th>}<th>Issued</th>{!compact && <th>Due</th>}<th className="r">Amount</th><th>Status</th></tr></thead><tbody>{visible.map((invoice) => <tr key={invoice.id}><td><Link className="table-link" to={`/invoices/${invoice.id}`}>{invoice.invoiceNumber || 'Draft'}</Link></td>{!compact && <td>{invoice.customerName}</td>}<td>{formatDate(invoice.issueDate)}</td>{!compact && <td>{formatDate(invoice.dueDate)}</td>}<td className="r">{money(invoice.grandTotal)}</td><td><Status status={invoice.status} /></td></tr>)}</tbody></table></div> : <Empty />}{pages > 1 && <div className="pagination"><span className="muted">Page {current} of {pages} · {invoices.length} invoices</span><div className="actions"><Button disabled={current === 1} onClick={() => setPage(current - 1)}>Previous</Button><Button disabled={current === pages} onClick={() => setPage(current + 1)}>Next</Button></div></div>}</>;
}
export function MonthlyChart({ sales }) {
  const maximum = Math.max(1000000, ...sales.map((item) => Number(item.total)));
  return <div className="bar-chart" role="img" aria-label={`Monthly sales: ${sales.map((item) => `${item.month} ${(Number(item.total) / 100000).toFixed(1)} lakh`).join(', ')}`}>
    {sales.map((item) => <div className="bar-column" key={item.month}><span className="muted">{(Number(item.total) / 100000).toFixed(1)}</span><div className="bar" style={{ height: `${Number(item.total) / maximum * 200}px` }} title={money(item.total)} /><span className="muted">{item.month}</span></div>)}
  </div>;
}
