## Behaviour

Issue: <!-- actual Jira key / GitHub issue link -->

Before: <!-- concrete failing input and stored result -->
After: <!-- concrete response and unchanged accepted-order count -->

## Validation

- [ ] `bash scripts/test.sh` passed without `--baseline`.
- [ ] BUY and SELL: 1 and 1000 accepted; 1001 rejected.
- [ ] Rejected requests add zero records, including with existing accepted orders.
- [ ] I read the diff and can explain validation before persistence.
- [ ] No credentials, unrelated changes, or generated data committed.

## Release

Artifact/version: <!-- fill after packaging -->
Staging evidence: <!-- report/run link -->
Recovery: stop submissions first; restore only a release that preserves OG-101,
otherwise fix forward. Deployment evidence belongs here after release.
