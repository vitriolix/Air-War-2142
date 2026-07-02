#!/usr/bin/env bash
# Sketchfab OAuth2 (Authorization Code grant) helpers for automated model downloads.
# Sourced by download-models.sh; expects _common.sh already sourced (REPO_ROOT, can_prompt,
# prompt_line, info/ok/warn).
#
# Config lives in local.properties (gitignored, machine-local — same file Android/Gradle
# already use for sdk.dir), namespaced by site so a future second auth-requiring model
# source (Thingiverse or similar) can live alongside this without collision:
#   sketchfab.clientId=...
#   sketchfab.clientSecret=...
#   sketchfab.refreshToken=...   # set after the first successful browser consent
#
# Why OAuth2 Authorization Code (not a static API key, not username/password): Sketchfab's
# Download API (an extension of their Data API) requires a Bearer OAuth2 access token —
# there's no long-lived personal token for it. Of Sketchfab's three grant types, Resource
# Owner Password Credentials would mean typing your real password into this script (Sketchfab
# itself documents that grant as "less secure"); Authorization Code means a one-time browser
# consent click instead — your password never touches this script or terminal.
#
# One-time setup (only you can do this, once): Sketchfab removed the self-service app
# dashboard — registering an app is now a support request (see
# https://sketchfab.com/developers/oauth#registering-your-app), with 4 fields: Application
# name (shown on the consent screen the user — you — sees when authorizing), Grant type
# (Authorization Code — matches this file), Redirect URI (must be exactly
# http://127.0.0.1:8737/callback, matching SKETCHFAB_OAUTH_PORT below), and Username (shown
# as the app's "author" in your own Connected Apps list later — cosmetic only).
#
# `_common.sh` runs under `set -euo pipefail`, which applies to this whole sourced file too.
# grep/jq legitimately exit non-zero for "no match"/"key not set yet" — the *normal*, expected
# case on a first run, not an error — but under `set -e` a plain assignment (not an `if`/`||`
# guarded one) that pipes through a failing grep/jq silently kills the entire script with NO
# error message. Every such pipeline below is deliberately terminated with `|| true` so the
# (correct, already-handled-by-the-caller) empty result comes through instead of a silent death.

SKETCHFAB_OAUTH_PORT=8737
SKETCHFAB_REDIRECT_URI="http://127.0.0.1:${SKETCHFAB_OAUTH_PORT}/callback"
SKETCHFAB_LOCAL_PROPS="$REPO_ROOT/local.properties"

sketchfab_deps_ok() {
  command -v jq >/dev/null 2>&1 && command -v node >/dev/null 2>&1 && command -v unzip >/dev/null 2>&1
}

sketchfab_prop() {
  [ -f "$SKETCHFAB_LOCAL_PROPS" ] || return 0
  grep -E "^$1=" "$SKETCHFAB_LOCAL_PROPS" 2>/dev/null | tail -1 | cut -d= -f2- || true
}

sketchfab_set_prop() {
  local key="$1" value="$2"
  touch "$SKETCHFAB_LOCAL_PROPS" || { warn "Couldn't write to $SKETCHFAB_LOCAL_PROPS"; return 1; }
  if grep -qE "^$key=" "$SKETCHFAB_LOCAL_PROPS" 2>/dev/null; then
    grep -vE "^$key=" "$SKETCHFAB_LOCAL_PROPS" > "$SKETCHFAB_LOCAL_PROPS.tmp" || true
    mv "$SKETCHFAB_LOCAL_PROPS.tmp" "$SKETCHFAB_LOCAL_PROPS" || { warn "Couldn't update $SKETCHFAB_LOCAL_PROPS"; return 1; }
  fi
  echo "$key=$value" >> "$SKETCHFAB_LOCAL_PROPS"
}

# jq-extract field $2 (a jq filter with a `// empty` fallback already baked in by the caller)
# from JSON $1, tolerating empty/invalid input (e.g. a failed curl) as "" rather than dying.
sketchfab_jq() {
  echo "$1" | jq -r "$2" 2>/dev/null || true
}

