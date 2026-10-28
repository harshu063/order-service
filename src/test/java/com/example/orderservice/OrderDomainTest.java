package com.example.orderservice;

import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderDomainTest {
    @Test
    void enforcesLifecycleAndMonotonicAggregateVersion() {
        Order order = new Order("80f3df4e-0810-4bf3-b513-4422609a4504", "tenant-a", "user-a",
                1200, "USD", Instant.parse("2026-09-21T00:00:00Z"));

        order.transitionTo(OrderStatus.CONFIRMED, Instant.parse("2026-09-21T00:01:00Z"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getAggregateVersion()).isEqualTo(2);
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.PAYMENT_FAILED, Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CONFIRMED");
    }

    @Test
    void calculatesLineTotalWithExactArithmetic() {
        Order order = new Order("80f3df4e-0810-4bf3-b513-4422609a4504", "tenant-a", "user-a",
                1500, "USD", Instant.parse("2026-09-21T00:00:00Z"));

        order.addItem(42, 3, "Keyboard", 500, 3);

        assertThat(order.getItems()).singleElement()
                .extracting("lineTotalMinor")
                .isEqualTo(1500L);
    }
}
