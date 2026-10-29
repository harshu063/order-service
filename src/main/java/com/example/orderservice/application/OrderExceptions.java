package com.example.orderservice.application;

public final class OrderExceptions {
    private OrderExceptions() {
    }

    public static class BadOrderRequest extends RuntimeException {
        public BadOrderRequest(String message) { super(message); }
    }

    public static class IdempotencyConflict extends RuntimeException {
        public IdempotencyConflict() {
            super("Idempotency-Key was already used for a different request");
        }
    }

    public static class OrderNotFound extends RuntimeException {
        public OrderNotFound() { super("Order not found"); }
    }

    public static class InvalidPaymentOutcome extends RuntimeException {
        public InvalidPaymentOutcome(String message) { super(message); }
    }
}
