#!/usr/bin/env bash
# Release prep, broken into discrete steps so each can be run on its own.
# Usage: release.sh <check-git|test|build|version|branch|tag|all>
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

step_check_git() {
  info "[release] Step: git is tidy"
  "$REPO_ROOT/scripts/tidy-git.sh"
}

step_test() {
  info "[release] Step: run all tests"
  $GRADLE allTests
  ok "All tests passed."
}

step_build() {
  info "[release] Step: build release artifacts (JVM jar, web production bundle, Android release APK)"
  $GRADLE jvmJar jsBrowserProductionWebpack assembleRelease
  ok "Artifacts built under composeApp/build/. (Android release APK is unsigned unless a signing config is set up.)"
}

step_version() {
  info "[release] Step: app version"
  ok "Current version: $(app_version)  (edit in composeApp/build.gradle.kts → korge { version })"
}

step_branch() {
  info "[release] Step: prepare release branch"
  require_git_repo
  local v b; v="$(app_version)"; b="release/$v"
  git switch -c "$b" 2>/dev/null || git switch "$b"
  ok "On release branch '$b'."
}

step_tag() {
  info "[release] Step: tag release"
  require_git_repo
  local v; v="$(app_version)"
  [ -z "$(git status --porcelain)" ] || fail "Working tree not clean — commit before tagging."
  git tag -a "v$v" -m "Release v$v"
  ok "Created tag v$v. Push it with: git push origin v$v"
}

step_all() {
  info "[release] Running full release prep for v$(app_version)…"
  step_check_git
  step_test
  step_build
  step_version
  ok "Release prep complete. Next, when ready: './gradlew releaseBranch' then './gradlew releaseTag'."
}

case "${1:-}" in
  check-git) step_check_git ;;
  test)      step_test ;;
  build)     step_build ;;
  version)   step_version ;;
  branch)    step_branch ;;
  tag)       step_tag ;;
  all)       step_all ;;
  *) fail "Usage: release.sh <check-git|test|build|version|branch|tag|all>" ;;
esac
