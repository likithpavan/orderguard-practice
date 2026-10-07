#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/java-env.sh"
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo 'Commit or save your tracked changes before switching to the exercise.' >&2
  exit 1
fi
og_branch="${1:-feature/OG-101-order-limit}"
git switch -c "$og_branch" starter/OG-101
echo 'OG-101 exercise started. This branch contains the known >1000 bug.'
echo 'Run bash scripts/run.sh, reproduce 1001, then run bash scripts/test.sh.'
