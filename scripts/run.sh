#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/java-env.sh"
bash scripts/build.sh
exec "$OG_JAVA" --add-modules jdk.httpserver -cp build/classes com.orderguard.OrderGuardApplication
