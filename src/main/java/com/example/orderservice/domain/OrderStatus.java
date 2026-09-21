package com.example.orderservice.domain;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    PAYMENT_FAILED;

    private Set<OrderStatus> allowedTargets() {
        return switch (this) {
            case PENDING_PAYMENT -> EnumSet.of(CONFIRMED, CANCELLED, PAYMENT_FAILED);
            case PAYMENT_FAILED -> EnumSet.of(PENDING_PAYMENT, CANCELLED);
            case CONFIRMED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitionTo(OrderStatus target) {
        return allowedTargets().contains(target);
    }
}
