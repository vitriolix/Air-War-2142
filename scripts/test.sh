#!/usr/bin/env bash
# Run tests for a platform (or all).
# Usage: test.sh <jvm|web|wasm|android|all>
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

case "${1:-all}" in
  jvm)        info "Running JVM tests…";          exec $GRADLE jvmTest ;;
  web|js)     info "Running JS tests…";           exec $GRADLE jsTest ;;
  wasm|wasmjs) info "Running Wasm tests…";        exec $GRADLE wasmJsTest ;;
  android)    info "Running Android unit tests…"; exec $GRADLE testDebugUnitTest ;;
  all)        info "Running all tests…";          exec $GRADLE allTests ;;
  *)          fail "Usage: test.sh <jvm|web|wasm|android|all>" ;;
esac
