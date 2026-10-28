package com.example.orderservice.application;

import com.example.orderservice.domain.OrderStatus;

import java.time.Instant;
import java.util.List;

public final class OrderModels {
    private OrderModels() {
    }

    public record RequestedLine(long productId, int quantity) {
    }

    record PricedLine(long productId, long productVersion, String title,
                      long unitPriceMinor, int quantity) {
    }

    public record OrderView(String orderId, OrderStatus status, long totalMinor,
                            String currency, long version, Instant createdAt) {
    }

    public record OrderPageView(List<OrderView> items, String nextCursor) {
    }

    public record CreateResult(OrderView order, boolean replayed) {
    }
}
