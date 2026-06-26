#!/usr/bin/env bash
# Launch the game on a platform to play with.
# Usage: play.sh <jvm|web|wasm|android|native> [--headless]
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

PLATFORM="${1:-}"; shift || true
HEADLESS=0
for a in "$@"; do [ "$a" = "--headless" ] && HEADLESS=1; done

# Serve a browser target: serve_browser <gradleRunTask> <label> <logfile>
serve_browser() {
  local task="$1" label="$2" log="$3"
  info "Building & serving the $label app…"
  # Headless = our Playwright capture path: tell the dev server not to auto-open the
  # system default browser (it'd collide with Chrome for Testing). See webpack.config.d.
  [ "$HEADLESS" -eq 1 ] && export AIR_WAR_2142_NO_OPEN=1
  $GRADLE "$task" --continuous > "$log" 2>&1 &
  echo $! > "${log%.log}.pid"
  info "Waiting for the dev server to compile…"
  local url=""
  for _ in $(seq 1 180); do
    # -a: the dev-server log carries ANSI/control bytes, so grep would otherwise print
    # "Binary file … matches" and that string would end up used as the URL.
    url="$(grep -aoE 'http://localhost:[0-9]+' "$log" 2>/dev/null | head -1 || true)"
    if grep -aq "compiled" "$log" 2>/dev/null && [ -n "$url" ]; then break; fi
    if grep -aq "BUILD FAILED" "$log" 2>/dev/null; then tail -20 "$log"; fail "$label build failed (see $log)."; fi
    sleep 1
  done
  [ -n "$url" ] || { tail -20 "$log"; fail "Dev server did not start (see $log)."; }
  if [ "$HEADLESS" -eq 1 ]; then
    ok "Serving (headless, no browser opened) at $url"
  else
    # The dev server opens the browser itself (webpack auto-open; see webpack.config.d).
    # Don't also `open` here or interactive playWeb pops two windows.
    ok "Serving at $url — the dev server is opening your browser…"
  fi
  info "Dev server left running. Stop it with: ./gradlew killServers"
}

case "$PLATFORM" in
  jvm|java|desktop)
    [ "$HEADLESS" -eq 1 ] && warn "Desktop has no headless mode; opening a window."
    info "Launching JVM desktop (runJvm)…"
    exec $GRADLE runJvm
    ;;

  native)
    fail "No Kotlin/Native desktop target is configured — the desktop build runs on the JVM. Use: ./gradlew playJvm
      (To add a true native desktop target later: korge { targetDesktop() }.)"
    ;;

  web|js)
    serve_browser jsBrowserDevelopmentRun "web (JS)" /tmp/clone1942-web.log
    ;;

  wasm|wasmjs)
    serve_browser wasmJsBrowserDevelopmentRun "web (Wasm)" /tmp/clone1942-wasm.log
    ;;

  android)
    [ "$HEADLESS" -eq 1 ] && warn "Headless Android isn't handled here; using the attached device/emulator."
    command -v adb >/dev/null 2>&1 || fail "adb not found."
    if [ -z "$(adb devices | sed '1d' | awk '$2=="device"{print $1}')" ]; then
      fail "No running device/emulator. Start an emulator (Android Studio) or plug in a device, then retry."
    fi
    info "Installing the debug build…"
    $GRADLE installDebug
    info "Launching on device…"
    adb shell am start -n com.example.clone1942/.MainActivity >/dev/null
    ok "Launched on the Android device/emulator."
    ;;

  *)
    fail "Usage: play.sh <jvm|web|wasm|android|native> [--headless]"
    ;;
esac
