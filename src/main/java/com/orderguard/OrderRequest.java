package com.orderguard;

// Keep raw input until validation; otherwise a fractional or missing quantity can be hidden.
public record OrderRequest(String symbol, String side, String quantity) {}
