#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
echo 'Starting the current OrderGuard branch. Open http://localhost:8080 after the server starts.'
echo 'Keep this terminal open. Press Control+C to stop the server.'
bash scripts/run.sh
