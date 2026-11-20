#!/usr/bin/env sh
# run-dev.sh - Development entry point for the JavaFX application.
#
# Usage:
#   ./run-dev.sh            -> run the application
#   ./run-dev.sh compile    -> compile only
#   ./run-dev.sh test       -> run the automated test suite
#   ./run-dev.sh package    -> build the application JAR
#   ./run-dev.sh clean      -> clean build artifacts
#
# Data (config, db, packages, logs) defaults to the per-user data directory.
# Pass -Dnnppss.dataDir=/path to override it.
set -eu

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

case "${1:-run}" in
  run)
    exec mvn -q javafx:run
    ;;
  compile)
    exec mvn -q -DskipTests compile
    ;;
  test)
    exec mvn -q test
    ;;
  package)
    exec mvn -q -DskipTests package
    ;;
  clean)
    exec mvn -q clean
    ;;
  *)
    printf 'Uso: %s [run|compile|test|package|clean]\n' "$0" >&2
    exit 2
    ;;
esac
