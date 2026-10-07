# Review OG-101

Open the PR diff, or run `git diff main...HEAD` on your feature branch.
Read the change as if you did not write it. Use these checks to form concrete
findings, then record whether each finding was resolved or disproved by evidence.

| Check | What you should see |
|---|---|
| Requirement | Both BUY and SELL use the same quantity rule |
| Boundary | 1000 accepted; 1001 rejected; lower bound remains 1 |
| Correct layer | Backend validation before accepted-order persistence |
| Side effects | Rejection does not save, clear, or alter accepted records |
| Existing behaviour | Supported symbols and side checks remain intact |
| Input handling | Whole numbers only; overflow and missing input rejected |
| Tests | Actual rejection, no added record, existing-record preservation |
| Scope | Focused fix; version intentional; no credentials or generated data |
| Release | Full CI gate, one artifact, staging evidence, usable recovery plan |

## A realistic review finding

Suppose the code says `if (quantity >= 1000)`. A useful review comment is:

“The ticket includes 1000 as a valid order. This comparison rejects 1000.
Please change the boundary and keep the BUY/SELL 1000 acceptance tests.”

That comment states the problem, its user-visible effect, and how to verify
the correction. “Looks fine” without reading the path or evidence is weak review.

## Ask an AI tool for a second pass

Use this prompt after implementation:

```text
Review the diff on this OrderGuard feature branch against the starter base.
Read AGENTS.md and docs/tickets/OG-101.md. Do not edit code.
Find concrete bugs, boundary mistakes, persistence side effects, test gaps,
and release risks. For each finding, give file, relevant method, triggering
input, actual outcome, required outcome, and severity. Distinguish a verified
bug from an assumption. Check BUY and SELL 1, 1000, 1001, and rejection with
existing records. Do not claim tests ran unless you actually run them.
```

AI review is additional input. Verify its findings. A teammate's review and
your team's required approval policy still apply to employer work.

## Debug in IntelliJ

The simplest reliable run route is the provided terminal script. To step
through Java in the debugger, select a JDK 17+ project SDK, mark
`src/main/java` as Sources Root and `src/main/resources` as Resources Root,
and run/debug `com.orderguard.OrderGuardApplication` as an Application.
If your configuration needs it, add VM option `--add-modules jdk.httpserver`.

Add a breakpoint before `validator.validate()` in `OrderService.submit()`.
Submit 1001 from the screen. On the fixed code, validation throws and execution
does not reach `repository.save()`. Submit 1000: execution does reach save.
Do not run the debugger and the terminal server on the same port simultaneously.
An IDE run that skips the build script may show development/untracked build
metadata; use the scripts for release identity.
