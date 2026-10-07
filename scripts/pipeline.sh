#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/java-env.sh"
bash scripts/test.sh
bash scripts/package.sh
bash scripts/release-check.sh dist/orderguard.jar staging
bash scripts/release-check.sh dist/orderguard.jar production
echo 'Local pipeline passed. The same JAR was checked in both environments; temporary servers are now stopped.'
