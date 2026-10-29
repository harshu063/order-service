package com.example.orderservice.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentOutcomeKafkaListenerTest {
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final PaymentOutcomeApplicationService outcomes = mock(PaymentOutcomeApplicationService.class);
    private final PaymentOutcomeKafkaListener listener = new PaymentOutcomeKafkaListener(objectMapper, outcomes);

    @Test
    void appliesCanonicalPaymentOutcomeWhenKafkaKeyMatchesOrderingKey() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String key = "tenant-a:" + orderId;
        String payload = """
                {"eventId":"%s","eventType":"payment.succeeded.v1","schemaVersion":1,
                "tenantId":"tenant-a","source":"payment-service",
                "subject":{"type":"payment","id":"%s"},"orderingKey":"%s",
                "occurredAt":"2026-10-30T00:00:00Z","correlationId":"checkout-42",
                "payload":{"paymentId":"%s","orderId":"%s","amountMinor":2599,"currency":"INR","paymentVersion":1}}
                """.formatted(UUID.randomUUID(), paymentId, key, paymentId, orderId);

        listener.consume(payload, key);

        verify(outcomes).apply(any(PaymentOutcomeCommand.class));
    }

    @Test
    void rejectsMessageWhoseKafkaKeyDoesNotMatchItsOrderingKey() {
        String payload = "{" + "\"orderingKey\":\"tenant-a:order-42\"}";

        assertThatThrownBy(() -> listener.consume(payload, "tenant-b:order-42"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be applied");
    }
}
