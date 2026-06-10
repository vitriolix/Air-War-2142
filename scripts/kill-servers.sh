#!/usr/bin/env bash
# Kill the JS/Wasm dev (webpack) servers and any running game process.
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

info "Stopping web dev servers (JS/Wasm webpack) and runJvm…"
pkill -9 -f "jsBrowserDevelopmentRun"      2>/dev/null || true
pkill -9 -f "wasmJsBrowserDevelopmentRun"  2>/dev/null || true
pkill -9 -f "webpack-dev-server"           2>/dev/null || true
pkill -9 -f "webpack/bin/webpack"          2>/dev/null || true
pkill -9 -f "GradleWrapperMain.*BrowserDevelopmentRun" 2>/dev/null || true
pkill -9 -f "runJvm"                       2>/dev/null || true
rm -f /tmp/clone1942-web.pid /tmp/clone1942-wasm.pid /tmp/clone1942-web.port 2>/dev/null || true

# (Intentionally not killing unrelated Gradle daemons.)
ok "Dev servers stopped."
