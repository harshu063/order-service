package com.example.orderservice.application;

import com.example.orderservice.domain.IdempotencyRecord;
import com.example.orderservice.domain.Order;
import com.example.orderservice.integration.CatalogClient;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderApplicationServiceTest {
    @Test
    void replayReturnsPersistedOrderWithoutCallingCatalog() {
        OrderRepository orders = mock(OrderRepository.class);
        IdempotencyRecordRepository idempotency = mock(IdempotencyRecordRepository.class);
        OrderCreator creator = mock(OrderCreator.class);
        CatalogClient catalog = mock(CatalogClient.class);
        OrderApplicationService service = new OrderApplicationService(
                orders, idempotency, creator, catalog, "default");
        var lines = List.of(new OrderModels.RequestedLine(42, 2));
        String hash = OrderApplicationService.fingerprint(lines);
        Order order = new Order("80f3df4e-0810-4bf3-b513-4422609a4504", "tenant-a", "user-a",
                5000, "USD", Instant.parse("2026-09-21T00:00:00Z"));
        IdempotencyRecord record = new IdempotencyRecord(
                "tenant-a", "user-a", "checkout-request-0001", hash, order, Instant.now());
        when(idempotency.findByTenantIdAndUserIdAndIdempotencyKey(
                "tenant-a", "user-a", "checkout-request-0001")).thenReturn(Optional.of(record));

        var result = service.create(jwt(), "token", "checkout-request-0001", lines, "corr-1");

        assertThat(result.replayed()).isTrue();
        assertThat(result.order().orderId()).isEqualTo(order.getId());
        verifyNoInteractions(catalog, creator);
    }

    private Jwt jwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-a")
                .claim("tenant_id", "tenant-a")
                .build();
    }
}
