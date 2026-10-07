package com.orderguard;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class OrderGuardApplication implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor;
    private boolean closed;
    public static final String HOST = "127.0.0.1";

    public OrderGuardApplication(int port, String environment, OrderRepository repository, BuildInfo build) throws IOException {
        this(port, environment, repository, build, UUID.randomUUID().toString(), "");
    }

    public OrderGuardApplication(int port, String environment, OrderRepository repository, BuildInfo build, String instanceId, String controlToken) throws IOException {
        OrderService service = new OrderService(new OrderValidator(), repository);
        server = HttpServer.create(new InetSocketAddress(HOST, port), 32);
        executor = Executors.newFixedThreadPool(4);
        server.setExecutor(executor);
        server.createContext("/api/orders", new OrderController(service));
        server.createContext("/api/health", exchange -> {
            if (!exchange.getRequestURI().getPath().equals("/api/health")) { HttpSupport.error(exchange, 404, "Route not found."); return; }
            if (!exchange.getRequestMethod().equals("GET")) { HttpSupport.error(exchange, 405, "Method not allowed."); return; }
            HttpSupport.json(exchange, 200, "{\"status\":\"UP\",\"environment\":" + HttpSupport.quote(environment)
                + ",\"version\":" + HttpSupport.quote(build.version()) + ",\"commit\":" + HttpSupport.quote(build.commit())
                + ",\"instanceId\":" + HttpSupport.quote(instanceId)
                + ",\"requiredQuantityLimit\":1000,\"simulated\":true}");
        });
        if (controlToken.length() >= 24) server.createContext("/internal/stop", exchange -> {
            try {
                if (!exchange.getRequestURI().getPath().equals("/internal/stop")) { HttpSupport.error(exchange, 404, "Route not found."); return; }
                if (!exchange.getRequestMethod().equals("POST")) { HttpSupport.error(exchange, 405, "Method not allowed."); return; }
                String supplied = exchange.getRequestHeaders().getFirst("X-OrderGuard-Control");
                if (supplied == null || !MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8), controlToken.getBytes(StandardCharsets.UTF_8))) {
                    HttpSupport.error(exchange, 403, "Managed environment control denied."); return;
                }
                HttpSupport.json(exchange, 200, "{\"status\":\"STOPPING\"}");
                new Thread(this::close, "orderguard-managed-stop").start();
            } finally { exchange.close(); }
        });
        server.createContext("/", exchange -> {
            Map<String, String> files = Map.of("/", "index.html", "/index.html", "index.html", "/app.js", "app.js", "/styles.css", "styles.css");
            String file = files.get(exchange.getRequestURI().getPath());
            if (file == null) { HttpSupport.error(exchange, 404, "Route not found."); return; }
            if (!exchange.getRequestMethod().equals("GET")) { HttpSupport.error(exchange, 405, "Method not allowed."); return; }
            try (var resource = OrderGuardApplication.class.getResourceAsStream("/public/" + file)) {
                if (resource == null) { HttpSupport.error(exchange, 500, "Application resource missing."); return; }
                String contentType = file.endsWith(".css") ? "text/css; charset=utf-8" : file.endsWith(".js") ? "text/javascript; charset=utf-8" : "text/html; charset=utf-8";
                HttpSupport.send(exchange, 200, contentType, resource.readAllBytes());
            }
        });
    }

    public void start() { server.start(); }
    public int port() { return server.getAddress().getPort(); }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        server.stop(0);
        executor.shutdownNow();
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("ORDERGUARD_PORT", "8080"));
        String environment = System.getenv().getOrDefault("ORDERGUARD_ENV", "local");
        if (!environment.matches("local|staging|production")) throw new IllegalArgumentException("Choose local, staging, or production.");
        Path data = Path.of(System.getenv().getOrDefault("ORDERGUARD_DATA", ".runtime/" + environment + "/orders.tsv"));
        String instance = System.getenv().getOrDefault("ORDERGUARD_INSTANCE_ID", UUID.randomUUID().toString());
        String control = System.getenv().getOrDefault("ORDERGUARD_CONTROL_TOKEN", "");
        OrderGuardApplication app = new OrderGuardApplication(port, environment, new OrderRepository(data), BuildInfo.load(), instance, control);
        Runtime.getRuntime().addShutdownHook(new Thread(app::close));
        app.start();
        System.out.printf("OrderGuard %s (%s) is running at http://%s:%d — simulated orders only.%n", BuildInfo.load().version(), environment, HOST, app.port());
    }
}
