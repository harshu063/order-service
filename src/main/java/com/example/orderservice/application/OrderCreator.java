package com.example.orderservice.application;

import com.example.orderservice.domain.IdempotencyRecord;
import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OutboxEvent;
import com.example.orderservice.application.OrderModels.CreateResult;
import com.example.orderservice.application.OrderModels.PricedLine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.example.orderservice.application.OrderExceptions.IdempotencyConflict;

@Component
class OrderCreator {
    private final OrderRepository orders;
    private final IdempotencyRecordRepository idempotencyRecords;
    private final OutboxEventRepository outboxEvents;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    OrderCreator(OrderRepository orders, IdempotencyRecordRepository idempotencyRecords,
                 OutboxEventRepository outboxEvents, ObjectMapper objectMapper) {
        this(orders, idempotencyRecords, outboxEvents, objectMapper, Clock.systemUTC());
    }

    OrderCreator(OrderRepository orders, IdempotencyRecordRepository idempotencyRecords,
                 OutboxEventRepository outboxEvents, ObjectMapper objectMapper, Clock clock) {
        this.orders = orders;
        this.idempotencyRecords = idempotencyRecords;
        this.outboxEvents = outboxEvents;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    CreateResult create(String tenantId, String userId, String idempotencyKey, String requestHash,
                        List<PricedLine> lines, long totalMinor, String currency,
                        String correlationId) {
        var existing = idempotencyRecords.findByTenantIdAndUserIdAndIdempotencyKey(
                tenantId, userId, idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflict();
            }
            return new CreateResult(OrderApplicationService.toView(existing.get().getOrder()), true);
        }

        Instant now = clock.instant();
        String orderId = UUID.randomUUID().toString();
        Order order = new Order(orderId, tenantId, userId, totalMinor, currency, now);
        lines.forEach(line -> order.addItem(line.productId(), line.productVersion(), line.title(),
                line.unitPriceMinor(), line.quantity()));
        orders.save(order);
        idempotencyRecords.save(new IdempotencyRecord(
                tenantId, userId, idempotencyKey, requestHash, order, now));

        String eventId = UUID.randomUUID().toString();
        String orderingKey = tenantId + ":" + orderId;
        outboxEvents.save(new OutboxEvent(eventId, orderId, order.getAggregateVersion(), orderingKey,
                orderCreatedPayload(eventId, order, orderingKey, correlationId, now), now));
        orders.flush();
        return new CreateResult(OrderApplicationService.toView(order), false);
    }

    private String orderCreatedPayload(String eventId, Order order, String orderingKey,
                                       String correlationId, Instant occurredAt) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", "order.created.v1");
        envelope.put("schemaVersion", 1);
        envelope.put("tenantId", order.getTenantId());
        envelope.put("source", "order-service");
        envelope.put("subject", Map.of("type", "order", "id", order.getId()));
        envelope.put("orderingKey", orderingKey);
        envelope.put("sequence", order.getAggregateVersion());
        envelope.put("occurredAt", occurredAt.toString());
        envelope.put("correlationId", correlationId);
        envelope.put("payload", Map.of(
                "orderId", order.getId(),
                "userId", order.getUserId(),
                "totalMinor", order.getTotalMinor(),
                "currency", order.getCurrency(),
                "orderVersion", order.getAggregateVersion()));
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize order.created.v1", exception);
        }
    }
}
