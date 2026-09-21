package com.example.orderservice.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "tenant_id", length = 100, nullable = false)
    private String tenantId;

    @Column(name = "user_id", length = 255, nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private OrderStatus status;

    @Column(name = "total_minor", nullable = false)
    private long totalMinor;

    @Column(length = 3, nullable = false)
    private String currency;

    @Column(name = "aggregate_version", nullable = false)
    private long aggregateVersion;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public Order(String id, String tenantId, String userId, long totalMinor, String currency, Instant now) {
        this.id = id;
        this.tenantId = tenantId;
        this.userId = userId;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.totalMinor = totalMinor;
        this.currency = currency;
        this.aggregateVersion = 1;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void addItem(long productId, long productVersion, String productTitle,
                        long unitPriceMinor, int quantity) {
        items.add(new OrderItem(this, productId, productVersion, productTitle,
                unitPriceMinor, quantity, Math.multiplyExact(unitPriceMinor, quantity)));
    }

    public void transitionTo(OrderStatus target, Instant now) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException("Order cannot transition from " + status + " to " + target);
        }
        status = target;
        aggregateVersion++;
        updatedAt = now;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getUserId() { return userId; }
    public OrderStatus getStatus() { return status; }
    public long getTotalMinor() { return totalMinor; }
    public String getCurrency() { return currency; }
    public long getAggregateVersion() { return aggregateVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
}
