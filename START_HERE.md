# Start OrderGuard

You have a real project to run and change. Work through the steps yourself;
you do not need to answer questions in ChatGPT to continue.

## 1. Open the project

Unzip **OrderGuard_Project.zip**. Open the **orderguard** folder in IntelliJ.
Open IntelliJ's Terminal. All commands below run inside that folder.

Check your tools:

```bash
java -version
git --version
```

You need a full **JDK 17 or newer**, plus Git. The application uses only Java's
standard library. You do not need Maven, Docker, a database server, or a paid
AI subscription to run it.

If Java is missing on your Mac: in IntelliJ's project SDK selector choose
**Download JDK**, select version **17** and a distribution such as Temurin.
After installation, copy its installation directory from the SDK settings.
Use that directory in this command, replacing the example path:

```bash
export ORDERGUARD_JAVA="/your/JDK/Contents/Home/bin/java"
"$ORDERGUARD_JAVA" -version
```

If Git is missing, run `xcode-select --install` in your Mac terminal and follow
the installation dialog. The script command `bash scripts/build.sh` checks
whether your Java installation includes the compiler.

## 2. See the working application first

```bash
bash scripts/run.sh
```

Keep this terminal running. Open **http://localhost:8080** in your browser.

Try these three orders:

| Action | Quantity | Expected result |
|---|---:|---|
| BUY | 1000 | Accepted; the count increases by one |
| BUY | 1001 | Error; the count stays the same |
| SELL | 1001 | Error; the count stays the same |

These are simulated orders. There is no broker connection or money movement.
Stop the server with **Control+C** when you have finished trying it.

## 3. Start your ticket from the buggy version

The downloaded checkout is `reference-solution`, the finished application.
The included Git repository also has a deliberately buggy `main` branch and
`starter/OG-101` tag. This lets you do a meaningful fix and pull request.

```bash
bash scripts/start-ticket.sh
bash scripts/run.sh
```

At localhost:8080 you will now see the starter banner. Submit **1001** shares.
The starter incorrectly accepts it. Reproduce the same problem for SELL.
Stop the server with Control+C.

## 4. Follow the project to release

Open **docs/PROJECT_STEPS.md**. It gives each action, command, expected result,
and the evidence you should keep, from receiving OG-101 to closing the ticket.

Your route is:

1. Read the ticket and reproduce the bug.
2. Find the backend validator and run the failing tests.
3. Fix it yourself or with an AI coding tool, then review the diff.
4. Run tests, commit, push, and open a pull request.
5. Review and merge; run the automatic pipeline.
6. Deploy the same JAR to local staging and training production.
7. Inspect logs, practise recovery, and record completion evidence.

The local route works immediately. Jira and GitHub are optional account-backed
parts; their exact setup is in **docs/JIRA_GITHUB.md**. Nothing has been pushed
to your accounts or published to a cloud host by creating this download.

## Commands you will use

| Command | What it does |
|---|---|
| `bash scripts/run.sh` | Builds and runs the local app at port 8080 |
| `bash scripts/test.sh` | Runs all acceptance, unit, storage, and HTTP checks |
| `bash scripts/prepare-ai-task.sh OG-101` | Creates a task packet for your AI tool |
| `bash scripts/pipeline.sh` | Tests, packages, and rehearses both release environments |
| `bash scripts/deploy-local.sh staging` | Runs the packaged app at port 8181 |
| `bash scripts/deploy-local.sh production` | Runs training production at port 8282 |
| `bash scripts/stop-local.sh staging` | Stops staging while preserving its orders |
| `bash scripts/stop-local.sh production` | Stops training production while preserving its orders |

If a port is occupied, stop the earlier OrderGuard terminal or managed process.
For the development app you can choose a different port:
`ORDERGUARD_PORT=8085 bash scripts/run.sh`.

Completing this project gives you practical SDLC experience. Expertise comes
from repeating the process on larger changes and learning your team's systems.
