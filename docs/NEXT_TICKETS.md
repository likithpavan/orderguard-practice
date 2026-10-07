# Build experience by repeating the same delivery cycle

After OG-101, take one change at a time. Write criteria and tests before
implementing. Use the same branch → review → CI → staging → production process.

| Ticket | Concrete next project change | What it teaches |
|---|---|---|
| OG-102 | Accept a client request ID; repeated submission with that ID creates one accepted order | Retry behaviour and idempotency |
| OG-103 | Replace the TSV store with a relational database and a versioned schema | SQL, migrations, transaction boundaries |
| OG-104 | Authenticate a user and restrict ledger/clear operations by role | Identity, authorization, security tests |
| OG-105 | Add a configurable quantity limit with explicit approved bounds | Configuration and release compatibility |
| OG-106 | Measure acceptance/rejection/error rates and expose useful metrics | Monitoring and incident diagnosis |
| OG-107 | Add a mock downstream router with failure and retry tests | Service integration and reconciliation |

This starter keeps infrastructure small so you can complete one SDLC cycle.
Afterwards you can move the same domain into your team's framework, Maven or
Gradle, JUnit, database, container platform, and approved delivery pipeline.

For front-office onboarding, learn your team's service ownership, repository
map, test data, environments, access process, risk controls, incident runbooks,
review standards, release approvals, and recovery constraints. This project
does not identify any particular bank's internal tools or architecture.
