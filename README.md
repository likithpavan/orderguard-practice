# OrderGuard — a complete ticket-to-release practice project

A Java 17 order-entry application with a browser screen, backend validation,
file-backed accepted-order ledger, tests, a real Git exercise, AI task packet,
Jira integration instructions, and local release and recovery scripts.

**Start with [START_HERE.md](START_HERE.md).** The first runnable command is
`bash scripts/run.sh`; then open http://localhost:8080.

## The ticket

**OG-101: Reject order quantities above 1,000.** Both BUY and SELL require a
whole number between 1 and 1000 inclusive. Rejection returns an error and adds
zero accepted records. If there are already five accepted orders, a rejected
request leaves five orders; it does not delete them or store a quantity of zero.

`main` / `starter/OG-101` contains the known upper-limit bug. The initial
download is checked out on `reference-solution`, which enforces the requirement.
`bash scripts/start-ticket.sh` creates your feature branch from the starter.

## Where the code lives

| File | Responsibility |
|---|---|
| `src/main/resources/public/index.html` | The order-entry screen |
| `src/main/resources/public/app.js` | Sends requests and renders the accepted ledger |
| `OrderGuardApplication.java` | Starts the HTTP server and registers routes |
| `OrderController.java` | Reads form input and sets the HTTP response |
| `OrderService.java` | Validates before creating and saving an accepted order |
| `OrderValidator.java` | Symbol, BUY/SELL, and quantity rules — OG-101 lives here |
| `OrderRepository.java` | Serialized writes to the local practice data file |
| `src/test/java/com/orderguard/OrderGuardTests.java` | Executable behaviour tests |
| `.github/workflows/ci-cd.yml` | GitHub CI and temporary release rehearsals |
| `tools/LocalDeploy.java` | Starts, stops, checks, and rolls back local releases |

Java source files in the table live in `src/main/java/com/orderguard/` unless
a complete path is shown. Use IntelliJ's Find in Files to search for `validate`,
`submit`, and `save`, then follow the request through these methods.

## Testing and packaging

```bash
bash scripts/test.sh
bash scripts/pipeline.sh
```

Tests run with a small standard-library runner, not JUnit. It returns a nonzero
exit code for failures and writes JUnit-compatible XML to
`build/reports/test-results.xml`. The pipeline stops when a check fails.
`--baseline` skips upper-limit acceptance tests; use it only to explore the
starter. CI and release commands always run the complete checks.

`dist/orderguard.jar` is the release artifact. `dist/release.properties` records
its version, source commit, and SHA-256 digest. Compile once and promote those
same bytes. Rebuilding the same source may produce a different JAR digest due
to archive metadata, so the release scripts copy the built artifact.

## API

| Request | Response |
|---|---|
| `GET /api/health` | HTTP 200 with environment, version, commit, and simulated flag |
| `GET /api/orders` | HTTP 200 with accepted orders and a consistent snapshot count |
| `POST /api/orders` | HTTP 201 on acceptance; HTTP 400 on invalid fields |
| `DELETE /api/orders` | Clears the simulated ledger in the running environment |

POST fields use `application/x-www-form-urlencoded`: `symbol`, `side`,
`quantity`. Responses use JSON. Supported symbols are AAPL, MSFT, and NVDA.

```bash
curl -i http://localhost:8080/api/orders \
  --data 'symbol=AAPL&side=BUY&quantity=1001'
curl http://localhost:8080/api/orders
```

The fixed version returns HTTP 400, a quantity error, and no added order.
Duplicate or unknown form fields are rejected. Oversized bodies return 413;
unsupported request content types return 415; unsupported methods return 405.

## Local environments

| Environment | Port | Data file |
|---|---:|---|
| Development | 8080 | `.runtime/local/orders.tsv` |
| Staging | 8181 | `.runtime/deploy/staging/orders.tsv` |
| Training production | 8282 | `.runtime/deploy/production/orders.tsv` |

```bash
bash scripts/package.sh
bash scripts/deploy-local.sh staging dist/orderguard.jar
bash scripts/status-local.sh staging
bash scripts/deploy-local.sh production dist/orderguard.jar
bash scripts/status-local.sh production
```

Managed environments keep separate data, processes, logs, and release records.
They continue running after the deployment command returns. Stop them with
`bash scripts/stop-local.sh staging` and `bash scripts/stop-local.sh production`.
Logs are in `.runtime/deploy/ENV/server.log`. A laptop reboot stops the servers;
run the deployment command again to restart with the same data.

Every candidate and rollback artifact is checked against OG-101 in an isolated
temporary store before changing the current deployment. The defective starter
cannot pass this release gate. Deployment is a brief stop/start, so it has
downtime. This is not a zero-downtime service or a cloud hosting platform.

## Scope

This is a single-machine learning project, bound to 127.0.0.1. It uses no
authentication, real trading, prices, broker routing, account limits, database
transactions across services, disaster-recovery service, or durable business
audit system. The file store supports one server per data file. Existing file
rows are reloaded, not retroactively validated against newer business rules.

GitHub jobs run on temporary runners and stop their simulated server before
completion. They demonstrate CI/CD gates and artifact promotion; they do not
host a persistent public application. Your local training production does run
persistently while your laptop and its Java process are running.

This is a teaching adaptation inspired by FINOS TraderX, independently written
for this exercise. It is not Goldman Sachs software or a description of their
internal delivery process. See [docs/SOURCES.md](docs/SOURCES.md).