# Ensures sketchfab.clientId/clientSecret are configured, prompting + saving to
# local.properties the first time (only if we can prompt). Returns 1 if not configured and
# we can't/didn't set them up — caller should fall back to manual-download instructions.
sketchfab_ensure_app_credentials() {
  local client_id client_secret
  client_id="$(sketchfab_prop sketchfab.clientId)"
  client_secret="$(sketchfab_prop sketchfab.clientSecret)"
  if [ -n "$client_id" ] && [ -n "$client_secret" ]; then
    return 0
  fi
  can_prompt || return 1

  echo ""
  echo "── Sketchfab automated download: one-time setup ──────────────────────────"
  echo "No Sketchfab OAuth app configured yet. Sketchfab's app dashboard is gone —"
  echo "registering one is now a support request:"
  echo "  1. See https://sketchfab.com/developers/oauth#registering-your-app and"
  echo "     contact Sketchfab support with these 4 fields:"
  echo "       Application name: (your choice — shown on the consent screen)"
  echo "       Grant type:       Authorization Code"
  echo "       Redirect URI:     $SKETCHFAB_REDIRECT_URI"
  echo "       Username:         (your choice — shown as the app's author, cosmetic)"
  echo "  2. Once approved, enter the Client ID and Client Secret they send you below."
  echo "(Saved to local.properties — gitignored, never committed.)"
  echo ""
  client_id="$(prompt_line "Sketchfab Client ID (blank to skip, use manual download instead):")"
  [ -z "$client_id" ] && return 1
  client_secret="$(prompt_line "Sketchfab Client Secret:")"
  [ -z "$client_secret" ] && return 1

  sketchfab_set_prop "sketchfab.clientId" "$client_id"
  sketchfab_set_prop "sketchfab.clientSecret" "$client_secret"
  ok "Saved Sketchfab app credentials to local.properties."
}

# Runs the browser-based Authorization Code consent flow once, saves the resulting refresh
# token. Returns 1 if it doesn't complete — caller should fall back to manual download.
sketchfab_authorize() {
  local client_id client_secret state auth_url cb_out code got_state
  client_id="$(sketchfab_prop sketchfab.clientId)"
  client_secret="$(sketchfab_prop sketchfab.clientSecret)"
  can_prompt || return 1

  state="$(LC_ALL=C tr -dc 'a-zA-Z0-9' </dev/urandom 2>/dev/null | head -c 24 || true)"
  [ -n "$state" ] || state="$$-$RANDOM-$RANDOM"
  auth_url="https://sketchfab.com/oauth2/authorize/?response_type=code&client_id=${client_id}&redirect_uri=${SKETCHFAB_REDIRECT_URI}&state=${state}"

  echo ""
  info "Opening your browser for one-time Sketchfab authorization…"
  echo "If it doesn't open automatically, visit:"
  echo "  $auth_url"
  echo ""
  open_url "$auth_url" || warn "Couldn't auto-open a browser — open the URL above manually."

  cb_out="$(node "$REPO_ROOT/scripts/sketchfab-oauth-callback.js" "$SKETCHFAB_OAUTH_PORT")" || {
    warn "Sketchfab authorization didn't complete (timed out, denied, or the redirect URI doesn't match your app's config)."
    return 1
  }
  code="$(printf '%s\n' "$cb_out" | grep '^CODE=' | cut -d= -f2- || true)"
  got_state="$(printf '%s\n' "$cb_out" | grep '^STATE=' | cut -d= -f2- || true)"
  if [ -z "$code" ] || [ "$got_state" != "$state" ]; then
    warn "Sketchfab authorization failed or state mismatch — aborting for safety."
    return 1
  fi

  local resp refresh_token
  resp="$(curl -s -X POST https://sketchfab.com/oauth2/token/ \
    --data-urlencode "grant_type=authorization_code" \
    --data-urlencode "code=$code" \
    --data-urlencode "client_id=$client_id" \
    --data-urlencode "client_secret=$client_secret" \
    --data-urlencode "redirect_uri=$SKETCHFAB_REDIRECT_URI" || true)"
  refresh_token="$(sketchfab_jq "$resp" '.refresh_token // empty')"
  if [ -z "$refresh_token" ]; then
    warn "Sketchfab token exchange failed: $(sketchfab_jq "$resp" '.error_description // .error // "unknown error"')"
    return 1
  fi
  sketchfab_set_prop "sketchfab.refreshToken" "$refresh_token"
  ok "Sketchfab authorization complete."
}

