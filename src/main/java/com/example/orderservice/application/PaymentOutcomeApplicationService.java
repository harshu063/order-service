package com.example.orderservice.application;

import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderStatus;
import com.example.orderservice.domain.PaymentOutcomeInbox;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PaymentOutcomeApplicationService {
    private final OrderRepository orders;
    private final PaymentOutcomeInboxRepository inbox;

    PaymentOutcomeApplicationService(OrderRepository orders, PaymentOutcomeInboxRepository inbox) {
        this.orders = orders;
        this.inbox = inbox;
    }

    @Transactional
    public PaymentOutcomeResult apply(PaymentOutcomeCommand command) {
        if (command == null || command.eventId() == null) {
            throw new OrderExceptions.InvalidPaymentOutcome("payment outcome must have an event id");
        }
        String eventId = command.eventId().toString();
        OrderStatus target = targetStatus(command);
        validateEnvelope(command);
        try {
            inbox.saveAndFlush(new PaymentOutcomeInbox(eventId, command.tenantId(),
                    command.payload().orderId().toString(), command.payload().paymentId().toString(),
                    command.eventType(), Instant.now()));
        } catch (DataIntegrityViolationException duplicate) {
            return new PaymentOutcomeResult(true, null);
        }
        Order order = orders.findByIdAndTenantId(command.payload().orderId().toString(), command.tenantId())
                .orElseThrow(() -> new OrderExceptions.InvalidPaymentOutcome("payment outcome references an unknown order"));
        validateMatches(order, command);
        try {
            order.transitionTo(target, Instant.now());
        } catch (IllegalStateException exception) {
            throw new OrderExceptions.InvalidPaymentOutcome(exception.getMessage());
        }
        return new PaymentOutcomeResult(false, order.getStatus());
    }

    private OrderStatus targetStatus(PaymentOutcomeCommand command) {
        if ("payment.succeeded.v1".equals(command.eventType())) {
            return OrderStatus.CONFIRMED;
        }
        if ("payment.failed.v1".equals(command.eventType())) {
            return OrderStatus.PAYMENT_FAILED;
        }
        throw new OrderExceptions.InvalidPaymentOutcome("unsupported payment outcome event type");
    }

    private void validateEnvelope(PaymentOutcomeCommand command) {
        if (command.schemaVersion() != 1 || !"payment-service".equals(command.source())
                || command.tenantId() == null || command.tenantId().isBlank()
                || command.occurredAt() == null || command.correlationId() == null || command.correlationId().isBlank()
                || command.correlationId().length() > 128
                || command.subject() == null || !"payment".equals(command.subject().type())
                || command.subject().id() == null || command.subject().id().isBlank()
                || command.payload() == null || command.payload().paymentId() == null
                || command.payload().orderId() == null || command.payload().amountMinor() < 0
                || command.payload().currency() == null || !command.payload().currency().matches("[A-Z]{3}")
                || command.payload().paymentVersion() < 1) {
            throw new OrderExceptions.InvalidPaymentOutcome("payment outcome is invalid");
        }
        if (!command.subject().id().equals(command.payload().paymentId().toString())
                || !((command.tenantId() + ":" + command.payload().orderId()).equals(command.orderingKey()))) {
            throw new OrderExceptions.InvalidPaymentOutcome("payment outcome does not match its envelope");
        }
    }

    private void validateMatches(Order order, PaymentOutcomeCommand command) {
        if (order.getTotalMinor() != command.payload().amountMinor()) {
            throw new OrderExceptions.InvalidPaymentOutcome("payment amount does not match the order");
        }
        if (!order.getCurrency().equals(command.payload().currency())) {
            throw new OrderExceptions.InvalidPaymentOutcome("payment currency does not match the order");
        }
    }
}
