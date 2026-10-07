package com.orderguard;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/** Dependency-free test runner. It tests behaviour and writes a JUnit-compatible XML report. */
public final class OrderGuardTests {
    @FunctionalInterface interface Check { void run() throws Exception; }
    private record Result(String name, String error, double seconds) {}
    private static final List<Result> RESULTS = new ArrayList<>();
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private static boolean baseline;

    public static void main(String[] args) throws Exception {
        baseline = args.length == 1 && args[0].equals("--baseline");
        if (args.length > 0 && !baseline) throw new IllegalArgumentException("Only optional argument: --baseline");
        if (baseline) System.out.println("BASELINE ONLY: upper-limit acceptance tests are skipped. This is not release approval.");
        validatorTests();
        serviceTests();
        repositoryTests();
        apiTests();
        managedControlTests();
        Path report = Path.of("build/reports/test-results.xml");
        Files.createDirectories(report.getParent());
        long failures = RESULTS.stream().filter(result -> result.error() != null).count();
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<testsuite name=\"OrderGuard\" tests=\"")
            .append(RESULTS.size()).append("\" failures=\"").append(failures).append("\">\n");
        for (Result result : RESULTS) {
            xml.append("  <testcase classname=\"OrderGuardTests\" name=\"").append(escape(result.name())).append("\" time=\"")
                .append(result.seconds()).append("\">");
            if (result.error() != null) xml.append("<failure message=\"").append(escape(result.error())).append("\"/>");
            xml.append("</testcase>\n");
        }
        Files.writeString(report, xml.append("</testsuite>\n"));
        System.out.printf("%nRESULT: %d passed, %d failed. Report: %s%n", RESULTS.size() - failures, failures, report);
        if (failures != 0) System.exit(1);
    }

    private static void validatorTests() {
        for (String side : List.of("BUY", "SELL")) {
            for (String quantity : List.of("1", "500", "1000")) test("validator accepts " + side + " " + quantity, () -> {
                var result = new OrderValidator().validate(new OrderRequest("AAPL", side, quantity));
                equal(Integer.parseInt(quantity), result.quantity(), "Accepted quantity changed");
                equal(side, result.side(), "Side changed");
            });
            for (String quantity : List.of("0", "-1", "1.5", "1000.0", "abc", "", " ", "+1", "1e3", "999999999999999999999", "NaN")) {
                test("validator rejects " + side + " [" + quantity + "]", () ->
                    rejects(() -> new OrderValidator().validate(new OrderRequest("AAPL", side, quantity))));
            }
            if (!baseline) for (String quantity : List.of("1001", "5000", "2147483647")) {
                test("OG-101 validator rejects " + side + " " + quantity, () ->
                    rejects(() -> new OrderValidator().validate(new OrderRequest("AAPL", side, quantity))));
            }
        }
        test("validator rejects a missing quantity", () -> rejects(() -> new OrderValidator().validate(new OrderRequest("AAPL", "BUY", null))));
        test("validator rejects a missing order", () -> rejects(() -> new OrderValidator().validate(null)));
        for (String symbol : List.of("", "BAD", "AAPL<script>")) test("validator rejects symbol " + symbol, () ->
            rejects(() -> new OrderValidator().validate(new OrderRequest(symbol, "BUY", "1"))));
        for (String side : List.of("", "HOLD", "BUYSELL")) test("validator rejects side " + side, () ->
            rejects(() -> new OrderValidator().validate(new OrderRequest("AAPL", side, "1"))));
        test("validator normalizes surrounding whitespace and case", () -> {
            var order = new OrderValidator().validate(new OrderRequest(" msft ", " sell ", " 1000 "));
            equal("MSFT", order.symbol(), "Symbol normalization");
            equal("SELL", order.side(), "Side normalization");
            equal(1000, order.quantity(), "Quantity normalization");
        });
    }

    private static void serviceTests() {
        test("service stores exactly one accepted order", () -> {
            var repository = OrderRepository.inMemory();
            var service = service(repository);
            Order order = service.submit(new OrderRequest("NVDA", "BUY", "1000"));
            equal(1, repository.count(), "Accepted count");
            equal(order, repository.findAll().get(0), "Stored order");
            truth(order.id() != null && order.createdAt() != null, "Identity and creation time required");
        });
        test("service creates distinct order IDs", () -> {
            var service = service(OrderRepository.inMemory());
            var first = service.submit(new OrderRequest("AAPL", "BUY", "1"));
            var second = service.submit(new OrderRequest("AAPL", "BUY", "1"));
            truth(!first.id().equals(second.id()), "Two submitted orders need distinct IDs");
        });
        if (!baseline) for (String side : List.of("BUY", "SELL")) {
            test("OG-101 rejected " + side + " order adds zero to empty store", () -> {
                var repository = OrderRepository.inMemory();
                var service = service(repository);
                rejects(() -> service.submit(new OrderRequest("AAPL", side, "1001")));
                equal(0, repository.count(), "Rejected order was stored");
            });
            test("OG-101 rejected " + side + " order preserves existing orders", () -> {
                var repository = OrderRepository.inMemory();
                var service = service(repository);
                var accepted = service.submit(new OrderRequest("AAPL", "BUY", "1000"));
                rejects(() -> service.submit(new OrderRequest("AAPL", side, "5000")));
                equal(List.of(accepted), repository.findAll(), "Existing accepted order changed");
            });
        }
    }

    private static void repositoryTests() {
        test("file storage survives a restart", () -> withTemporaryDirectory(directory -> {
            Path file = directory.resolve("orders.tsv");
            var repository = new OrderRepository(file);
            var accepted = service(repository).submit(new OrderRequest("AAPL", "SELL", "1000"));
            equal(List.of(accepted), new OrderRepository(file).findAll(), "Reloaded data differs");
        }));
        test("clear persists an empty ledger", () -> withTemporaryDirectory(directory -> {
            Path file = directory.resolve("orders.tsv");
            var repository = new OrderRepository(file);
            service(repository).submit(new OrderRequest("AAPL", "BUY", "1"));
            repository.clear();
            equal(0, new OrderRepository(file).count(), "Cleared records returned after restart");
        }));
        if (!baseline) test("OG-101 rejection leaves persisted bytes unchanged", () -> withTemporaryDirectory(directory -> {
            Path file = directory.resolve("orders.tsv");
            var service = service(new OrderRepository(file));
            service.submit(new OrderRequest("AAPL", "BUY", "1"));
            String before = Files.readString(file);
            rejects(() -> service.submit(new OrderRequest("AAPL", "SELL", "1001")));
            equal(before, Files.readString(file), "A rejected request wrote to the file");
        }));
        test("damaged data fails visibly and is preserved", () -> withTemporaryDirectory(directory -> {
            Path file = directory.resolve("orders.tsv");
            Files.writeString(file, "damaged data\n");
            throwsType(IOException.class, () -> new OrderRepository(file));
            equal("damaged data\n", Files.readString(file), "Damaged file overwritten");
        }));
        test("failed persistence does not report a stored order", () -> withTemporaryDirectory(directory -> {
            Path file = directory.resolve("orders.tsv");
            var repository = new OrderRepository(file);
            Files.createDirectory(file); // Forces a write failure without OS-specific permissions.
            throwsType(IOException.class, () -> service(repository).submit(new OrderRequest("AAPL", "BUY", "1")));
            equal(0, repository.count(), "Failed write changed the in-memory count");
        }));
    }

    private static void apiTests() {
        test("HTTP health identifies the running release", () -> withServer((base, repository) -> {
            var response = get(base, "/api/health");
            equal(200, response.statusCode(), "Health status");
            contains(response.body(), "\"version\":\"test\"");
            contains(response.body(), "\"environment\":\"local\"");
            contains(response.body(), "\"simulated\":true");
        }));
        for (String path : List.of("/", "/index.html", "/app.js", "/styles.css")) test("HTTP serves " + path, () -> withServer((base, repository) -> {
            var response = get(base, path);
            equal(200, response.statusCode(), "Static response");
            truth(!response.body().isEmpty(), "Empty resource");
            equal("nosniff", response.headers().firstValue("X-Content-Type-Options").orElse(""), "Header");
        }));
        for (String path : List.of("/api/orders/anything", "/api/health-extra", "/unknown", "/VERSION")) test("HTTP rejects unknown route " + path, () -> withServer((base, repository) ->
            equal(404, get(base, path).statusCode(), "Unknown route response")));
        for (String side : List.of("BUY", "SELL")) {
            for (String quantity : List.of("1", "1000")) test("HTTP accepts " + side + " " + quantity, () -> withServer((base, repository) -> {
                var response = post(base, "symbol=AAPL&side=" + side + "&quantity=" + quantity, "application/x-www-form-urlencoded");
                equal(201, response.statusCode(), "Accepted HTTP status");
                contains(response.body(), "\"status\":\"ACCEPTED\"");
                contains(response.body(), "\"quantity\":" + quantity);
                truth(response.headers().firstValue("X-Request-Id").isPresent(), "Missing request ID");
                equal(1, repository.count(), "Accepted HTTP request not stored exactly once");
            }));
            if (!baseline) test("OG-101 HTTP rejects " + side + " 1001 without storing it", () -> withServer((base, repository) -> {
                equal(201, post(base, "symbol=AAPL&side=BUY&quantity=1000", "application/x-www-form-urlencoded").statusCode(), "Seed order");
                var response = post(base, "symbol=AAPL&side=" + side + "&quantity=1001", "application/x-www-form-urlencoded");
                equal(400, response.statusCode(), "Invalid HTTP status");
                contains(response.body(), "from 1 to 1000");
                equal(1, repository.count(), "Rejected order increased count");
                contains(get(base, "/api/orders").body(), "\"count\":1");
            }));
        }
        for (String body : List.of("symbol=AAPL&side=BUY", "symbol=AAPL&side=BUY&quantity=0", "symbol=AAPL&side=SELL&quantity=-1", "symbol=AAPL&side=BUY&quantity=1.5", "symbol=AAPL&side=BUY&quantity=2&quantity=3", "symbol=AAPL&side=BUY&quantity=2&extra=x", "symbol=%ZZ&side=BUY&quantity=2")) {
            test("HTTP rejects malformed form: " + body, () -> withServer((base, repository) -> {
                equal(400, post(base, body, "application/x-www-form-urlencoded").statusCode(), "Malformed form response");
                equal(0, repository.count(), "Malformed request stored an order");
            }));
        }
        test("HTTP rejects unsupported request content", () -> withServer((base, repository) ->
            equal(415, post(base, "{}", "application/json").statusCode(), "Content type response")));
        test("HTTP rejects oversized requests before storage", () -> withServer((base, repository) -> {
            equal(413, post(base, "quantity=" + "1".repeat(5000), "application/x-www-form-urlencoded").statusCode(), "Body limit response");
            equal(0, repository.count(), "Oversized request stored an order");
        }));
        test("HTTP rejects an unsupported method", () -> withServer((base, repository) -> {
            var response = CLIENT.send(HttpRequest.newBuilder(URI.create(base + "/api/orders")).method("PUT", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
            equal(405, response.statusCode(), "Unsupported method status");
            contains(response.headers().firstValue("Allow").orElse(""), "POST");
        }));
        test("HTTP clears only the practice ledger", () -> withServer((base, repository) -> {
            post(base, "symbol=AAPL&side=BUY&quantity=1", "application/x-www-form-urlencoded");
            var response = CLIENT.send(HttpRequest.newBuilder(URI.create(base + "/api/orders")).DELETE().build(), HttpResponse.BodyHandlers.ofString());
            equal(200, response.statusCode(), "Clear status");
            equal(0, repository.count(), "Clear did not remove practice data");
        }));
        test("HTTP concurrent accepted requests keep distinct records", () -> withServer((base, repository) -> {
            var executor = Executors.newFixedThreadPool(6);
            try {
                List<java.util.concurrent.Future<Integer>> responses = new ArrayList<>();
                for (int i = 0; i < 24; i++) responses.add(executor.submit(() -> post(base, "symbol=MSFT&side=SELL&quantity=1", "application/x-www-form-urlencoded").statusCode()));
                for (var response : responses) equal(201, response.get(), "Concurrent response");
                equal(24, repository.count(), "Lost concurrent orders");
                equal(24L, repository.findAll().stream().map(Order::id).distinct().count(), "Duplicate order IDs");
                contains(get(base, "/api/orders").body(), "\"count\":24");
            } finally { executor.shutdownNow(); }
        }));
    }

    private static OrderService service(OrderRepository repository) { return new OrderService(new OrderValidator(), repository); }
    private static void managedControlTests() {
        test("managed stop route is absent from the normal development app", () -> withServer((base, repository) ->
            equal(404, postAt(base, "/internal/stop", "wrong-token").statusCode(), "Unexpected control route")));
        test("managed stop requires its private token and never exposes it in health", () -> {
            String token = "test-control-token-at-least-24-characters";
            try (var app = new OrderGuardApplication(0, "staging", OrderRepository.inMemory(), new BuildInfo("test", "commit"), "test-instance", token)) {
                app.start();
                String base = "http://127.0.0.1:" + app.port();
                var health = get(base, "/api/health");
                contains(health.body(), "test-instance");
                truth(!health.body().contains(token), "Control token exposed in health");
                equal(403, postAt(base, "/internal/stop", "wrong-token").statusCode(), "Wrong token accepted");
                equal(200, get(base, "/api/health").statusCode(), "Unauthorized request stopped the app");
                equal(404, postAt(base, "/internal/stop-extra", token).statusCode(), "Prefix route accepted");
            }
        });
        test("managed stop shuts down only its own training instance", () -> {
            String token = "another-test-control-token-at-least-24";
            try (var app = new OrderGuardApplication(0, "production", OrderRepository.inMemory(), new BuildInfo("test", "commit"), "test-instance", token)) {
                app.start();
                String base = "http://127.0.0.1:" + app.port();
                equal(200, postAt(base, "/internal/stop", token).statusCode(), "Managed stop status");
                long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
                while (System.nanoTime() < deadline) {
                    try { get(base, "/api/health"); }
                    catch (IOException stopped) { return; }
                    Thread.sleep(50);
                }
                throw new AssertionError("Managed instance still answers health requests");
            }
        });
    }
    private static HttpResponse<String> postAt(String base, String path, String token) throws Exception {
        return CLIENT.send(HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(5)).header("X-OrderGuard-Control", token)
            .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
    }
    @FunctionalInterface interface ServerCheck { void run(String base, OrderRepository repository) throws Exception; }
    private static void withServer(ServerCheck check) throws Exception {
        var repository = OrderRepository.inMemory();
        try (var app = new OrderGuardApplication(0, "local", repository, new BuildInfo("test", "test-commit"))) {
            app.start();
            check.run("http://127.0.0.1:" + app.port(), repository);
        }
    }
    @FunctionalInterface interface DirectoryCheck { void run(Path directory) throws Exception; }
    private static void withTemporaryDirectory(DirectoryCheck check) throws Exception {
        Path directory = Files.createTempDirectory("orderguard-test-");
        try { check.run(directory); }
        finally { try (var files = Files.walk(directory)) { for (Path path : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); } }
    }
    private static HttpResponse<String> get(String base, String path) throws Exception {
        return CLIENT.send(HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private static HttpResponse<String> post(String base, String body, String contentType) throws Exception {
        return CLIENT.send(HttpRequest.newBuilder(URI.create(base + "/api/orders")).timeout(Duration.ofSeconds(5)).header("Content-Type", contentType)
            .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private static void test(String name, Check check) {
        long started = System.nanoTime();
        String error = null;
        try { check.run(); System.out.println("PASS " + name); }
        catch (Exception | AssertionError failure) { error = failure.toString(); System.err.println("FAIL " + name + " → " + error); }
        RESULTS.add(new Result(name, error, (System.nanoTime() - started) / 1_000_000_000.0));
    }
    private static void rejects(Check check) throws Exception { throwsType(ValidationException.class, check); }
    private static void throwsType(Class<? extends Exception> type, Check check) throws Exception {
        try { check.run(); }
        catch (Exception exception) { if (type.isInstance(exception)) return; throw exception; }
        throw new AssertionError("Expected " + type.getSimpleName() + " but request succeeded");
    }
    private static void equal(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) throw new AssertionError(message + ": expected " + expected + ", got " + actual);
    }
    private static void truth(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void contains(String actual, String expected) { truth(actual.contains(expected), "Missing " + expected + " in " + actual); }
    private static String escape(String value) { return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "&#10;").replace("\r", "&#13;"); }
}
