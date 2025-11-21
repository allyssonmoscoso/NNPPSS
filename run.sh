#!/usr/bin/env sh
DIR="$(cd "$(dirname "$0")" && pwd)"
java -jar "$DIR/dist/NNPPSS-fat.jar" "$@"
