package com.example.orderservice.application;

import com.example.orderservice.domain.OrderStatus;

public record PaymentOutcomeResult(boolean replayed, OrderStatus status) {
}
