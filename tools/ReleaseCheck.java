import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/** Starts the actual JAR with disposable data, tests it over HTTP, then shuts it down. */
class ReleaseCheck {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !args[1].matches("local|staging|production")) throw new IllegalArgumentException("Usage: java tools/ReleaseCheck.java JAR local|staging|production");
        Path jar = Path.of(args[0]).toAbsolutePath();
        String environment = args[1];
        Properties build = new Properties();
        try (JarFile file = new JarFile(jar.toFile()); var input = file.getInputStream(file.getJarEntry("build-info.properties"))) { build.load(input); }
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar)));
        Path scratch = Files.createTempDirectory("orderguard-release-check-");
        Path log = scratch.resolve("server.log");
        Process child = null;
        try {
            var builder = new ProcessBuilder(javaExecutable(), "--add-modules", "jdk.httpserver", "-jar", jar.toString());
            builder.environment().put("ORDERGUARD_PORT", "0");
            builder.environment().put("ORDERGUARD_ENV", environment);
            builder.environment().put("ORDERGUARD_DATA", scratch.resolve("orders.tsv").toString());
            builder.redirectErrorStream(true).redirectOutput(log.toFile());
            child = builder.start();
            String base = awaitAddress(child, log);
            var health = request(base, "/api/health", "GET", null);
            require(health.statusCode() == 200, "Health failed");
            require(health.body().contains("\"environment\":\"" + environment + "\""), "Wrong environment");
            require(health.body().contains("\"version\":\"" + build.getProperty("version") + "\""), "Wrong version");
            require(health.body().contains("\"commit\":\"" + build.getProperty("commit") + "\""), "Wrong commit");
            require(request(base, "/", "GET", null).statusCode() == 200, "Order desk did not load");
            int count = 0;
            for (String side : new String[]{"BUY", "SELL"}) {
                for (String quantity : new String[]{"1", "1000"}) {
                    require(request(base, "/api/orders", "POST", "symbol=AAPL&side=" + side + "&quantity=" + quantity).statusCode() == 201, side + " " + quantity + " must be accepted");
                    count++;
                }
                for (String quantity : new String[]{"1001", "5000", "0", "-1", "1.5"}) {
                    var rejected = request(base, "/api/orders", "POST", "symbol=AAPL&side=" + side + "&quantity=" + quantity);
                    require(rejected.statusCode() == 400, side + " " + quantity + " must be rejected; got " + rejected.statusCode());
                    require(request(base, "/api/orders", "GET", null).body().contains("\"count\":" + count + ","), "Rejected request changed the count");
                }
            }
            String report = "PASS — packaged JAR release check\nenvironment=" + environment + "\nversion=" + build.getProperty("version")
                + "\ncommit=" + build.getProperty("commit") + "\nsha256=" + hash + "\nchecks=health, UI, BUY/SELL boundaries, rejection without persistence\n";
            Path reports = Path.of("build/reports");
            Files.createDirectories(reports);
            Files.writeString(reports.resolve("release-" + environment + ".txt"), report);
            System.out.print(report);
        } finally {
            if (child != null) {
                child.destroy();
                if (!child.waitFor(5, TimeUnit.SECONDS)) { child.destroyForcibly(); child.waitFor(5, TimeUnit.SECONDS); }
            }
            try (var paths = Files.walk(scratch)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); }
        }
    }
    private static String javaExecutable() { return Path.of(System.getProperty("java.home"), "bin", "java").toString(); }
    private static String awaitAddress(Process child, Path log) throws Exception {
        var pattern = Pattern.compile("http://127\\.0\\.0\\.1:(\\d+)");
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        while (System.nanoTime() < deadline) {
            String contents = Files.exists(log) ? Files.readString(log) : "";
            var match = pattern.matcher(contents);
            if (match.find()) return match.group();
            if (!child.isAlive()) throw new IllegalStateException("JAR failed to start: " + contents);
            Thread.sleep(100);
        }
        throw new IllegalStateException("Timed out starting JAR. " + Files.readString(log));
    }
    private static HttpResponse<String> request(String base, String path, String method, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(5));
        if (body == null) request.method(method, HttpRequest.BodyPublishers.noBody());
        else request.header("Content-Type", "application/x-www-form-urlencoded").method(method, HttpRequest.BodyPublishers.ofString(body));
        return CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
