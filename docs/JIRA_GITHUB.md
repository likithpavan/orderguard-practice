# Connect your practice Jira and GitHub

The app, tests, AI packet, and local releases work without accounts. This file
adds real remote services when you are ready. These are instructions and
working scripts; this download has not created issues, repositories, or releases
in your accounts.

## A. Create the work item

In your Jira Cloud software project/space, create a Bug. Copy
`docs/tickets/OG-101.md` as its content. Use project key OG if available.
Jira chooses the issue number: your first issue might be **OG-1**. Do not expect
Jira to assign OG-101 just because that is the training filename.

Use the actual key consistently: `feature/OG-1-order-limit`, commit
`OG-1 Reject quantities above 1000`, PR title `OG-1: Enforce order limit`.
The core requirement and local tests stay the same.

## B. Put this existing local repository on GitHub

Create a new **empty** GitHub repository named `orderguard-practice`. Do not
initialize it with a README, license, or Git ignore file; these files/history
already exist in your local project. Choose visibility for your learning needs.
Public means the practice code and commits are visible to everyone; account
plan limits can affect protection features on private repositories.

Copy the HTTPS repository URL from GitHub. Replace the example below:

```bash
git remote add origin https://github.com/YOUR_USERNAME/orderguard-practice.git
git remote -v
git push -u origin main
git push origin reference-solution
git push origin starter/OG-101
```

The included local history has no remote and uses a training author. Set your
own local Git name/email before your first commit as described in PROJECT_STEPS.
For GitHub authentication use your configured credential manager, SSH setup,
or GitHub CLI authentication. Never put a token in the remote URL or code.

**Expected:** `main` contains the known starter defect, so its initial CI is
red and staging is blocked. `reference-solution` has the working answer.
If GitHub sets another default branch, select `main` in repository settings.
Keep the first feature PR targeted at `main`.

Push your fixed feature branch:

```bash
git push -u origin feature/OG-1-order-limit
```

Open the PR in GitHub: base `main`, compare your feature branch. Complete the
provided PR template and link the Jira item. If using only the local ticket,
use the OG-101 branch/title and optionally create a GitHub issue using the
provided issue template.

## C. Link Jira to the repository

Your Jira administrator connects GitHub through the **GitHub for Atlassian**
app/integration and selects the practice repository. Grant only the repositories
needed for the exercise. Account setup and organization policy may require
an administrator.

After linking, Jira can associate branch/commit/PR information when the actual
issue key appears in the branch name, commit message, or PR title. Verify the
item's development panel shows your pushed branch and PR.

This association tells you which development work belongs to the ticket.
It does not prove that the fix passed tests or is deployed. The team still
needs a component/service-to-repository mapping, for example:

| Ticket component | Repository | First search |
|---|---|---|
| Order entry / validation | `orderguard-practice` | POST `/api/orders`, then `OrderValidator` |

In an employer environment, ask for the actual repository, base branch, build
command, and runbook for the service named by the issue.

## D. Import a real Jira issue and prepare an AI task

This optional script makes a read-only Jira Cloud REST request. It does not
change status, comments, assignees, or anything else in Jira.

Set `JIRA_SITE` to your real site and `JIRA_EMAIL` to the token owner's account:

```bash
export JIRA_SITE="https://your-team.atlassian.net"
export JIRA_EMAIL="you@example.com"
```

Create an API token using Atlassian's account security page. A scoped token
with the permissions needed to read the issue is preferable; follow the
official Get issue documentation for its scope and project permissions.
For a **scoped** token, also set your real cloud ID:

```bash
export JIRA_CLOUD_ID="your-real-cloud-id"
```

The script then calls `https://api.atlassian.com/ex/jira/CLOUD_ID/rest/api/3/...`.
For an **unscoped** token, leave `JIRA_CLOUD_ID` unset; the script calls your
`.atlassian.net` origin. These token types have different required API origins.
Use your site's official administration/developer guidance to find its cloud ID.

Read the token into an environment variable without putting its text in shell
history. In a Bash terminal:

```bash
read -r -s -p "Jira API token: " JIRA_API_TOKEN
echo
export JIRA_API_TOKEN
bash scripts/from-jira.sh OG-1
unset JIRA_API_TOKEN
```

If your Mac terminal uses Zsh, start `bash` first for that `read` command.
Never paste a token into ChatGPT, a commit, or a tracked file.

