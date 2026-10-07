#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/java-env.sh"
"$OG_JAVA" tools/LocalDeploy.java rollback "${1:?Choose staging or production}"