# Internal: POST grant_type=refresh_token, echo the resulting access_token (empty = failed).
# Side outputs: $SKETCHFAB_REFRESH_ERROR (error detail on failure), $SKETCHFAB_NEW_REFRESH_TOKEN
# (Sketchfab may rotate the refresh token on each use — empty if unchanged/absent).
#
# Sets $SKETCHFAB_ACCESS_TOKEN_OUT instead of echoing a return value — deliberately NOT
# called via `x="$(sketchfab_do_refresh …)"`. Command substitution forks a SUBSHELL; that
# isolates more than just stdout — ANY variable this function sets (including the
# SKETCHFAB_REFRESH_ERROR/SKETCHFAB_NEW_REFRESH_TOKEN globals below) only exists inside that
# subshell and vanishes once it exits, leaving them completely unset in the caller. Under
# `set -u` (this repo's `_common.sh`), the caller then dies with "unbound variable" the
# moment it reads one — which is exactly what happened here on the very first real end-to-end
# run. The original version of this function ALSO documented (correctly!) that
# sketchfab_access_token must avoid `$(...)` for the same reason — this function just didn't
# get the same fix applied when it was added afterward. Call as a plain statement.
sketchfab_do_refresh() {
  local client_id="$1" client_secret="$2" refresh_token="$3" resp
  resp="$(curl -s -X POST https://sketchfab.com/oauth2/token/ \
    --data-urlencode "grant_type=refresh_token" \
    --data-urlencode "client_id=$client_id" \
    --data-urlencode "client_secret=$client_secret" \
    --data-urlencode "refresh_token=$refresh_token" || true)"
  SKETCHFAB_REFRESH_ERROR="$(sketchfab_jq "$resp" '.error_description // .error // "unknown error"')"
  SKETCHFAB_NEW_REFRESH_TOKEN="$(sketchfab_jq "$resp" '.refresh_token // empty')"
  SKETCHFAB_ACCESS_TOKEN_OUT="$(sketchfab_jq "$resp" '.access_token // empty')"
}

# Echoes a fresh access token (minting via the stored refresh token, running the full
# browser consent flow first if none exists yet, or if the stored refresh token turns out to
# be dead — revoked/expired). Returns 1 / echoes nothing if automated download isn't
# available — callers should fall back to manual instructions.
#
# Sets $SKETCHFAB_ACCESS_TOKEN on success — deliberately NOT an `echo`-as-return-value
# function (i.e. don't call this via `x="$(sketchfab_access_token)"`). Its call chain
# (sketchfab_ensure_app_credentials, sketchfab_authorize) prints multi-line interactive
# setup instructions via plain `echo`/info/warn; a command substitution captures ALL of a
# function's stdout, so wrapping this in `$(...)` would silently swallow that entire
# instructional block into the "token" value instead of ever showing it on screen — bit us
# once already (see tooling-gradle-native-wip memory).
sketchfab_access_token() {
  SKETCHFAB_ACCESS_TOKEN=""
  sketchfab_deps_ok || { warn "Automated Sketchfab download needs jq, node, and unzip — falling back to manual download."; return 1; }
  sketchfab_ensure_app_credentials || return 1

  local client_id client_secret refresh_token access_token
  client_id="$(sketchfab_prop sketchfab.clientId)"
  client_secret="$(sketchfab_prop sketchfab.clientSecret)"
  refresh_token="$(sketchfab_prop sketchfab.refreshToken)"

  if [ -z "$refresh_token" ]; then
    sketchfab_authorize || return 1
    refresh_token="$(sketchfab_prop sketchfab.refreshToken)"
  fi

  sketchfab_do_refresh "$client_id" "$client_secret" "$refresh_token"
  access_token="$SKETCHFAB_ACCESS_TOKEN_OUT"
  if [ -z "$access_token" ]; then
    # The stored refresh token is dead (revoked/expired/invalid), not just "not set yet" —
    # clear it and, if we can prompt, re-run the one-time browser consent to get a fresh
    # one, rather than silently falling back to manual downloads forever until someone
    # notices and clears local.properties by hand.
    warn "Sketchfab token refresh failed: $SKETCHFAB_REFRESH_ERROR"
    sketchfab_set_prop "sketchfab.refreshToken" ""
    if can_prompt; then
      warn "Re-authorizing…"
      sketchfab_authorize || return 1
      refresh_token="$(sketchfab_prop sketchfab.refreshToken)"
      sketchfab_do_refresh "$client_id" "$client_secret" "$refresh_token"
      access_token="$SKETCHFAB_ACCESS_TOKEN_OUT"
    fi
  fi
  if [ -z "$access_token" ]; then
    warn "Falling back to manual download."
    return 1
  fi

  # Some OAuth servers rotate the refresh token on each use — persist if it changed.
  if [ -n "$SKETCHFAB_NEW_REFRESH_TOKEN" ] && [ "$SKETCHFAB_NEW_REFRESH_TOKEN" != "$refresh_token" ]; then
    sketchfab_set_prop "sketchfab.refreshToken" "$SKETCHFAB_NEW_REFRESH_TOKEN"
  fi

  SKETCHFAB_ACCESS_TOKEN="$access_token"
}