**Expected:** `work/OG-1/jira.json` and `work/OG-1/ai-task.md` are created.
Open them locally with your approved AI coding tool. `work/` is ignored by Git.
The task packet tells the tool which files to inspect and which checks to run.
Read the actual imported description: it may contain requirements beyond OG-101.
The template packet is intentionally for this project, not a generic ticket solver.

This is the concrete automation chain:
**real issue → read-only import → code task packet → AI-assisted edit → tests → PR**.
The provided import has been compiled and checked for local validation; it has
not been tested against your account or a live issue because no credentials
were supplied.

## E. Automate Jira status signals

In your practice project's Automation area, create these rules only when your
workflow has the matching statuses and the GitHub integration is configured:

| Trigger | Condition | Action |
|---|---|---|
| Branch created | Linked issue is To Do | Transition to In Progress |
| Pull request created | Linked issue is In Progress | Transition to In Review |
| Pull request merged | Linked issue is In Review | Transition to Ready for Release |

Adapt status names to the workflow your project actually supports. Test one
rule at a time and inspect Jira's automation audit log. Rule actors need the
transition permissions; automation usage limits depend on the account plan.

Do not transition to Done just because a branch exists or a PR merged.
In this project, **you record successful training-production verification and
then close the issue**. The GitHub jobs are explicitly rehearsals; linking their
environment records to Jira must not represent them as a real hosted service.

For a real continuous-delivery system, a trusted successful production
deployment event plus its verification evidence can drive a completion rule.
That requires a real deployment integration and a team-defined definition of Done.
Do not expose an unauthenticated webhook that runs arbitrary AI commands.

## F. Run and protect the GitHub workflow

The checked-in `.github/workflows/ci-cd.yml` is executable GitHub Actions
configuration. Enable Actions in the practice repository if necessary.

| Event | Result |
|---|---|
| Feature push / PR | Compile and full tests; package only on success |
| Successful `main` push | Build/test, upload JAR, download the same JAR, staging rehearsal |
| Manual run on `main`, production input false | Build/test and staging rehearsal |
| Manual run on `main`, production input true | Build/test, staging, then training-production rehearsal |

Create environments **practice-staging** and **practice-production** in
repository settings. Where supported, restrict deployment branches to `main`
and set required reviewers for practice-production. Environment names alone
do not require approval; the protection rule must actually be configured.

GitHub's documented plan limits mean required environment reviewers on Free,
Pro, and Team are available for public repositories. If the setting is absent
for your chosen private repo, keep the explicit manual workflow start and
record the limitation; do not assume YAML has created an approval gate.

For a solo approval exercise you may allow yourself as a reviewer. That is a
practice gate, not independent approval. With a collaborator, use their review
and optionally prevent self-review.

The build job uploads `orderguard-release` plus test evidence. Staging and
training production download that artifact and compare its actual version,
commit, and digest with `release.properties`. No recompile occurs in those jobs.
Each rehearsal starts the JAR with disposable data and then stops it.
Runner completion does not leave a public server running.

Inspect Actions logs and download the reports. Use branch protection or rulesets
to require the **Build and test** check on PRs once GitHub has observed a run.
Availability varies with plan and visibility. The workflow uses readable major
action tags for learning; follow your employer's approved version-pinning and
supply-chain policy in a real repository.

This project stops at local training production and remote pipeline rehearsals.
A lasting cloud deployment needs an actual hosting target, its release adapter,
credentials, operating budget, and runbook. None of those accounts are assumed.

## Official references

- [Jira and GitHub development linking](https://support.atlassian.com/jira-cloud-administration/docs/use-the-github-for-jira-app/)
- [Jira API authentication](https://developer.atlassian.com/cloud/jira/platform/basic-auth-for-rest-apis/)
- [Token types and origins](https://support.atlassian.com/atlassian-account/docs/manage-api-tokens-for-your-atlassian-account/)
- [Get issue API](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issues/#api-rest-api-3-issue-issueidorkey-get)
- [Jira automation triggers](https://support.atlassian.com/cloud-automation/docs/jira-automation-triggers/)
- [GitHub environments and reviewer limits](https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments)
- [GitHub workflow artifacts](https://docs.github.com/en/actions/tutorials/store-and-share-data)
