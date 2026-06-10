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
info() { printf "${C_BLUE}▶ %s${C_OFF}\n" "$*"; }
ok()   { printf "${C_GREEN}✓ %s${C_OFF}\n" "$*"; }
warn() { printf "${C_YELLOW}! %s${C_OFF}\n" "$*"; }
fail() { printf "${C_RED}✗ %s${C_OFF}\n" "$*" >&2; exit 1; }

is_git_repo()      { git rev-parse --is-inside-work-tree >/dev/null 2>&1; }
require_git_repo() { is_git_repo || fail "Not a git repository yet. Run 'git init' and add a remote first."; }

# App version, read from the korge {} block in composeApp/build.gradle.kts
app_version() { grep -E 'version[[:space:]]*=[[:space:]]*"' composeApp/build.gradle.kts | head -1 | sed -E 's/.*"([^"]+)".*/\1/'; }