# Downloads model $1 (sketchfab id) into directory $2 via the Download API. The API only
# ever returns glTF (as a ZIP: scene.gltf + scene.bin + textures) or USDZ — never a .glb —
# so this unpacks the ZIP rather than saving a single file. Returns 1 on any failure; caller
# should fall back to manual instructions.
sketchfab_download_model() {
  local model_id="$1" save_dir="$2" access_token resp gltf_url tmp_dir zip_path
  sketchfab_access_token || return 1
  access_token="$SKETCHFAB_ACCESS_TOKEN"
  [ -n "$access_token" ] || return 1

  resp="$(curl -s "https://api.sketchfab.com/v3/models/${model_id}/download" \
    -H "Authorization: Bearer ${access_token}" || true)"
  gltf_url="$(sketchfab_jq "$resp" '.gltf.url // empty')"
  if [ -z "$gltf_url" ]; then
    warn "Sketchfab download request failed: $(sketchfab_jq "$resp" '.detail // .error // "unknown error"')"
    return 1
  fi

  mkdir -p "$save_dir"
  tmp_dir="$(mktemp -d)"
  zip_path="$tmp_dir/model.zip"
  info "Downloading glTF archive…"
  if ! curl -sL "$gltf_url" -o "$zip_path"; then
    warn "Download failed."; rm -rf "$tmp_dir"; return 1
  fi
  if ! unzip -oq "$zip_path" -d "$save_dir"; then
    warn "Failed to extract archive."; rm -rf "$tmp_dir"; return 1
  fi
  rm -rf "$tmp_dir"
  sketchfab_write_credit "$model_id" "$save_dir"
  ok "Saved to $save_dir (scene.gltf + scene.bin + textures)."
}

# Writes CREDIT.md into $2 with attribution for model $1 — creator, license, and a link to
# the model's own page — using Sketchfab's suggested credit format. Credits every model
# regardless of whether its license legally requires it (project policy: credit everyone we
# can). GET /v3/models/{uid} is public (no auth needed — verified). Best-effort: a failure
# here doesn't fail the download, since the model itself already saved successfully.
sketchfab_write_credit() {
  local model_id="$1" save_dir="$2" resp model_name model_url creator creator_url license_name license_url license_reqs
  resp="$(curl -s "https://api.sketchfab.com/v3/models/${model_id}" || true)"
  model_name="$(sketchfab_jq "$resp" '.name // empty')"
  model_url="$(sketchfab_jq "$resp" '.viewerUrl // empty')"
  creator="$(sketchfab_jq "$resp" '.user.displayName // empty')"
  creator_url="$(sketchfab_jq "$resp" '.user.profileUrl // empty')"
  license_name="$(sketchfab_jq "$resp" '.license.fullName // empty')"
  license_url="$(sketchfab_jq "$resp" '.license.url // empty')"
  license_reqs="$(sketchfab_jq "$resp" '.license.requirements // empty')"
  [ -n "$model_name" ] || model_name="(unknown title)"
  [ -n "$model_url" ] || model_url="https://sketchfab.com/3d-models/${model_id}"
  [ -n "$creator" ] || creator="(unknown creator)"
  [ -n "$license_name" ] || license_name="(unknown license)"

  {
    echo "# Attribution — $model_name"
    echo ""
    echo "This work is based on [$model_name]($model_url) by [$creator](${creator_url:-$model_url}) licensed under [$license_name](${license_url:-$model_url})."
    echo ""
    echo "- Model page: $model_url"
    echo "- Creator: $creator${creator_url:+ ($creator_url)}"
    echo "- License: $license_name${license_url:+ ($license_url)}${license_reqs:+ — $license_reqs}"
    echo "- Downloaded via Sketchfab Download API on $(date -u +%Y-%m-%d)"
  } > "$save_dir/CREDIT.md"
}
