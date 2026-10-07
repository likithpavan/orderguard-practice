#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/java-env.sh"
og_issue="${1:?Provide the actual Jira key}"
bash scripts/import-jira.sh "$og_issue"
bash scripts/prepare-ai-task.sh "$og_issue"
echo 'Read the imported issue and task packet, then create the appropriate feature branch.'
