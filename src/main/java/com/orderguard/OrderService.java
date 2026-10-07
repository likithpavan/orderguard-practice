package com.orderguard;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderService {
    private final OrderValidator validator;
    private final OrderRepository repository;

    public OrderService(OrderValidator validator, OrderRepository repository) {
        this.validator = validator;
        this.repository = repository;
    }

    public Order submit(OrderRequest request) throws IOException {
        // Validation must finish successfully before any order is created or persisted.
        OrderValidator.ValidatedOrder valid = validator.validate(request);
        Order order = new Order(UUID.randomUUID(), valid.symbol(), valid.side(), valid.quantity(), Instant.now());
        repository.save(order);
        return order;
    }

    public List<Order> listOrders() { return repository.findAll(); }
    public int count() { return repository.count(); }
    public void clearPracticeOrders() throws IOException { repository.clear(); }
}
