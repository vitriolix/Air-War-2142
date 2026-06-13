#!/usr/bin/env bash
# Open a GitHub pull request for the current branch.
# Any extra args are passed through to `gh pr create` (e.g. --title "…" --body "…" --draft).
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"
require_git_repo
command -v gh >/dev/null 2>&1 || fail "GitHub CLI 'gh' not found (brew install gh; gh auth login)."
git remote get-url origin >/dev/null 2>&1 || fail "No 'origin' remote. Add one: git remote add origin <url>"

branch="$(git rev-parse --abbrev-ref HEAD)"
if [ "$branch" = "main" ] || [ "$branch" = "master" ]; then
  fail "You're on '$branch'. Create a feature branch first: git switch -c my-feature"
fi
if [ -n "$(git status --porcelain)" ]; then
  fail "Working tree not clean — commit your changes first (see: ./gradlew tidyGit)."
fi

info "Pushing '$branch' to origin…"
git push -u origin "$branch"

info "Creating pull request…"
if [ "$#" -gt 0 ]; then
  gh pr create "$@"
else
  gh pr create --fill
fi
ok "Pull request created."
