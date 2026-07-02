#!/usr/bin/env node
// One-shot local HTTP listener for the Sketchfab OAuth2 Authorization Code redirect.
// Usage: node sketchfab-oauth-callback.js <port>
// Listens on 127.0.0.1:<port>, waits for the browser to redirect to /callback?code=...&state=...,
// prints CODE=<code> / STATE=<state> / ERROR=<error> as lines on stdout, replies to the browser
// with a "you can close this tab" page, then exits. Exits with TIMEOUT=1 after 5 minutes if the
// user never completes the browser consent step.
const http = require('http');

const port = parseInt(process.argv[2], 10);
if (!port) {
  console.error('Usage: node sketchfab-oauth-callback.js <port>');
  process.exit(2);
}

const server = http.createServer((req, res) => {
  const url = new URL(req.url, `http://127.0.0.1:${port}`);
  if (url.pathname !== '/callback') {
    res.writeHead(404).end();
    return;
  }
  const code = url.searchParams.get('code');
  const state = url.searchParams.get('state');
  const error = url.searchParams.get('error');

  res.writeHead(200, { 'Content-Type': 'text/html' });
  res.end(error
    ? `<html><body>Sketchfab authorization failed: ${error}. You can close this tab.</body></html>`
    : `<html><body>Sketchfab authorization complete — you can close this tab and return to the terminal.</body></html>`);

  if (error) console.log(`ERROR=${error}`);
  if (code) console.log(`CODE=${code}`);
  if (state) console.log(`STATE=${state}`);

  server.close(() => process.exit(error || !code ? 1 : 0));
});

const timeout = setTimeout(() => {
  console.log('TIMEOUT=1');
  server.close(() => process.exit(1));
}, 5 * 60 * 1000);
timeout.unref();

server.listen(port, '127.0.0.1');
