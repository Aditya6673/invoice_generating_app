import { useEffect, useState } from 'react';
import { Link, NavLink, Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom';
import { useBilling } from './context/BillingContext.jsx';
import { Button, Empty } from './components/ui.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Invoices from './pages/Invoices.jsx';
import InvoiceEditor from './pages/InvoiceEditor.jsx';
import InvoiceDetail from './pages/InvoiceDetail.jsx';
import Customers, { CustomerDetail } from './pages/Customers.jsx';
import Payments from './pages/Payments.jsx';
import Reports from './pages/Reports.jsx';
import Settings from './pages/Settings.jsx';
import SignIn from './pages/SignIn.jsx';

function Shell() {
  const { authenticated, user, role, signOut, loading, error, refresh } = useBilling();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);
  useEffect(() => { setMenuOpen(false); window.scrollTo(0, 0); }, [location.pathname]);
  if (!authenticated) return <Navigate to="/sign-in" replace />;
  const links = [['/', 'Dashboard'], ['/invoices', 'Invoices'], ['/customers', 'Customers'], ['/payments', 'Payments'], ['/reports', 'Reports'], ['/settings', 'Settings']];
  return <div className="app"><a className="skip-link" href="#main-content">Skip to content</a>
    <aside className={`side ${menuOpen ? 'menu-open' : ''}`}><div className="brand-row"><Link className="brand" to="/">Invoice Platform</Link><button className="menu-toggle" aria-label="Toggle navigation" aria-expanded={menuOpen} onClick={() => setMenuOpen(!menuOpen)}>☰</button></div>
      <nav aria-label="Main navigation">{links.filter(([path]) => path !== '/reports' || role !== 'STAFF').map(([path, label]) => <NavLink key={path} to={path} end={path === '/'} className={({ isActive }) => isActive ? 'on' : ''}>{label}</NavLink>)}</nav>
      <div className="sidebar-footer"><div className="sidebar-user">{user.name}</div><div className="sidebar-caption">{role.charAt(0) + role.slice(1).toLowerCase()}</div><Button onClick={signOut}>Sign out</Button></div>
    </aside>
    <main className="main" id="main-content" tabIndex={-1}>{loading ? <div className="loading" role="status"><span className="spinner" />Loading your workspace…</div> : error ? <div className="load-error" role="alert"><span>{error}</span><Button onClick={refresh}>Retry</Button></div> : <Outlet />}</main>
  </div>;
}
function NotFound() { return <div className="card"><Empty title="Page not found" description="This page may have moved or the record is no longer available."><Link className="btn p" to="/">Back to dashboard</Link></Empty></div>; }
function RoleGuard({ children }) { return useBilling().role === 'STAFF' ? <Navigate to="/" replace /> : children; }
export default function App() {
  const { toast, dismissToast } = useBilling();
  const location = useLocation();
  useEffect(() => { const page = location.pathname.split('/')[1] || 'Dashboard'; document.title = `${page.charAt(0).toUpperCase()}${page.slice(1).replace('-', ' ')} · Invoice Platform`; }, [location.pathname]);
  return <><Routes><Route path="/sign-in" element={<SignIn />} /><Route element={<Shell />}><Route index element={<Dashboard />} /><Route path="invoices" element={<Invoices />} /><Route path="invoices/new" element={<InvoiceEditor />} /><Route path="invoices/:id/edit" element={<InvoiceEditor />} /><Route path="invoices/:id" element={<InvoiceDetail />} /><Route path="customers" element={<Customers />} /><Route path="customers/:id" element={<CustomerDetail />} /><Route path="payments" element={<Payments />} /><Route path="reports" element={<RoleGuard><Reports /></RoleGuard>} /><Route path="settings" element={<Settings />} /><Route path="*" element={<NotFound />} /></Route></Routes>{toast && <div className={`toast ${toast.type}`} role="status"><span>{toast.message}</span><button onClick={dismissToast} aria-label="Dismiss notification">×</button></div>}</>;
}
