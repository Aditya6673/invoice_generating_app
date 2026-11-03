import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useBilling } from '../context/BillingContext.jsx';
import { Button, ErrorMessage, Field, SelectField } from '../components/ui.jsx';
import { states } from '../lib/billing.js';

export default function SignIn() {
  const { authenticate, authenticated } = useBilling();
  const navigate = useNavigate();
  const [register, setRegister] = useState(false);
  const [values, setValues] = useState({ email: '', password: '', name: '', businessName: '', stateCode: '27' });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [resetHelp, setResetHelp] = useState(false);
  const change = (key) => (event) => setValues({ ...values, [key]: event.target.value });
  if (authenticated) return <Navigate to="/" replace />;
  async function submit(event) { event.preventDefault(); setBusy(true); setError(''); try { await authenticate(register ? values : { email: values.email, password: values.password }, register); navigate('/'); } catch (failure) { setError(failure.message); } finally { setBusy(false); } }
  return <div className="auth-layout"><section className="auth-brand"><Link to="/" className="auth-logo">Invoice Platform</Link><h2>Invoices, payments and GST in one place.</h2><p>Create GST-ready invoices, see who has paid, and send reminders without chasing.</p></section><div className="auth-main"><form className="card auth-card" onSubmit={submit}><h1>{register ? 'Create your account' : 'Sign in'}</h1><ErrorMessage message={error} /><div className="stack">{register && <><Field label="Your name" required autoComplete="name" value={values.name} onChange={change('name')} /><Field label="Business name" required autoComplete="organization" value={values.businessName} onChange={change('businessName')} /><SelectField label="Business state" value={values.stateCode} onChange={change('stateCode')} options={Object.entries(states).map(([value, label]) => ({ value, label }))} /></>}<Field label="Email" type="email" required autoComplete="email" value={values.email} onChange={change('email')} /><Field label="Password" type="password" required minLength={register ? 8 : undefined} autoComplete={register ? 'new-password' : 'current-password'} value={values.password} onChange={change('password')} /><Button primary disabled={busy}>{busy ? 'Please wait…' : register ? 'Create account' : 'Sign in'}</Button></div>{!register && <div className="auth-footer"><button type="button" className="text-button" onClick={() => setResetHelp(!resetHelp)}>Forgot password?</button>{resetHelp && <p className="muted" role="status">Contact your business administrator to recover access to your account.</p>}</div>}<div className="auth-footer"><span className="muted">{register ? 'Already have an account? ' : 'New to Invoice Platform? '}</span><button type="button" className="text-button" onClick={() => { setRegister(!register); setError(''); }}>{register ? 'Sign in' : 'Create account'}</button></div></form></div></div>;
}
