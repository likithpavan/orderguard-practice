# Record a reviewable training release

Copy this into `work/OG-101/release.md`. Replace placeholders with actual results.
Do not mark a step complete before you have its evidence.

| Item | Evidence to record |
|---|---|
| Ticket | Actual issue key and criteria |
| Code | PR link or local merge commit |
| Review | Reviewer and findings; label solo/AI practice honestly |
| Tests | Passing full suite and manual BUY/SELL results |
| Artifact | Version, commit, SHA-256 from `dist/release.properties` |
| Staging | Running version and digest, boundary/count checks, logs |
| Release decision | Person/role, timestamp, and practice decision |
| Production | Running version and same digest, boundary/count checks |
| Recovery | Previous guarded artifact or explicit stop-and-fix-forward plan |
| Completion | Release note and links recorded in the ticket |

Suggested release note:

“BUY and SELL quantities above 1000 are rejected before accepted-order
storage. Whole quantities 1 through 1000 remain accepted. Existing orders
are preserved.”

## What gets deployed

Deploy `dist/orderguard.jar`, not the source folder and not the feature branch
name. The JAR contains compiled backend classes and browser resources. The
deployment scripts copy those exact bytes into an environment release folder.
Environment variables choose port, environment, data file, and local management
identity. Staging data and training-production data stay separate.

This exercise uses brief stop/start deployment. Rollback changes running code;
it does not undo orders that were previously accepted. Investigating existing
bad records is a separate data/reconciliation task.

## Management and recovery

`tools/LocalDeploy.java` performs an isolated guard check before stopping the
current server. It records a random instance ID and a private control token in
the ignored environment state file. Health exposes the public instance ID but
never the token. Managed stopping uses the token-protected loopback endpoint,
so it does not kill an arbitrary process based only on a recycled process ID.
Do not share or commit the runtime state folder.

A failed startup attempts to restore the previous release only after that
artifact passes the current guard check. If that check fails or there is no
previous release, keep submissions stopped and fix forward. Do not restore the
0.1.0 starter merely because it starts successfully.

The GitHub workflow's training-production job is a temporary runner rehearsal.
Only your local deployment commands leave these simulated environments running.
Real production adds an actual deployment platform, observability, identity,
data controls, and the employer's operating procedures.
