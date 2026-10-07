#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/java-env.sh"
bash scripts/build.sh
rm -rf build/test-classes
mkdir -p build/test-classes
find src/test/java -name '*.java' -print | LC_ALL=C sort > build/test-sources.txt
"$OG_JAVA" -m jdk.compiler/com.sun.tools.javac.Main --release 17 --add-modules jdk.httpserver -cp build/classes -d build/test-classes @build/test-sources.txt
"$OG_JAVA" --add-modules jdk.httpserver -cp build/classes:build/test-classes com.orderguard.OrderGuardTests "$@"
