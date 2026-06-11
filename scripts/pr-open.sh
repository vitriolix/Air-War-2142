#!/usr/bin/env bash
# Open a GitHub pull request's page in the browser.
#   ./scripts/pr-open.sh           → the PR for the current branch
#   ./scripts/pr-open.sh 2         → PR #2
#   ./scripts/pr-open.sh my-branch → the PR for that branch
# Any arg is passed through to `gh pr view` (a number, URL, or branch name).
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"
require_git_repo
command -v gh >/dev/null 2>&1 || fail "GitHub CLI 'gh' not found (brew install gh; gh auth login)."
git remote get-url origin >/dev/null 2>&1 || fail "No 'origin' remote. Add one: git remote add origin <url>"

target="${1:-$(git rev-parse --abbrev-ref HEAD)}"
info "Opening PR '$target' in your browser…"
gh pr view "$target" --web || fail "No pull request found for '$target'."
ok "Opened."
