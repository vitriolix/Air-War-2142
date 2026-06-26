#!/usr/bin/env bash
# Boot the web build and stream its BROWSER console + errors to this terminal.
# (The web app is client-side — its logs live in the browser, not a server.)
# Usage: web-console.sh [js|wasm] [--headed]
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

TARGET="js"; HEADED=""
for a in "$@"; do
  case "$a" in
    js|web)       TARGET="js" ;;
    wasm|wasmjs)  TARGET="wasm" ;;
    --headed)     HEADED="--headed" ;;
  esac
done

if [ "$TARGET" = "wasm" ]; then
  RUN_TASK="wasmJsBrowserDevelopmentRun"; LOG="/tmp/clone1942-wasm.log"
else
  RUN_TASK="jsBrowserDevelopmentRun";     LOG="/tmp/clone1942-web.log"
fi

TOOL_DIR="$REPO_ROOT/scripts/web-console"

# One-time: install the Playwright streamer deps (reuses the cached browser binary).
if [ ! -d "$TOOL_DIR/node_modules/playwright" ]; then
  info "Installing console-streamer deps (one-time)…"
  ( cd "$TOOL_DIR" && PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 npm install --silent ) || fail "npm install failed in $TOOL_DIR"
  ( cd "$TOOL_DIR" && npx --yes playwright install chromium >/dev/null 2>&1 ) || warn "Could not verify chromium; it may download on first run."
fi

info "Starting $TARGET dev server…"
# This is a Playwright-driven console/capture path, so stop the dev server auto-opening
# the system default browser (it'd collide with our Chrome). See webpack.config.d.
export AIR_WAR_2142_NO_OPEN=1
$GRADLE "$RUN_TASK" --continuous > "$LOG" 2>&1 &
SERVER_PID=$!
cleanup() {
  info "Stopping dev server…"
  kill "$SERVER_PID" 2>/dev/null || true
  pkill -9 -f "$RUN_TASK" 2>/dev/null || true
  pkill -9 -f "webpack" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

info "Waiting for the dev server to compile…"
URL=""
for _ in $(seq 1 180); do
  # -a: the dev-server log carries ANSI/control bytes, so grep would otherwise print
  # "Binary file … matches" and that string would end up used as the URL.
  URL="$(grep -aoE 'http://localhost:[0-9]+' "$LOG" 2>/dev/null | head -1 || true)"
  if grep -aq "compiled" "$LOG" 2>/dev/null && [ -n "$URL" ]; then break; fi
  grep -aq "BUILD FAILED" "$LOG" 2>/dev/null && { tail -20 "$LOG"; fail "Build failed (see $LOG)."; }
  sleep 1
done
[ -n "$URL" ] || { tail -20 "$LOG"; fail "Dev server did not start (see $LOG)."; }

ok "Streaming browser console from $URL"
node "$TOOL_DIR/stream.js" "$URL" $HEADED
