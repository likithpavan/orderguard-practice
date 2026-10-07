#!/usr/bin/env bash
# Source this file from the other scripts; JAVA_HOME is respected but never changed.
set -euo pipefail
ORDERGUARD_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ORDERGUARD_ROOT"
if [ -n "${ORDERGUARD_JAVA:-}" ]; then
  OG_JAVA="$ORDERGUARD_JAVA"
elif [ -n "${JAVA_HOME:-}" ]; then
  OG_JAVA="$JAVA_HOME/bin/java"
else
  OG_JAVA=java
fi
if ! og_jdk_version=$("$OG_JAVA" -m jdk.compiler/com.sun.tools.javac.Main -version 2>/dev/null); then
  echo 'OrderGuard needs a full JDK 17 or newer. See START_HERE.md for setup.' >&2
  exit 1
fi
if [[ "$og_jdk_version" =~ javac\ ([0-9]+) ]] && [ "${BASH_REMATCH[1]}" -lt 17 ]; then
  echo 'OrderGuard needs JDK 17 or newer; your selected JDK is older.' >&2
  exit 1
fi
