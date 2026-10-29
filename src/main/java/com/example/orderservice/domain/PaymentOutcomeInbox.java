package com.example.orderservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "payment_outcome_inbox")
public class PaymentOutcomeInbox {
    @Id
    @Column(name = "event_id", length = 36, columnDefinition = "CHAR(36)", nullable = false)
    private String eventId;

    @Column(name = "tenant_id", length = 100, nullable = false)
    private String tenantId;

    @Column(name = "order_id", length = 36, columnDefinition = "CHAR(36)", nullable = false)
    private String orderId;

    @Column(name = "payment_id", length = 36, columnDefinition = "CHAR(36)", nullable = false)
    private String paymentId;

    @Column(name = "event_type", length = 150, nullable = false)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected PaymentOutcomeInbox() {
    }

    public PaymentOutcomeInbox(String eventId, String tenantId, String orderId, String paymentId,
                               String eventType, Instant processedAt) {
        this.eventId = eventId;
        this.tenantId = tenantId;
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.eventType = eventType;
        this.processedAt = processedAt;
    }
}
