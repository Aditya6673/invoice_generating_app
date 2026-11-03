import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { createServer } from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { createFixtures, handleFixtureRequest } from './fixtures.js';

const output = path.resolve('test-results');
fs.mkdirSync(output, { recursive: true });
const fixtures = createFixtures();
const requests = [];
let failData = false;
let tokenExpired = false;
const api = createServer(async (req, res) => {
  const url = new URL(req.url, 'http://localhost');
  const pathname = url.pathname.replace('/api/v1', '');
  const chunks = []; for await (const chunk of req) chunks.push(chunk);
  const body = chunks.length ? JSON.parse(Buffer.concat(chunks).toString()) : null;
  requests.push({ pathname, method: req.method, body, authorization: req.headers.authorization });
  res.setHeader('Content-Type', 'application/json');
  if (pathname === '/auth/login') {
    if (body.email !== 'owner@example.com') { res.writeHead(401); res.end(JSON.stringify({ success: false, message: 'Invalid email or password' })); return; }
    res.end(JSON.stringify({ success: true, data: { accessToken: 'test-access', refreshToken: 'test-refresh', user: { name: 'Test Owner', role: 'OWNER', businessId: 'test-business' } } })); return;
  }
  if (pathname === '/auth/refresh') { tokenExpired = false; res.end(JSON.stringify({ success: true, data: { accessToken: 'refreshed-access', refreshToken: 'test-refresh', user: { name: 'Test Owner', role: 'OWNER', businessId: 'test-business' } } })); return; }
  if (!req.headers.authorization || tokenExpired) { res.writeHead(401); res.end(JSON.stringify({ success: false, message: 'Session expired' })); return; }
  if (failData) { res.writeHead(503); res.end(JSON.stringify({ success: false, message: 'Billing service unavailable' })); return; }
  if (pathname.endsWith('/pdf')) { res.setHeader('Content-Type', 'application/pdf'); res.end('%PDF-1.4\nTest fixture PDF\n%%EOF'); return; }
  if (pathname === '/reports/export') { res.setHeader('Content-Type', url.searchParams.get('type') === 'xlsx' ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' : 'text/csv'); res.end('Fixture invoice export'); return; }
  try { const data = handleFixtureRequest(fixtures, pathname, req.method, body, url.searchParams); res.end(JSON.stringify({ success: true, data })); }
  catch (error) { res.writeHead(404); res.end(JSON.stringify({ success: false, message: error.message })); }
});
await new Promise((resolve) => api.listen(5181, '127.0.0.1', resolve));
const server = spawn(process.execPath, ['node_modules/vite/bin/vite.js', '--host', '127.0.0.1', '--port', '5175', '--strictPort'], { windowsHide: true, env: { ...process.env, VITE_BACKEND_URL: 'http://127.0.0.1:5181' }, stdio: ['ignore', 'pipe', 'pipe'] });
let serverOutput = '';
server.stdout.on('data', (chunk) => { serverOutput += chunk; }); server.stderr.on('data', (chunk) => { serverOutput += chunk; });
const pause = (time) => new Promise((resolve) => setTimeout(resolve, time));
let browser, session, browserErrors = '';
const pending = new Map();
const events = [];
let nextId = 1;
function send(method, params = {}, sessionId) {
  const id = nextId++;
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => { pending.delete(id); reject(new Error(`CDP timed out: ${method}`)); }, 10000);
    pending.set(id, { resolve, reject, timer });
    browser.stdio[3].write(JSON.stringify({ id, method, params, ...(sessionId ? { sessionId } : {}) }) + '\0');
  });
}
async function until(check, label, timeout = 10000) { const start = Date.now(); while (Date.now() - start < timeout) { if (await check()) return; await pause(75); } throw new Error(`Timed out waiting for ${label}`); }
async function evaluate(fn, ...args) {
  const response = await send('Runtime.evaluate', { expression: `(${fn.toString()})(${args.map((arg) => JSON.stringify(arg)).join(',')})`, returnByValue: true, awaitPromise: true }, session);
  if (response.exceptionDetails) throw new Error(response.exceptionDetails.exception?.description || response.exceptionDetails.text);
  return response.result.value;
}
async function goto(url, heading) {
  await send('Page.navigate', { url: `http://127.0.0.1:5175${url}` }, session);
  await until(() => evaluate((heading) => document.querySelector('h1')?.textContent === heading, heading), heading);
}
async function click(text, selector = 'button,a') {
  await evaluate((text, selector) => { const element = [...document.querySelectorAll(selector)].find((element) => element.textContent.trim() === text && element.getClientRects().length); if (!element) throw new Error(`Cannot find ${text}`); element.click(); }, text, selector);
  await pause(200);
}
async function fill(label, value) {
  await evaluate((label, value) => {
    const labelElement = [...document.querySelectorAll('label')].find((element) => element.textContent === label);
    const input = document.getElementById(labelElement?.htmlFor);
    if (!input) throw new Error(`Cannot find ${label}`);
    const prototype = input.tagName === 'SELECT' ? HTMLSelectElement.prototype : input.tagName === 'TEXTAREA' ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
    Object.getOwnPropertyDescriptor(prototype, 'value').set.call(input, value);
    input.dispatchEvent(new Event('input', { bubbles: true })); input.dispatchEvent(new Event('change', { bubbles: true }));
  }, label, String(value)); await pause(75);
}
async function screenshot(name) { const { data } = await send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false }, session); fs.writeFileSync(path.join(output, `${name}.png`), Buffer.from(data, 'base64')); }
const passed = (message) => console.log(`PASS ${message}`);
try {
  await until(() => serverOutput.includes('Local:'), 'Vite server');
  browser = spawn(process.env.CHROME_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe', ['--headless=new', '--no-sandbox', '--remote-debugging-pipe', '--no-first-run', '--no-default-browser-check', '--disable-gpu', '--disable-background-networking', '--disable-extensions', `--user-data-dir=${path.join(output, `chrome-${Date.now()}`)}`, 'about:blank'], { windowsHide: true, stdio: ['ignore', 'ignore', 'pipe', 'pipe', 'pipe'] });
  browser.stderr.on('data', (chunk) => { browserErrors += chunk; });
  let buffer = '';
  browser.stdio[4].on('data', (chunk) => {
    buffer += chunk.toString(); let delimiter;
    while ((delimiter = buffer.indexOf('\0')) >= 0) {
      const message = JSON.parse(buffer.slice(0, delimiter)); buffer = buffer.slice(delimiter + 1);
      const callback = pending.get(message.id);
      if (callback) { clearTimeout(callback.timer); pending.delete(message.id); message.error ? callback.reject(new Error(JSON.stringify(message.error))) : callback.resolve(message.result); }
      else events.push(message);
    }
  });
  const target = await send('Target.createTarget', { url: 'about:blank' });
  session = (await send('Target.attachToTarget', { targetId: target.targetId, flatten: true })).sessionId;
  await send('Page.enable', {}, session); await send('Runtime.enable', {}, session);
  await send('Emulation.setDeviceMetricsOverride', { width: 1280, height: 820, deviceScaleFactor: 1, mobile: false }, session);
  await send('Browser.setDownloadBehavior', { behavior: 'allow', downloadPath: output, eventsEnabled: true });
  await goto('/', 'Sign in'); assert.equal(requests.length, 0); passed('Unauthenticated visitors see sign-in without sample records or business requests');
  await screenshot('sign-in-desktop');
  await fill('Email', 'invalid@example.com'); await fill('Password', 'invalid-password'); await click('Sign in', 'form button');
  assert.ok(await evaluate(() => document.body.textContent.includes('Invalid email or password'))); passed('Sign-in displays API validation errors');
  await fill('Email', 'owner@example.com'); await click('Sign in', 'form button');
  await until(() => evaluate(() => document.querySelector('h1')?.textContent === 'Dashboard'), 'authenticated dashboard');
  assert.equal(await evaluate(() => getComputedStyle(document.querySelector('.side')).backgroundColor), 'rgb(16, 41, 43)');
  assert.ok(await evaluate(() => document.body.textContent.includes('Test Owner')));
  assert.ok(requests.filter((request) => !request.pathname.startsWith('/auth/')).every((request) => request.authorization === 'Bearer test-access'));
  await evaluate(() => document.fonts.ready.then(() => true)); await screenshot('dashboard-desktop'); passed('Authenticated API loading and supplied design palette');
  await goto('/invoices', 'Invoices'); await click('Overdue', '.filter-buttons button');
  assert.equal(await evaluate(() => document.querySelectorAll('tbody tr').length), 2);
  await fill('Search', 'Bluepeak'); assert.equal(await evaluate(() => document.querySelectorAll('tbody tr').length), 1);
  await fill('Search', 'missing'); assert.ok(await evaluate(() => document.body.textContent.includes('No results found'))); passed('Invoice search, status filters and empty states');
  await goto('/customers', 'Customers'); await click('Add customer');
  await fill('Customer name', 'Browser Test Customer'); await fill('Email', 'browser@example.com'); await fill('Phone', '9876543210'); await click('Save customer');
  await fill('Search', 'Browser Test Customer'); assert.equal(await evaluate(() => document.querySelectorAll('tbody tr').length), 1);
  assert.ok(requests.some((request) => request.pathname === '/customers' && request.method === 'POST' && request.body.name === 'Browser Test Customer'));
  await click('Browser Test Customer'); await click('Edit'); await fill('Customer name', 'Updated Test Customer'); await click('Save customer');
  await until(() => evaluate(() => document.querySelector('h1')?.textContent === 'Updated Test Customer'), 'updated customer'); passed('Customer creation and editing persist through API');
  await click('New invoice'); await fill('Description', 'Frontend development'); await fill('Quantity', 2); await fill('Unit price (₹)', 1000); await fill('Discount (₹)', 100);
  assert.ok(await evaluate(() => document.querySelector('.invoice-summary').textContent.includes('₹2,242')));
  await click('Save draft'); await until(() => evaluate(() => document.querySelector('h1')?.textContent === 'Draft invoice'), 'draft');
  const invoiceUrl = await evaluate(() => location.pathname);
  await click('Edit draft'); await click('Issue invoice'); await until(() => evaluate(() => document.querySelector('h1')?.textContent.startsWith('Invoice INV/')), 'issued invoice');
  const invoiceHeading = await evaluate(() => document.querySelector('h1').textContent);
  assert.equal(await evaluate(() => location.pathname), invoiceUrl);
  assert.equal(requests.filter((request) => request.pathname === '/invoices' && request.method === 'POST').length, 1); passed('GST preview, draft saving and issuing through real API contracts');
  await fill('Amount (₹)', 1000); await click('Record payment', 'form button');
  await until(() => evaluate(() => document.querySelector('.invoice-paper .chip')?.textContent === 'Partly paid'), 'partial payment');
  await fill('Amount (₹)', 1242); await click('Record payment', 'form button');
  await until(() => evaluate(() => document.querySelector('.invoice-paper .chip')?.textContent === 'Paid'), 'full payment');
  await goto('/payments', 'Payments'); await fill('Search', 'Updated Test Customer'); assert.equal(await evaluate(() => document.querySelectorAll('tbody tr').length), 2); passed('Payments update balance, status and API payment history');
  await goto(invoiceUrl, invoiceHeading); await click('Download PDF');
  await until(() => events.some((event) => event.method === 'Browser.downloadProgress' && event.params.state === 'completed'), 'PDF download');
  assert.ok(requests.some((request) => request.pathname.endsWith('/pdf'))); passed('Invoice PDF is downloaded from backend');
  await goto('/reports', 'Reports'); await click('Export CSV'); await click('Export Excel');
  await until(() => fs.existsSync(path.join(output, 'invoices.csv')) && fs.existsSync(path.join(output, 'invoices.xlsx')), 'report downloads');
  await fill('Report', 'tax'); assert.ok(await evaluate(() => !document.body.textContent.includes('Top customers'))); passed('API reports, report selection and backend CSV/Excel downloads');
  await goto('/settings', 'Settings'); await fill('Business name', 'Updated Business'); await fill('Default due days', 30); await click('Save changes');
  await goto('/settings', 'Settings'); assert.equal(await evaluate(() => document.querySelector('input').value), 'Updated Business');
  await click('Invite'); await fill('Name', 'Test Accountant'); await fill('Email', 'accountant@example.com'); await click('Invite', 'dialog button');
  assert.ok(await evaluate(() => document.querySelector('table').textContent.includes('Test Accountant'))); passed('Business profile and team changes save to backend');
  await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true }, session);
  for (const [url, heading] of [['/', 'Dashboard'], ['/invoices', 'Invoices'], ['/customers', 'Customers'], ['/reports', 'Reports'], ['/invoices/new', 'New invoice'], ['/invoices/i115', 'Invoice INV/2026-27/0115'], ['/settings', 'Settings']]) {
    await goto(url, heading); assert.ok(await evaluate(() => document.documentElement.scrollWidth <= window.innerWidth + 1), `Overflow on ${url}`); await screenshot(`mobile-${url.split('/')[1] || 'dashboard'}`);
  }
  await goto('/', 'Dashboard'); await evaluate(() => document.querySelector('.menu-toggle').click()); await click('Invoices', 'nav a'); assert.ok(await evaluate(() => !document.querySelector('.side').classList.contains('menu-open'))); passed('Mobile layouts and navigation');
  tokenExpired = true; await goto('/', 'Dashboard'); assert.ok(requests.some((request) => request.pathname === '/auth/refresh')); passed('Expired access tokens refresh through API');
  failData = true; await send('Page.reload', {}, session); await until(() => evaluate(() => document.body.textContent.includes('Billing service unavailable')), 'API failure');
  assert.equal(await evaluate(() => document.querySelectorAll('.g4 .card').length), 0); failData = false; await click('Retry'); await until(() => evaluate(() => document.querySelector('h1')?.textContent === 'Dashboard'), 'retry'); passed('API failures show an error with no sample fallback and retry works');
  await evaluate(() => document.querySelector('.menu-toggle').click()); await click('Sign out'); await until(() => evaluate(() => document.querySelector('h1')?.textContent === 'Sign in'), 'logout');
  assert.equal(await evaluate(() => sessionStorage.getItem('invoice-platform-session')), null); passed('Logout clears session and protects routes');
  const errors = events.filter((event) => event.method === 'Runtime.exceptionThrown'); assert.deepEqual(errors.map((event) => event.params.exceptionDetails.text), []);
  console.log('All browser checks passed. Screenshots saved to test-results.');
} catch (error) {
  if (browser && session) { await screenshot('failure').catch(() => {}); console.error(await evaluate(() => document.body.innerText).catch(() => '')); }
  console.error(error); if (browserErrors) console.error(browserErrors.slice(-2000)); process.exitCode = 1;
} finally {
  if (browser) { await send('Browser.close').catch(() => {}); browser.kill(); }
  server.kill(); api.close();
  for (const callback of pending.values()) { clearTimeout(callback.timer); callback.reject(new Error('Browser closed')); }
}
