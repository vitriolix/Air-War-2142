#!/usr/bin/env bash
# Make sure git is tidy: a clean working tree, on a branch, and report push state.
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"
require_git_repo

info "Checking working tree…"
if [ -n "$(git status --porcelain)" ]; then
  git status --short
  fail "Working tree is not clean — commit or stash your changes first."
fi
ok "Working tree is clean."

branch="$(git rev-parse --abbrev-ref HEAD)"
info "On branch: $branch"

if git rev-parse --abbrev-ref --symbolic-full-name '@{u}' >/dev/null 2>&1; then
  ahead="$(git rev-list --count '@{u}..HEAD')"
  behind="$(git rev-list --count 'HEAD..@{u}')"
  if [ "$ahead" -gt 0 ]; then warn "$ahead local commit(s) not pushed."; fi
  if [ "$behind" -gt 0 ]; then warn "$behind commit(s) behind upstream (consider pulling)."; fi
  [ "$ahead" -eq 0 ] && [ "$behind" -eq 0 ] && ok "In sync with upstream."
else
  warn "No upstream tracking branch set for '$branch'."
fi
