package com.orderguard;

import java.util.Locale;
import java.util.Set;

public final class OrderValidator {
    public static final int MAX_QUANTITY = 1000;
    private static final Set<String> SYMBOLS = Set.of("AAPL", "MSFT", "NVDA");
    public record ValidatedOrder(String symbol, String side, int quantity) {}

    public ValidatedOrder validate(OrderRequest request) {
        if (request == null) throw new ValidationException("An order request is required.");
        String symbol = normalized(request.symbol());
        String side = normalized(request.side());
        if (!SYMBOLS.contains(symbol)) {
            throw new ValidationException("Choose a supported stock: AAPL, MSFT, or NVDA.");
        }
        if (!side.equals("BUY") && !side.equals("SELL")) {
            throw new ValidationException("Order action must be BUY or SELL.");
        }
        String rawQuantity = request.quantity() == null ? "" : request.quantity().trim();
        if (!rawQuantity.matches("[0-9]+")) throw invalidQuantity();
        final int quantity;
        try { quantity = Integer.parseInt(rawQuantity); }
        catch (NumberFormatException exception) { throw invalidQuantity(); }
        if (quantity < 1) throw invalidQuantity();

        // OG-101 starter: the upper-limit check is missing here.

        return new ValidatedOrder(symbol, side, quantity);
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static ValidationException invalidQuantity() {
        return new ValidationException("Quantity must be a whole number from 1 to 1000.");
    }
}
