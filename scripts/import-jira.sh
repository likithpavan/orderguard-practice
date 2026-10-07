#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/java-env.sh"
"$OG_JAVA" tools/JiraTicket.java "${1:?Provide the actual Jira key}"
