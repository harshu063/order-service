package com.example.orderservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "idempotency_records", uniqueConstraints =
        @UniqueConstraint(name = "uq_idempotency_owner_key",
                columnNames = {"tenant_id", "user_id", "idempotency_key"}))
public class IdempotencyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", length = 100, nullable = false)
    private String tenantId;

    @Column(name = "user_id", length = 255, nullable = false)
    private String userId;

    @Column(name = "idempotency_key", length = 128, nullable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 64, nullable = false)
    private String requestHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String tenantId, String userId, String idempotencyKey,
                             String requestHash, Order order, Instant createdAt) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.order = order;
        this.createdAt = createdAt;
    }

    public String getRequestHash() { return requestHash; }
    public Order getOrder() { return order; }
}
