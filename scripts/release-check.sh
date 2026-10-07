#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/java-env.sh"
"$OG_JAVA" tools/ReleaseCheck.java "${1:-dist/orderguard.jar}" "${2:-staging}"
