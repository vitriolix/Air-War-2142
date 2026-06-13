#!/usr/bin/env bash
# Delete local branches whose upstream remote branch is gone (e.g. squash-merged + auto-deleted) —
# but only after confirming the work is actually merged. "Merged" = the branch is an ancestor of
# the base branch (fast-forward/merge commit) OR it has a merged PR (covers squash/rebase merges,
# which aren't ancestors). Branches that are NOT merged are never deleted silently: you're asked
# what to do (keep / delete anyway / show its commits). With no terminal (CI), unmerged = kept.
# If the branch being deleted is the one currently checked out, it switches to the base branch
# first (you can't delete the branch you're on) — requires a clean working tree.
#
# Usage: prune-branches.sh [--dry-run]   (--dry-run reports, deletes nothing, never prompts)
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"
require_git_repo

dry=0
[ "${1:-}" = "--dry-run" ] && dry=1

# Base branch (merge target), e.g. main — from origin's default, else "main".
base="$(git symbolic-ref --quiet refs/remotes/origin/HEAD 2>/dev/null | sed 's@^refs/remotes/origin/@@')"
base="${base:-main}"

info "Fetching + pruning stale remote-tracking refs…"
git fetch --prune || warn "fetch failed (offline?) — checking from local state only."

# Can we prompt? Probe the controlling terminal once, suppressing the open error if there's none.
tty_ok=0
if { : >/dev/tty; } 2>/dev/null; then tty_ok=1; fi

# Delete a branch, switching to $base first if it's the one currently checked out (you can't
# delete the branch you're on). Refuses to switch a dirty tree. Returns non-zero if it couldn't.
del_branch() {
  local b="$1"
  if [ "$b" = "$(git rev-parse --abbrev-ref HEAD)" ]; then
    if [ -n "$(git status --porcelain)" ]; then
      warn "Working tree not clean — can't switch off '$b' to delete it (commit/stash first)."
      return 1
    fi
    info "Switching to '$base' to delete the current branch '$b'…"
    git switch "$base" >/dev/null 2>&1 || { warn "Could not switch to '$base'."; return 1; }
  fi
  git branch -D "$b" >/dev/null 2>&1
}

# True if branch $1's work is already in $base.
is_merged() {
  git merge-base --is-ancestor "$1" "$base" 2>/dev/null && return 0   # ff / merge commit
  if command -v gh >/dev/null 2>&1; then                              # squash / rebase merge
    local n
    n="$(gh pr list --head "$1" --state merged --json number --jq 'length' 2>/dev/null || echo 0)"
    [ "${n:-0}" -gt 0 ] && return 0
  fi
  return 1
}

gone="$(git branch -vv | awk '/: gone]/ { print ($1=="*") ? $2 : $1 }')"

found=0; pruned=0; kept=0
for b in $gone; do
  case "$b" in main|master) continue ;; esac
  [ "$b" = "$base" ] && continue
  found=$((found + 1))

  if is_merged "$b"; then
    if [ "$dry" -eq 1 ]; then
      info "would delete (merged): $b"; pruned=$((pruned + 1))
    elif del_branch "$b"; then
      ok "deleted (merged): $b"; pruned=$((pruned + 1))
    else
      kept=$((kept + 1))
    fi
    continue
  fi

  # Not merged — deleting would lose commits. Decide deliberately.
  warn "'$b' is NOT merged into '$base' (no merged PR found) — it has unmerged commits."
  if [ "$dry" -eq 1 ]; then
    info "would prompt about unmerged '$b' (kept in dry-run)."
    kept=$((kept + 1)); continue
  fi

  if [ "$tty_ok" -eq 0 ]; then
    warn "No terminal to prompt — keeping unmerged '$b'."
    kept=$((kept + 1)); continue
  fi
  while true; do
    printf "  → keep / delete-anyway / log  [k/d/l] (default k): " >/dev/tty
    read -r ans </dev/tty || { warn "Keeping unmerged '$b'."; kept=$((kept + 1)); break; }
    case "$ans" in
      d|D) if del_branch "$b"; then ok "deleted (unmerged, forced): $b"; pruned=$((pruned+1)); else kept=$((kept+1)); fi; break ;;
      l|L) git --no-pager log --oneline -n 30 "$base..$b" >/dev/tty 2>/dev/null || true ;;
      ""|k|K) info "kept: $b"; kept=$((kept + 1)); break ;;
      *) printf "  (enter k, d, or l)\n" >/dev/tty ;;
    esac
  done
done

if [ "$found" -eq 0 ]; then
  ok "No local branches with a gone upstream. Nothing to prune."
else
  ok "Done — $pruned deleted, $kept kept (of $found with a gone upstream)."
fi
