package com.example.orderservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "outbox_events", uniqueConstraints =
        @UniqueConstraint(name = "uq_outbox_aggregate_event",
                columnNames = {"aggregate_type", "aggregate_id", "aggregate_version", "event_type"}))
public class OutboxEvent {
    @Id
    @Column(name = "event_id", length = 36, nullable = false)
    private String eventId;

    @Column(name = "aggregate_type", length = 100, nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 200, nullable = false)
    private String aggregateId;

    @Column(name = "aggregate_version", nullable = false)
    private long aggregateVersion;

    @Column(name = "event_type", length = 150, nullable = false)
    private String eventType;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(name = "ordering_key", length = 300, nullable = false)
    private String orderingKey;

    @Column(columnDefinition = "json", nullable = false)
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "publish_attempts", nullable = false)
    private int publishAttempts;

    protected OutboxEvent() {
    }

    public OutboxEvent(String eventId, String aggregateId, long aggregateVersion,
                       String orderingKey, String payload, Instant occurredAt) {
        this.eventId = eventId;
        this.aggregateType = "order";
        this.aggregateId = aggregateId;
        this.aggregateVersion = aggregateVersion;
        this.eventType = "order.created.v1";
        this.schemaVersion = 1;
        this.orderingKey = orderingKey;
        this.payload = payload;
        this.occurredAt = occurredAt;
    }

    public String getEventId() { return eventId; }
    public String getPayload() { return payload; }
}
