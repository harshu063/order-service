package com.example.orderservice.api;

import com.example.orderservice.application.OrderModels.OrderPageView;
import com.example.orderservice.application.OrderModels.OrderView;
import com.example.orderservice.application.OrderModels.RequestedLine;
import com.example.orderservice.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class OrderApiModels {
    private OrderApiModels() {
    }

    public record CreateOrderRequest(
            @NotEmpty @Size(max = 100) List<@Valid CreateOrderItem> items) {
        List<RequestedLine> toCommand() {
            return items.stream()
                    .map(item -> new RequestedLine(item.productId(), item.quantity()))
                    .toList();
        }
    }

    public record CreateOrderItem(@NotNull @Positive Long productId,
                                  @NotNull @Min(1) @Max(100) Integer quantity) {
    }

    public record OrderResponse(UUID orderId, OrderStatus status, long totalMinor,
                                String currency, long version) {
        static OrderResponse from(OrderView order) {
            return new OrderResponse(UUID.fromString(order.orderId()), order.status(), order.totalMinor(),
                    order.currency(), order.version());
        }
    }

    public record OrderPageResponse(List<OrderResponse> items, String nextCursor) {
        static OrderPageResponse from(OrderPageView page) {
            return new OrderPageResponse(page.items().stream().map(OrderResponse::from).toList(),
                    page.nextCursor());
        }
    }
}
