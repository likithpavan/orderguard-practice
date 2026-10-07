#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/java-env.sh"
rm -rf build/classes
mkdir -p build/classes build/reports
find src/main/java -name '*.java' -print | LC_ALL=C sort > build/main-sources.txt
"$OG_JAVA" -m jdk.compiler/com.sun.tools.javac.Main --release 17 --add-modules jdk.httpserver -d build/classes @build/main-sources.txt
cp -R src/main/resources/. build/classes/
og_commit=$(git rev-parse --short=12 HEAD 2>/dev/null || printf 'untracked')
if ! git diff --quiet HEAD -- src/main VERSION 2>/dev/null; then og_commit="$og_commit-dirty"; fi
printf 'version=%s\ncommit=%s\n' "$(cat VERSION)" "$og_commit" > build/classes/build-info.properties
echo "Compiled OrderGuard $(cat VERSION) ($og_commit)."
