package com.example.orderservice.api;

import com.example.orderservice.application.OrderApplicationService;
import com.example.orderservice.api.OrderApiModels.CreateOrderRequest;
import com.example.orderservice.api.OrderApiModels.OrderPageResponse;
import com.example.orderservice.api.OrderApiModels.OrderResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/orders")
class OrderController {
    private final OrderApplicationService orders;

    OrderController(OrderApplicationService orders) {
        this.orders = orders;
    }

    @PostMapping
    ResponseEntity<OrderResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") @Size(min = 16, max = 128) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest servletRequest) {
        var result = orders.create(jwt, jwt.getTokenValue(), idempotencyKey, request.toCommand(),
                correlationId(servletRequest));
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/orders/" + result.order().orderId()))
                .header("Idempotent-Replayed", Boolean.toString(result.replayed()))
                .body(OrderResponse.from(result.order()));
    }

    @GetMapping
    OrderPageResponse list(@AuthenticationPrincipal Jwt jwt,
                           @RequestParam(required = false) String cursor,
                           @RequestParam(defaultValue = "25") @Min(1) @Max(100) int limit) {
        return OrderPageResponse.from(orders.listOwnOrders(jwt, cursor, limit));
    }

    @GetMapping("/{orderId}")
    OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return OrderResponse.from(orders.getOwnOrder(jwt, orderId.toString()));
    }

    private String correlationId(HttpServletRequest request) {
        return request.getAttribute(CorrelationIdFilter.ATTRIBUTE).toString();
    }
}
