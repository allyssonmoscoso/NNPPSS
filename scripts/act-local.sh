#!/usr/bin/env sh
# Runs the Linux leg of the GitHub workflow locally with act.
set -eu

command -v act >/dev/null 2>&1 || {
    echo "act is not installed or not on PATH." >&2
    exit 1
}
command -v docker >/dev/null 2>&1 || {
    echo "Docker is required by act." >&2
    exit 1
}

NETWORK="nnppss-act"
if ! docker network inspect "$NETWORK" >/dev/null 2>&1; then
    docker network create "$NETWORK" >/dev/null
fi

exec act -j build --matrix os:ubuntu-latest \
    --network "$NETWORK" \
    --artifact-server-path .act-artifacts "$@"
