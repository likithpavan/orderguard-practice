#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/java-env.sh"
"$OG_JAVA" tools/LocalDeploy.java deploy "${1:?Choose staging or production}" "${2:-dist/orderguard.jar}"
