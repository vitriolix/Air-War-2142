#!/usr/bin/env bash
# Shared helpers for the project task scripts. Sourced by the others.
set -euo pipefail

# Repo root = parent of this scripts/ dir, regardless of the caller's cwd.
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

# ./gradlew picks up org.gradle.java.home (JDK 21) pinned in gradle.properties,
# so these scripts work even if the machine default JDK is older.
GRADLE="./gradlew"

# --- pretty output ---
if [ -t 1 ]; then
  C_BLUE='\033[34m'; C_GREEN='\033[32m'; C_YELLOW='\033[33m'; C_RED='\033[31m'; C_OFF='\033[0m'
else
  C_BLUE=''; C_GREEN=''; C_YELLOW=''; C_RED=''; C_OFF=''
fi
# All four print to stderr (matches `fail`, which already did) — keeps stdout clean for
# actual return values, since some callers invoke a function that prints status via
# command substitution (`x="$(some_fn)"`), which captures *only* stdout. A stdout print
# here would otherwise get silently absorbed into that captured value instead of ever
# reaching the screen — this bit the Sketchfab OAuth flow once already.
info() { printf "${C_BLUE}▶ %s${C_OFF}\n" "$*" >&2; }
ok()   { printf "${C_GREEN}✓ %s${C_OFF}\n" "$*" >&2; }
warn() { printf "${C_YELLOW}! %s${C_OFF}\n" "$*" >&2; }
fail() { printf "${C_RED}✗ %s${C_OFF}\n" "$*" >&2; exit 1; }

# Best-effort open $1 in the default browser (macOS `open`, Linux `xdg-open`). Returns 1
# (does nothing) if neither is available — caller should already have printed the URL too.
open_url() {
  { command -v open >/dev/null 2>&1 && open "$1"; } \
    || { command -v xdg-open >/dev/null 2>&1 && xdg-open "$1"; } \
    || return 1
}

is_git_repo()      { git rev-parse --is-inside-work-tree >/dev/null 2>&1; }
require_git_repo() { is_git_repo || fail "Not a git repository yet. Run 'git init' and add a remote first."; }

# Can we prompt interactively? Either a real controlling terminal (/dev/tty — run directly)
# or AIR_WAR_2142_FORCE_PROMPT=1 (set by DownloadModels.kt's --force-prompt, which forwards the
# launching terminal's real stdin through Gradle's client<->daemon protocol — the Gradle
# build daemon itself has no OS-level controlling terminal, `--no-daemon` or not, so /dev/tty
# alone would never detect that path; see tooling-gradle-native-wip memory).
can_prompt() {
  { : >/dev/tty; } 2>/dev/null && return 0
  [ "${AIR_WAR_2142_FORCE_PROMPT:-}" = "1" ]
}

# Prompt with $1, read one line from /dev/tty if available else fd 0 (only meaningful when
# AIR_WAR_2142_FORCE_PROMPT=1 forwarded real input — callers must check can_prompt first). Echoes
# the answer (empty on EOF/no input — `read`'s nonzero EOF exit is swallowed so this doesn't
# abort under `set -e`). Callers use this via command substitution (`x="$(prompt_line …)"`),
# which captures stdout only — the prompt message MUST go to stderr, or it silently gets
# captured into the "answer" instead of ever reaching the screen (this bit us once already).
prompt_line() {
  local msg="$1" answer=""
  echo "$msg" >&2
  if { : >/dev/tty; } 2>/dev/null; then
    read -r answer </dev/tty || answer=""
  else
    read -r answer || answer=""
  fi
  echo "$answer"
}

# App version, read from the korge {} block in composeApp/build.gradle.kts
app_version() { grep -E 'version[[:space:]]*=[[:space:]]*"' composeApp/build.gradle.kts | head -1 | sed -E 's/.*"([^"]+)".*/\1/'; }
