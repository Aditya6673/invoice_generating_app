const API_URL = (import.meta.env.VITE_API_URL || '/api/v1').replace(/\/$/, '');
const SESSION_KEY = 'invoice-platform-session';
let refreshPromise;
export const getSession = () => {
  try { return JSON.parse(sessionStorage.getItem(SESSION_KEY) || 'null'); } catch { return null; }
};
export const setSession = (session) => session ? sessionStorage.setItem(SESSION_KEY, JSON.stringify(session)) : sessionStorage.removeItem(SESSION_KEY);

export async function request(path, { method = 'GET', body, blob = false, retry = true } = {}) {
  const session = getSession();
  if (!path.startsWith('/auth/') && !session?.accessToken) throw new Error('Please sign in to access your business.');
  let response;
  try {
    response = await fetch(`${API_URL}${path}`, {
      method,
      headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...(session?.accessToken ? { Authorization: `Bearer ${session.accessToken}` } : {}) },
      body: body ? JSON.stringify(body) : undefined,
      signal: AbortSignal.timeout(15000),
    });
  } catch {
    throw new Error('Cannot connect to the billing server. Check that the backend is running and try again.');
  }
  if (response.status === 401 && session?.refreshToken && retry && !path.startsWith('/auth/')) {
    refreshPromise ||= request('/auth/refresh', { method: 'POST', body: { refreshToken: session.refreshToken }, retry: false }).then((tokens) => { setSession({ ...session, ...tokens }); }).finally(() => { refreshPromise = null; });
    try { await refreshPromise; return request(path, { method, body, blob, retry: false }); }
    catch { setSession(null); window.dispatchEvent(new Event('billing-session-expired')); throw new Error('Your session has expired. Please sign in again.'); }
  }
  if (response.status === 204) return null;
  if (response.status === 401 && !path.startsWith('/auth/')) {
    setSession(null);
    window.dispatchEvent(new Event('billing-session-expired'));
  }
  if (blob && response.ok) return response.blob();
  const data = await response.json().catch(() => null);
  if (!response.ok || data?.success === false) throw new Error(data?.errors?.join(' · ') || data?.message || `Request failed (${response.status}). Please try again.`);
  return data?.data ?? data;
}
export async function fetchAll(path) {
  const separator = path.includes('?') ? '&' : '?';
  const first = await request(`${path}${separator}size=100&page=0`);
  if (Array.isArray(first)) return first;
  const rows = [...(first?.content || [])];
  for (let page = 1; page < (first?.totalPages || 1); page++) {
    const next = await request(`${path}${separator}size=100&page=${page}`);
    rows.push(...(next?.content || []));
  }
  return rows;
}
