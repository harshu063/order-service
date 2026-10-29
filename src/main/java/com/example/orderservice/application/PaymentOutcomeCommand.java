package com.example.orderservice.application;

import java.time.Instant;
import java.util.UUID;

public record PaymentOutcomeCommand(
        UUID eventId,
        String eventType,
        int schemaVersion,
        String tenantId,
        String source,
        Subject subject,
        String orderingKey,
        Instant occurredAt,
        String correlationId,
        Payload payload) {

    public record Subject(String type, String id) {
    }

    public record Payload(UUID paymentId, UUID orderId, long amountMinor, String currency, long paymentVersion) {
    }
}
