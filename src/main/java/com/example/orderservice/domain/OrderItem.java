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

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private long productId;

    @Column(name = "product_version", nullable = false)
    private long productVersion;

    @Column(name = "product_title", length = 200, nullable = false)
    private String productTitle;

    @Column(name = "unit_price_minor", nullable = false)
    private long unitPriceMinor;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total_minor", nullable = false)
    private long lineTotalMinor;

    protected OrderItem() {
    }

    OrderItem(Order order, long productId, long productVersion, String productTitle,
              long unitPriceMinor, int quantity, long lineTotalMinor) {
        this.order = order;
        this.productId = productId;
        this.productVersion = productVersion;
        this.productTitle = productTitle;
        this.unitPriceMinor = unitPriceMinor;
        this.quantity = quantity;
        this.lineTotalMinor = lineTotalMinor;
    }

    public long getProductId() { return productId; }
    public long getProductVersion() { return productVersion; }
    public String getProductTitle() { return productTitle; }
    public long getUnitPriceMinor() { return unitPriceMinor; }
    public int getQuantity() { return quantity; }
    public long getLineTotalMinor() { return lineTotalMinor; }
}
