package com.orderguard;

import java.time.Instant;
import java.util.UUID;

public record Order(UUID id, String symbol, String side, int quantity, Instant createdAt) {
    public String toJson() {
        return "{\"id\":" + HttpSupport.quote(id.toString())
            + ",\"symbol\":" + HttpSupport.quote(symbol)
            + ",\"side\":" + HttpSupport.quote(side)
            + ",\"quantity\":" + quantity
            + ",\"createdAt\":" + HttpSupport.quote(createdAt.toString()) + "}";
    }
}
