# Design inspiration and official references

OrderGuard is independently written teaching code. The order-entry domain is
inspired by the public FINOS TraderX reference application, rather than your
earlier Gold project. The 1,000-share rule and OG-101 exercise are teaching
requirements for this project, not a statement of any bank's order policy.

Official sources checked while preparing the project (7 October 2026):

- [FINOS TraderX](https://github.com/finos/traderX) — public trading-system example.
- [JDK 17 HTTP server](https://docs.oracle.com/en/java/javase/17/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html) — the embedded backend server.
- [Git documentation](https://git-scm.com/docs) — branches, commits, remotes, merges.
- [GitHub flow](https://docs.github.com/en/get-started/using-github/github-flow) — feature branch and PR workflow.
- [GitHub artifacts](https://docs.github.com/en/actions/tutorials/store-and-share-data) — passing an immutable build between jobs.
- [GitHub deployments and environments](https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments) — approval rules and plan limits.
- [actions/checkout](https://github.com/actions/checkout) and [actions/setup-java](https://github.com/actions/setup-java) — runner checkout and JDK setup.
- [actions/upload-artifact](https://github.com/actions/upload-artifact) — artifact upload action. The workflow uses the v4 upload / v5 download pairing shown in GitHub's artifact guide, with current checkout and setup-java major tags.
- [Jira development linking](https://support.atlassian.com/jira-cloud-administration/docs/use-the-github-for-jira-app/) — actual issue keys connect branches, commits, and PRs.
- [Jira automation triggers](https://support.atlassian.com/cloud-automation/docs/jira-automation-triggers/) — event-driven status signals.
- [Jira API authentication](https://developer.atlassian.com/cloud/jira/platform/basic-auth-for-rest-apis/) — account email and API token for personal practice scripts.
- [API tokens and scoped origins](https://support.atlassian.com/atlassian-account/docs/manage-api-tokens-for-your-atlassian-account/) — scoped tokens use the Atlassian API origin.
- [Jira Get issue API](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issues/#api-rest-api-3-issue-issueidorkey-get) — the optional read-only importer endpoint.

The local implementation, training workflow, and recovery rules are the design
of this exercise. Your employer's approved process takes precedence at work.
