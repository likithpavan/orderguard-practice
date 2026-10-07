package com.orderguard;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class HttpSupport {
    private HttpSupport() {}

    public static String quote(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> { if (c < 32) escaped.append(String.format("\\u%04x", (int)c)); else escaped.append(c); }
            }
        }
        return escaped.append('"').toString();
    }

    public static void json(HttpExchange exchange, int status, String body) throws IOException {
        send(exchange, status, "application/json; charset=utf-8", body.getBytes(StandardCharsets.UTF_8));
    }

    public static void send(HttpExchange exchange, int status, String contentType, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; frame-ancestors 'none'; base-uri 'none'");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) { output.write(bytes); }
    }

    public static void error(HttpExchange exchange, int status, String message) throws IOException {
        json(exchange, status, "{\"status\":\"REJECTED\",\"message\":" + quote(message) + "}");
    }

    public static Map<String, String> decodeForm(String body) {
        Map<String, String> fields = new LinkedHashMap<>();
        Set<String> allowed = Set.of("symbol", "side", "quantity");
        if (body.isEmpty()) return fields;
        for (String part : body.split("&", -1)) {
            String[] pair = part.split("=", 2);
            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            String value = pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
            if (!allowed.contains(key) || fields.putIfAbsent(key, value) != null) {
                throw new ValidationException("Use each of symbol, side, and quantity only once.");
            }
        }
        return fields;
    }
}
