package com.orderguard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// A single-instance, file-backed training store. Updates are serialized and replace the file atomically.
public final class OrderRepository {
    private final Path file;
    private List<Order> orders = new ArrayList<>();

    public OrderRepository(Path file) throws IOException {
        this.file = file.toAbsolutePath();
        Files.createDirectories(this.file.getParent());
        if (Files.exists(this.file)) {
            try {
                for (String line : Files.readAllLines(this.file, StandardCharsets.UTF_8)) {
                    if (line.isBlank()) continue;
                    String[] fields = line.split("\t", -1);
                    if (fields.length != 5) throw new IllegalArgumentException("Invalid row.");
                    orders.add(new Order(UUID.fromString(fields[0]), fields[1], fields[2],
                        Integer.parseInt(fields[3]), Instant.parse(fields[4])));
                }
            } catch (IllegalArgumentException exception) {
                throw new IOException("The practice order file is damaged; preserve it before repairing it.", exception);
            }
        }
    }

    private OrderRepository() { this.file = null; }
    public static OrderRepository inMemory() { return new OrderRepository(); }

    public synchronized void save(Order order) throws IOException {
        List<Order> updated = new ArrayList<>(orders);
        updated.add(order);
        persist(updated);
        orders = updated;
    }

    public synchronized List<Order> findAll() { return List.copyOf(orders); }
    public synchronized int count() { return orders.size(); }

    public synchronized void clear() throws IOException {
        persist(List.of());
        orders = new ArrayList<>();
    }

    private void persist(List<Order> updated) throws IOException {
        if (file == null) return;
        StringBuilder contents = new StringBuilder();
        for (Order order : updated) {
            contents.append(order.id()).append('\t').append(order.symbol()).append('\t')
                .append(order.side()).append('\t').append(order.quantity()).append('\t')
                .append(order.createdAt()).append('\n');
        }
        Path temporary = Files.createTempFile(file.getParent(), "orders-", ".tmp");
        try {
            Files.writeString(temporary, contents, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
    }
}
