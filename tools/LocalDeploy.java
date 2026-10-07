import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/** Managed local training environments. Never invokes a broker or a cloud provider. */
class LocalDeploy {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
    private static Path root;
    private static String environment;
    private static int port;

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || !args[1].matches("staging|production") || !args[0].matches("deploy|stop|rollback|status")) {
            throw new IllegalArgumentException("Usage: LocalDeploy deploy|stop|rollback|status staging|production [JAR]");
        }
        environment = args[1];
        port = environment.equals("staging") ? 8181 : 8282;
        root = Path.of(".runtime/deploy", environment).toAbsolutePath();
        Files.createDirectories(root);
        restrict(root, true);
        try (var channel = FileChannel.open(root.resolve("operation.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.tryLock()) {
            if (lock == null) throw new IllegalStateException("Another deployment operation is running for " + environment);
            switch (args[0]) {
                case "status" -> status();
                case "stop" -> { stop(load("active.properties")); Files.deleteIfExists(root.resolve("active.properties")); System.out.println(environment + " stopped. Order data preserved."); }
                case "rollback" -> {
                    Properties previous = load("previous.properties");
                    if (previous.isEmpty()) throw new IllegalStateException("No previous release. Keep submissions stopped and fix forward.");
                    deploy(Path.of(previous.getProperty("jar")));
                }
                case "deploy" -> {
                    if (args.length != 3) throw new IllegalArgumentException("Provide the JAR to deploy.");
                    deploy(Path.of(args[2]).toAbsolutePath());
                }
            }
        }
    }

    private static void deploy(Path candidate) throws Exception {
        if (!Files.isRegularFile(candidate)) throw new IllegalArgumentException("JAR not found: " + candidate);
        // Runs in an isolated temporary store before touching the current process or its data.
        guardCheck(candidate);
        String hash = digest(candidate);
        Properties current = load("active.properties");
        if (hash.equals(current.getProperty("sha256")) && healthy(current)) {
            System.out.println(environment + " already runs this artifact at http://127.0.0.1:" + port);
            return;
        }
        Path release = root.resolve("releases").resolve(hash).resolve("orderguard.jar");
        Files.createDirectories(release.getParent());
        if (!candidate.equals(release)) Files.copy(candidate, release, StandardCopyOption.REPLACE_EXISTING);
        if (!digest(release).equals(hash)) throw new IllegalStateException("Artifact copy failed its digest check.");
        stop(current);
        Files.deleteIfExists(root.resolve("active.properties"));
        try {
            Properties next = start(release, hash);
            if (!current.isEmpty()) save("previous.properties", current);
            save("active.properties", next);
            System.out.println("Deployed " + environment + " at http://127.0.0.1:" + port + "\nsha256=" + hash + "\nlog=" + root.resolve("server.log"));
        } catch (Exception failure) {
            System.err.println("Candidate failed to start. " + failure.getMessage());
            if (!current.isEmpty()) {
                try {
                    Path prior = ownedJar(current);
                    guardCheck(prior); // A defective baseline must not be restored.
                    save("active.properties", start(prior, current.getProperty("sha256")));
                    System.err.println("Previous guarded release restored. Data preserved.");
                } catch (Exception restoreFailure) { System.err.println("Recovery blocked: " + restoreFailure.getMessage() + ". Keep submissions stopped and fix forward."); }
            }
            throw failure;
        }
    }

    private static Properties start(Path jar, String hash) throws Exception {
        var builder = new ProcessBuilder(javaExecutable(), "--add-modules", "jdk.httpserver", "-jar", jar.toString());
        String instance = java.util.UUID.randomUUID().toString();
        String token = java.util.UUID.randomUUID().toString() + java.util.UUID.randomUUID();
        builder.environment().put("ORDERGUARD_PORT", Integer.toString(port));
        builder.environment().put("ORDERGUARD_ENV", environment);
        builder.environment().put("ORDERGUARD_DATA", root.resolve("orders.tsv").toString());
        builder.environment().put("ORDERGUARD_INSTANCE_ID", instance);
        builder.environment().put("ORDERGUARD_CONTROL_TOKEN", token);
        Path log = root.resolve("server.log");
        long logOffset = Files.exists(log) ? Files.size(log) : 0;
        builder.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()));
        Process child = builder.start();
        Properties state = new Properties();
        state.setProperty("pid", Long.toString(child.pid()));
        state.setProperty("jar", jar.toString());
        state.setProperty("sha256", hash);
        state.setProperty("instance", instance);
        state.setProperty("controlToken", token);
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (System.nanoTime() < deadline) {
            if (!child.isAlive()) throw new IllegalStateException("Server exited. See " + root.resolve("server.log"));
            byte[] logBytes = Files.exists(log) ? Files.readAllBytes(log) : new byte[0];
            String newLog = logBytes.length > logOffset
                ? new String(logBytes, (int)logOffset, logBytes.length - (int)logOffset, java.nio.charset.StandardCharsets.UTF_8) : "";
            if (newLog.contains("http://127.0.0.1:" + port + " ") && healthy(state)) return state;
            Thread.sleep(150);
        }
        child.destroy();
        if (!child.waitFor(5, TimeUnit.SECONDS)) child.destroyForcibly();
        throw new IllegalStateException("Health check timed out. Check that port " + port + " is free.");
    }

    private static void guardCheck(Path jar) throws Exception {
        var process = new ProcessBuilder(javaExecutable(), "tools/ReleaseCheck.java", jar.toString(), environment).inheritIO().start();
        if (process.waitFor() != 0) throw new IllegalStateException("Release guard check failed. Current deployment was not changed.");
    }
    private static void stop(Properties state) throws Exception {
        if (state.isEmpty()) return;
        ownedJar(state);
        String body = healthBody();
        if (body == null) return;
        if (!body.contains("\"instanceId\":\"" + state.getProperty("instance") + "\"")) {
            throw new IllegalStateException("The port belongs to another server instance; no process stopped.");
        }
        var response = CLIENT.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/internal/stop"))
            .timeout(Duration.ofSeconds(3)).header("X-OrderGuard-Control", state.getProperty("controlToken", ""))
            .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IllegalStateException("Managed stop was refused. No unrelated process was stopped.");
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            if (healthBody() == null) return;
            Thread.sleep(100);
        }
        throw new IllegalStateException("Managed server did not stop; check its log.");
    }
    private static boolean healthy(Properties state) {
        if (state.isEmpty()) return false;
        try {
            Path jar = ownedJar(state);
            Properties build = new Properties();
            try (var file = new java.util.jar.JarFile(jar.toFile()); var input = file.getInputStream(file.getJarEntry("build-info.properties"))) { build.load(input); }
            var response = CLIENT.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/health")).timeout(Duration.ofSeconds(1)).GET().build(), HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && response.body().contains("\"environment\":\"" + environment + "\"")
                && response.body().contains("\"instanceId\":\"" + state.getProperty("instance") + "\"")
                && response.body().contains("\"version\":\"" + build.getProperty("version") + "\"")
                && response.body().contains("\"commit\":\"" + build.getProperty("commit") + "\"");
        } catch (Exception exception) { return false; }
    }
    private static String healthBody() {
        try {
            var response = CLIENT.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/health")).timeout(Duration.ofSeconds(1)).GET().build(), HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (java.net.ConnectException exception) { return null; }
        catch (Exception exception) { throw new IllegalStateException("Cannot confirm whether the existing server stopped.", exception); }
    }
    private static Path ownedJar(Properties state) {
        Path jar = Path.of(state.getProperty("jar")).toAbsolutePath().normalize();
        Path expected = root.resolve("releases").resolve(state.getProperty("sha256", "missing")).resolve("orderguard.jar").normalize();
        if (!jar.equals(expected) || !jar.startsWith(root.resolve("releases"))) throw new IllegalStateException("Unexpected release state. No process stopped.");
        return jar;
    }
    private static void status() throws Exception {
        Properties state = load("active.properties");
        System.out.println(environment + ": " + (healthy(state) ? "UP" : "STOPPED or unhealthy") + "\nurl=http://127.0.0.1:" + port + "\nsha256=" + state.getProperty("sha256", "none") + "\ndata=" + root.resolve("orders.tsv"));
    }
    private static Properties load(String name) throws Exception {
        Properties state = new Properties();
        Path file = root.resolve(name);
        if (Files.exists(file)) try (var input = Files.newInputStream(file)) { state.load(input); }
        return state;
    }
    private static void save(String name, Properties state) throws Exception {
        Path temporary = root.resolve(name + ".tmp");
        try (var output = Files.newOutputStream(temporary)) { state.store(output, "Local OrderGuard training deployment"); }
        Files.move(temporary, root.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        restrict(root.resolve(name), false);
    }
    private static void restrict(Path path, boolean directory) throws Exception {
        try { Files.setPosixFilePermissions(path, java.nio.file.attribute.PosixFilePermissions.fromString(directory ? "rwx------" : "rw-------")); }
        catch (UnsupportedOperationException ignored) { /* The local runner also works on file systems without POSIX permissions. */ }
    }
    private static String digest(Path jar) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))); }
    private static String javaExecutable() { return Path.of(System.getProperty("java.home"), "bin", "java").toString(); }
}
