# Do one complete delivery: OG-101

Follow this file in order. Run commands from the `orderguard` folder. Stop a
development server with Control+C before changing branches or rebuilding it.
Keep short notes in `work/OG-101/evidence.md`; `work/` is ignored by Git.

## Step 1 — Receive and understand the ticket

Open `docs/tickets/OG-101.md`. The requirement is **1 ≤ quantity ≤ 1000** for
BUY and SELL, with whole numbers only. The required decision for 1001 is an
error and no new accepted order. The whole accepted ledger is not reset to zero.

If you have Jira, create the issue from that file using `docs/JIRA_GITHUB.md`.
Otherwise the local ticket file is your starting work item. Move the issue to
In Progress when you begin. Use your real assigned key in remote work.

**Keep:** the issue key, acceptance criteria, and your one-sentence problem statement.

## Step 2 — Start with the existing code

The download includes a local Git repository. It opens on the finished
`reference-solution` branch so you can see the working app first. Start the
exercise from the buggy version:

```bash
git status
bash scripts/start-ticket.sh
git log --oneline --decorate -3
bash scripts/run.sh
```

The default feature branch is `feature/OG-101-order-limit`. If Jira gave you
`OG-1`, use `bash scripts/start-ticket.sh feature/OG-1-order-limit` instead.

At http://localhost:8080 submit 1001 with BUY, then SELL. Observe that the
starter wrongly stores both. Capture the decision and accepted count. Stop
the server with Control+C.

**Expected:** the starter banner and the reproducible defect. Existing code
from Git is now local; creating a branch gives your change its own history.

If you join a team with an existing remote repository, the analogous starting
commands are `git clone REPOSITORY_URL`, `git fetch origin`, then a feature
branch from the team's designated base. Jira does not automatically contain
the source code; the team maps its service/component to a repository.

## Step 3 — Find the relevant code

Open these files in order in IntelliJ:

1. `src/main/resources/public/app.js`: it sends POST `/api/orders`.
2. `OrderController.java`: it extracts symbol, action, and quantity.
3. `OrderService.java`: it calls `validator.validate()` before `repository.save()`.
4. `OrderValidator.java`: it parses a positive whole number, but the starter
   has no upper-limit check. This is where you implement OG-101.
5. `OrderRepository.java`: only accepted orders should reach `save()`.

The Java files are under `src/main/java/com/orderguard/`. Search the repository
with IntelliJ's Find in Files, or:

```bash
git grep -n 'validate\|submit\|save' -- src/main/java
```

**Keep:** a short note identifying the missing check and why it must precede storage.

## Step 4 — Run the tests before changing code

```bash
bash scripts/test.sh
```

**Expected on the starter:** the command fails. The OG-101 upper-limit tests
show that requests which should fail are succeeding. The rest of the checks
also protect existing behaviour. `build/reports/test-results.xml` records results.

An automated test gives the app an input, observes its actual result, and
compares it with the expected result. Example: BUY 1001 must throw a validation
error and leave the repository count unchanged. A failure before your fix is
evidence that the check can detect the ticket's bug.

Optional: `bash scripts/test.sh --baseline` runs checks that exclude the known
upper-limit defect. It is useful for exploration and never enough for release.

**Keep:** failing test names and the reason they fail.

## Step 5 — Make the fix, with AI if you want

Generate a task packet:

```bash
bash scripts/prepare-ai-task.sh OG-101
```

Open `work/OG-101/ai-task.md`. Give this file and the repository to your approved
AI coding tool. If your tool can read the working folder, ask it to read the
packet. If using a chat-only tool, include only the relevant practice files.
The packet has criteria, paths, and the exact verification command.

The required logic is simple: after successfully parsing a positive whole
quantity, reject it when it is greater than 1000. Keep 1000 accepted. Read the
AI's diff and verify that `OrderService` still validates before it saves.

If you want to code it yourself, add this inside `OrderValidator.validate()`
after the positive-number check and before the validated-order return:

```java
if (quantity > MAX_QUANTITY) {
    throw invalidQuantity();
}
```

Change `VERSION` from `0.1.0` to `0.2.0`. Add one useful test of your own, such
as rejecting SELL 1002 with two accepted orders already present and verifying
both records are unchanged. Follow the existing test method structure.

**Keep:** your diff and a plain explanation of the change. AI can generate
code and suggest checks; you remain responsible for the implementation.

For employer work, use the employer-approved AI tool and data policy. This
practice repository contains no employer code or production data.

## Step 6 — Test the complete behaviour

```bash
bash scripts/test.sh
bash scripts/run.sh
```

Run the manual matrix below in the browser, then stop the server:

| Case | Decision | Accepted count change |
|---|---|---:|
| BUY 1 and SELL 1 | Accepted | +1 each |
| BUY 1000 and SELL 1000 | Accepted | +1 each |
| BUY 1001 and SELL 1001 | Quantity error | 0 each |
| 0, -1, 1.5, missing quantity | Quantity error | 0 each |

Use `docs/test-cases.csv` to record actual results. The automated suite also
checks direct HTTP requests, duplicate fields, persistence across restart,
write failures, and concurrent accepted requests.

**Expected:** all complete checks pass. A rejected order never appears in the
accepted ledger. Keep the test report and manual result notes.

## Step 7 — Review and commit locally

```bash
git diff --check
git diff
git status
```

Review `docs/CODE_REVIEW.md`. Check that the diff is focused and that no test
has been weakened. Stage the files you intentionally changed, for example:

