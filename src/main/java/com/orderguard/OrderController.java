package com.orderguard;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public final class OrderController implements HttpHandler {
    private static final int MAX_BODY_BYTES = 4096;
    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }

    @Override public void handle(HttpExchange exchange) throws IOException {
        String requestId = UUID.randomUUID().toString();
        exchange.getResponseHeaders().set("X-Request-Id", requestId);
        if (!exchange.getRequestURI().getPath().equals("/api/orders")) {
            HttpSupport.error(exchange, 404, "Route not found."); return;
        }
        try {
            switch (exchange.getRequestMethod()) {
                case "GET" -> {
                    var snapshot = service.listOrders();
                    String orders = snapshot.stream().map(Order::toJson).collect(Collectors.joining(","));
                    HttpSupport.json(exchange, 200, "{\"count\":" + snapshot.size() + ",\"orders\":[" + orders + "]}");
                }
                case "POST" -> {
                    String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                    if (contentType == null || !contentType.toLowerCase(Locale.ROOT).split(";", 2)[0].trim().equals("application/x-www-form-urlencoded")) {
                        HttpSupport.error(exchange, 415, "Send application/x-www-form-urlencoded order fields."); return;
                    }
                    byte[] bytes = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
                    if (bytes.length > MAX_BODY_BYTES) {
                        HttpSupport.error(exchange, 413, "Order request is too large."); return;
                    }
                    var fields = HttpSupport.decodeForm(new String(bytes, StandardCharsets.UTF_8));
                    Order order = service.submit(new OrderRequest(fields.get("symbol"), fields.get("side"), fields.get("quantity")));
                    HttpSupport.json(exchange, 201, "{\"status\":\"ACCEPTED\",\"message\":\"Practice order accepted.\",\"order\":" + order.toJson() + "}");
                    System.out.printf("%s request=%s result=ACCEPTED order=%s side=%s quantity=%d%n", Instant.now(), requestId, order.id(), order.side(), order.quantity());
                }
                case "DELETE" -> {
                    service.clearPracticeOrders();
                    HttpSupport.json(exchange, 200, "{\"message\":\"Practice orders cleared.\",\"count\":0}");
                }
                default -> {
                    exchange.getResponseHeaders().set("Allow", "GET, POST, DELETE");
                    HttpSupport.error(exchange, 405, "Method not allowed.");
                }
            }
        } catch (IllegalArgumentException exception) {
            String message = exception instanceof ValidationException ? exception.getMessage() : "Malformed order input.";
            HttpSupport.error(exchange, 400, message);
            System.out.printf("%s request=%s result=REJECTED%n", Instant.now(), requestId);
        } catch (IOException exception) {
            System.err.printf("%s request=%s result=ERROR type=%s%n", Instant.now(), requestId, exception.getClass().getSimpleName());
            try { HttpSupport.error(exchange, 500, "The order could not be processed. Check the server log."); }
            catch (IOException ignored) { /* Client may already have disconnected. */ }
        } finally { exchange.close(); }
    }
}
