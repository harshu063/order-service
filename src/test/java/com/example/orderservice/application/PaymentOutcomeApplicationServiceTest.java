package com.example.orderservice.application;

import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentOutcomeApplicationServiceTest {
    @Test
    void appliesSucceededOutcomeOnceAndConfirmsPendingOrder() {
        OrderRepository orders = mock(OrderRepository.class);
        PaymentOutcomeInboxRepository inbox = mock(PaymentOutcomeInboxRepository.class);
        Order order = pendingOrder();
        PaymentOutcomeCommand command = command("payment.succeeded.v1", order.getId(), order.getTotalMinor());
        when(orders.findByIdAndTenantId(order.getId(), "tenant-a")).thenReturn(java.util.Optional.of(order));

        PaymentOutcomeResult result = new PaymentOutcomeApplicationService(orders, inbox).apply(command);

        assertThat(result.replayed()).isFalse();
        assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getAggregateVersion()).isEqualTo(2);
    }

    @Test
    void replaysPersistedEventWithoutReapplyingTheOrderTransition() {
        OrderRepository orders = mock(OrderRepository.class);
        PaymentOutcomeInboxRepository inbox = mock(PaymentOutcomeInboxRepository.class);
        PaymentOutcomeCommand command = command("payment.failed.v1", UUID.randomUUID().toString(), 5000);
        doThrow(new DataIntegrityViolationException("duplicate event"))
                .when(inbox).saveAndFlush(org.mockito.ArgumentMatchers.any());

        PaymentOutcomeResult result = new PaymentOutcomeApplicationService(orders, inbox).apply(command);

        assertThat(result.replayed()).isTrue();
        assertThat(result.status()).isNull();
        verifyNoInteractions(orders);
    }

    @Test
    void rejectsOutcomeWhoseAmountDoesNotMatchTheOrder() {
        OrderRepository orders = mock(OrderRepository.class);
        PaymentOutcomeInboxRepository inbox = mock(PaymentOutcomeInboxRepository.class);
        Order order = pendingOrder();
        PaymentOutcomeCommand command = command("payment.succeeded.v1", order.getId(), 4999);
        when(orders.findByIdAndTenantId(order.getId(), "tenant-a")).thenReturn(java.util.Optional.of(order));

        assertThatThrownBy(() -> new PaymentOutcomeApplicationService(orders, inbox).apply(command))
                .isInstanceOf(OrderExceptions.InvalidPaymentOutcome.class)
                .hasMessageContaining("amount");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void rejectsASecondDistinctOutcomeAfterTheOrderIsTerminal() {
        OrderRepository orders = mock(OrderRepository.class);
        PaymentOutcomeInboxRepository inbox = mock(PaymentOutcomeInboxRepository.class);
        Order order = pendingOrder();
        order.transitionTo(OrderStatus.PAYMENT_FAILED, Instant.parse("2026-10-01T00:01:00Z"));
        PaymentOutcomeCommand command = command("payment.succeeded.v1", order.getId(), order.getTotalMinor());
        when(orders.findByIdAndTenantId(order.getId(), "tenant-a")).thenReturn(java.util.Optional.of(order));

        assertThatThrownBy(() -> new PaymentOutcomeApplicationService(orders, inbox).apply(command))
                .isInstanceOf(OrderExceptions.InvalidPaymentOutcome.class)
                .hasMessageContaining("cannot transition");
    }

    private Order pendingOrder() {
        return new Order(UUID.randomUUID().toString(), "tenant-a", "user-a", 5000, "USD",
                Instant.parse("2026-10-01T00:00:00Z"));
    }

    private PaymentOutcomeCommand command(String eventType, String orderId, long amountMinor) {
        UUID paymentId = UUID.randomUUID();
        return new PaymentOutcomeCommand(UUID.randomUUID(), eventType, 1, "tenant-a", "payment-service",
                new PaymentOutcomeCommand.Subject("payment", paymentId.toString()),
                "tenant-a:" + orderId, Instant.parse("2026-10-01T00:02:00Z"), "corr-001",
                new PaymentOutcomeCommand.Payload(paymentId, UUID.fromString(orderId), amountMinor, "USD", 1));
    }
}