```bash
git config user.name "Your Name"
git config user.email "Your GitHub email or no-reply address"
git add src/main/java/com/orderguard/OrderValidator.java VERSION
git add src/test/java/com/orderguard/OrderGuardTests.java
git diff --cached
git commit -m "OG-101 Reject quantities above 1000 before saving orders"
```

Use your actual Jira key if it differs. These Git identity settings apply to
this repository only. A commit records a local snapshot; it does not upload it.

**Keep:** the commit ID and your review findings.

## Step 8 — Push and open a pull request

Follow `docs/JIRA_GITHUB.md` to create an empty GitHub practice repository and
add its URL as `origin`. Publish `main` first; it contains the known defect and
is the target for your first feature PR. Its first CI run is expected to fail.

```bash
git push -u origin main
git push -u origin feature/OG-101-order-limit
```

If you named your branch using `OG-1`, use that branch name. Open a PR from
your feature branch into `main`. Use title **OG-101: Reject quantities above
1000** and complete the provided PR template. Include the issue and test evidence.

**Expected:** GitHub stores your commits, shows the changed lines, and starts
the CI workflow. A PR proposes a merge; it has not released the app.

If you are practising without GitHub, use the local merge route in Step 10.

## Step 9 — Do code review and watch CI

Open the PR's Files changed and Checks tabs. Inspect the complete diff using
`docs/CODE_REVIEW.md`. Ask a real collaborator to review if available. A solo
exercise or AI review is a practice review, not independent peer approval.

CI starts a clean runner, checks out the code, compiles Java, runs the full
suite, and packages a JAR only after the suite passes. Inspect failed logs,
fix the problem on your feature branch, commit, and push again. Do not bypass
the failure or change expected rejection into expected acceptance.

An optional AI review prompt is provided in `docs/CODE_REVIEW.md`. Address
its findings with evidence rather than accepting them automatically.

**Keep:** a passing CI link and your review notes. CI is automated checking;
code review is a human assessment of the change and its implications.

## Step 10 — Merge the reviewed change

On GitHub, merge after the checks pass and your practice review is complete.
Then bring the merged code back to your laptop:

```bash
git fetch origin
git switch main
git pull --ff-only origin main
```

For a completely local exercise instead:

```bash
git switch main
git merge --no-ff feature/OG-101-order-limit -m "Merge OG-101 order-limit fix"
```

Use your actual branch name. **Expected:** `main` now includes your fix and
version 0.2.0. The GitHub main push also runs the staging rehearsal when CI passes.

## Step 11 — Build one release artifact

```bash
bash scripts/pipeline.sh
cat dist/release.properties
```

This runs the tests, builds `dist/orderguard.jar`, and checks that exact JAR
in disposable staging and production instances. The temporary servers stop
after their checks. Reports appear in `build/reports/`.

Version identifies the release; commit identifies the source snapshot;
SHA-256 identifies the actual JAR bytes. A source branch name alone cannot
identify precisely what is running. Save `release.properties` with your evidence.

Do not edit or repackage between staging and training production. If you make
a source change, test it and create a new release candidate.

## Step 12 — Deploy to local staging

```bash
bash scripts/deploy-local.sh staging dist/orderguard.jar
bash scripts/status-local.sh staging
```

Open **http://localhost:8181**. Verify the environment badge and version. Repeat
BUY/SELL 1000 and 1001 and record the count changes. The deployment command
returns while the staging server keeps running with its own data file.

Inspect `.runtime/deploy/staging/server.log`. Accepted and rejected requests
have request IDs; the browser response shows the same ID for tracing.

**Keep:** staging report, UI results, running version/commit, and artifact digest.

## Step 13 — Promote to training production

Complete `docs/RELEASE_CHECKLIST.md`. In a team this is where your approved
release process, roles, timing, and permissions apply. For this local exercise,
record your own practice decision; do not label it a colleague's approval.

```bash
bash scripts/deploy-local.sh production dist/orderguard.jar
bash scripts/status-local.sh production
```

Open **http://localhost:8282**. It uses separate data from staging. Check the
same boundaries and inspect `.runtime/deploy/production/server.log`.
Compare staging and production hashes from the two status commands.

**Expected:** both run the identical JAR digest, and production enforces the rule.
This is local training production on your laptop, not a real bank or cloud service.

For the optional GitHub approval exercise, use Actions → Run workflow on
`main` and select the training-production input. This triggers a new complete
run whose build artifact is promoted unchanged through both temporary jobs.
Configure `practice-production` reviewers first if your plan supports them.

## Step 14 — Monitor and practise recovery

Watch health, acceptance behaviour, errors, request IDs, and data integrity.
An HTTP 400 for quantity 1001 is expected business rejection, not an outage.
An HTTP 500 or an unreachable health route requires investigation.

Use `docs/FAILURE_DRILLS.md` to practise a second safe release and rollback.
Stop submissions first when the guard is uncertain. Never roll back to the
starter: it restores the prohibited behaviour. The scripts precheck every
rollback artifact; if it cannot enforce OG-101, keep the service stopped and
fix forward. Accepted records are preserved across release changes.

## Step 15 — Close the ticket with evidence

Add the PR/commit, CI report, release version/digest, staging results, and
training-production results to the ticket or `work/OG-101/evidence.md`.
Write a release note: “BUY and SELL quantities above 1000 are rejected before
storage. Quantities 1 through 1000 remain accepted.” Then mark the practice
ticket Done. Do not mark it Done just because the PR merged.

Stop the managed servers when finished:

```bash
bash scripts/stop-local.sh staging
bash scripts/stop-local.sh production
```

To return to the supplied answer, commit your work first and use
`git switch reference-solution`. Your exercise branch and commits remain in Git.
