// Attach a headless browser to the running web build and stream its
// console + errors to stdout until killed.  Usage: node stream.js <url> [--headed]
const { chromium } = require('playwright');
const url = process.argv[2];
const headed = process.argv.includes('--headed');
const ts = () => new Date().toISOString().slice(11, 23);

(async () => {
  const browser = await chromium.launch({ headless: !headed });
  const page = await browser.newPage();
  page.on('console', m => console.log(`${ts()} [${m.type()}] ${m.text()}`));
  page.on('pageerror', e => console.log(`${ts()} [pageerror] ${e.stack || e.message}`));
  page.on('requestfailed', r => console.log(`${ts()} [reqfailed] ${r.url()} — ${r.failure()?.errorText || ''}`));
  await page.goto(url, { waitUntil: 'load' });
  console.log(`${ts()} # attached to ${url} — streaming browser console (Ctrl-C to stop)`);
  await new Promise(() => {}); // keep the page alive and streaming
})().catch(e => { console.error('stream FAIL:', e); process.exit(1); });
