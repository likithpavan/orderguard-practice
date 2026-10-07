#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/java-env.sh"
bash scripts/build.sh
mkdir -p dist
"$OG_JAVA" -m jdk.jartool/sun.tools.jar.Main --create --file dist/orderguard.jar --main-class com.orderguard.OrderGuardApplication -C build/classes .
"$OG_JAVA" tools/ArtifactInfo.java dist/orderguard.jar > dist/release.properties
echo 'Packaged dist/orderguard.jar. Release identity:'
cat dist/release.properties
