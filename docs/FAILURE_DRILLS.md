# Practise failures after your first successful release

Use your practice repository and simulated data. Start from your fixed branch.
Stop the development server before switching branches.

## 1. An off-by-one bug and red CI

Temporarily change the upper-bound comparison to `>= MAX_QUANTITY`.
Run `bash scripts/test.sh`. The 1000 acceptance checks must fail. Read the
failure, restore `> MAX_QUANTITY`, and rerun the checks. This teaches why a
boundary test matters. Do not commit the temporary defect to main.

## 2. Bypass the browser

Run the fixed development app and use a second terminal:

```bash
curl -i http://localhost:8080/api/orders \
  --data 'symbol=AAPL&side=SELL&quantity=1001'
curl http://localhost:8080/api/orders
```

Expect HTTP 400 and unchanged accepted count. A UI-only validation fix fails
this check. Backend validation protects all callers.

## 3. Two safe releases and a real local rollback

After fixing OG-101, test and deploy version 0.2.0 to training production:

```bash
bash scripts/pipeline.sh
bash scripts/deploy-local.sh production dist/orderguard.jar
```

Open http://localhost:8282 and submit one valid order. Note its count and ID.
For a second harmless release, change `VERSION` to `0.2.1`, commit that change
on your practice branch, and run:

```bash
bash scripts/pipeline.sh
bash scripts/deploy-local.sh staging dist/orderguard.jar
bash scripts/deploy-local.sh production dist/orderguard.jar
bash scripts/status-local.sh production
bash scripts/rollback-local.sh production
bash scripts/status-local.sh production
```

Expect the running version to go from 0.2.1 back to 0.2.0. The saved order
should still be present. Both artifacts must enforce OG-101. Inspect the hashes
and `.runtime/deploy/production/server.log` to trace the change.
The first release has no prior release to roll back to; you need two guarded
versions for this drill. Re-deploy 0.2.1 when finished if you want it active.

## 4. Try to deploy the defective starter

Preserve your work with a commit. On a separate temporary branch from the
starter tag, package version 0.1.0 without claiming it passed tests. Attempt
`bash scripts/deploy-local.sh staging dist/orderguard.jar`.

Expect the isolated release check to reject the artifact before replacing the
current deployment. A “UP” health result is not enough: business acceptance
criteria must hold. Switch back to your fixed branch, rebuild the valid JAR,
and confirm staging remains healthy. Never edit the release check to bypass this.

## 5. Inspect a storage failure

Read the test named “failed persistence does not report a stored order”. It
forces a write failure, expects an exception, and checks that the in-memory
accepted count is unchanged. Then inspect the damaged-data test: bad files
must fail visibly rather than be silently erased.

Do not corrupt your saved practice ledger just to repeat this drill. The tests
use temporary directories and remove them afterwards.

## 6. Practise a merge conflict

Commit your existing work first. Create two throwaway branches from the same
fixed commit. Edit the same line of `VERSION` differently on each branch and
commit each change. Merge one into the other. Git will show a conflict.

Read both versions, choose the intended version, remove the conflict markers,
stage the resolved file, and finish the merge commit. Use `git status` at every
stage. If you want to cancel the uncompleted practice merge, use
`git merge --abort`, then return to your fixed branch.

For real code conflicts, choosing a version is not enough: understand both
changes and run the affected tests after resolution.

## What to record

For each drill, record the symptom, the evidence, the cause, your action, and
the verification. This is useful incident/debugging practice and stronger
evidence of skill than a checklist with every box pre-ticked.
