package com.example.orderservice.application;

import com.example.orderservice.domain.Order;
import com.example.orderservice.integration.CatalogClient;
import com.example.orderservice.integration.CatalogClient.CatalogProduct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import static com.example.orderservice.application.OrderExceptions.BadOrderRequest;
import static com.example.orderservice.application.OrderExceptions.IdempotencyConflict;
import static com.example.orderservice.application.OrderExceptions.OrderNotFound;
import static com.example.orderservice.application.OrderModels.CreateResult;
import static com.example.orderservice.application.OrderModels.OrderPageView;
import static com.example.orderservice.application.OrderModels.OrderView;
import static com.example.orderservice.application.OrderModels.PricedLine;
import static com.example.orderservice.application.OrderModels.RequestedLine;

@Service
public class OrderApplicationService {
    private final OrderRepository orders;
    private final IdempotencyRecordRepository idempotencyRecords;
    private final OrderCreator creator;
    private final CatalogClient catalog;
    private final String defaultTenant;

    OrderApplicationService(OrderRepository orders, IdempotencyRecordRepository idempotencyRecords,
                            OrderCreator creator, CatalogClient catalog,
                            @Value("${security.default-tenant}") String defaultTenant) {
        this.orders = orders;
        this.idempotencyRecords = idempotencyRecords;
        this.creator = creator;
        this.catalog = catalog;
        this.defaultTenant = defaultTenant;
    }

    public CreateResult create(Jwt jwt, String bearerToken, String idempotencyKey,
                               List<RequestedLine> requestedLines, String correlationId) {
        Identity identity = identity(jwt);
        String key = validatedIdempotencyKey(idempotencyKey);
        List<RequestedLine> lines = normalizedLines(requestedLines);
        String requestHash = fingerprint(lines);

        var existing = idempotencyRecords.findByTenantIdAndUserIdAndIdempotencyKey(
                identity.tenantId(), identity.userId(), key);
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflict();
            }
            return new CreateResult(toView(existing.get().getOrder()), true);
        }

        List<PricedLine> pricedLines = new ArrayList<>();
        String currency = null;
        long totalMinor = 0;
        for (RequestedLine line : lines) {
            CatalogProduct product = validatedProduct(catalog.getProduct(line.productId(), bearerToken), line.productId());
            if (currency == null) {
                currency = product.currency();
            } else if (!currency.equals(product.currency())) {
                throw new BadOrderRequest("All order items must use the same currency");
            }
            try {
                totalMinor = Math.addExact(totalMinor, Math.multiplyExact(product.priceMinor(), line.quantity()));
            } catch (ArithmeticException exception) {
                throw new BadOrderRequest("Order total is too large");
            }
            pricedLines.add(new PricedLine(product.id(), product.version(), product.title(),
                    product.priceMinor(), line.quantity()));
        }

        try {
            return creator.create(identity.tenantId(), identity.userId(), key, requestHash,
                    pricedLines, totalMinor, currency, correlationId);
        } catch (DataIntegrityViolationException race) {
            var winner = idempotencyRecords.findByTenantIdAndUserIdAndIdempotencyKey(
                    identity.tenantId(), identity.userId(), key);
            if (winner.isEmpty()) {
                throw race;
            }
            if (!winner.get().getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflict();
            }
            return new CreateResult(toView(winner.get().getOrder()), true);
        }
    }

    public OrderView getOwnOrder(Jwt jwt, String orderId) {
        Identity identity = identity(jwt);
        return orders.findByIdAndTenantIdAndUserId(orderId, identity.tenantId(), identity.userId())
                .map(OrderApplicationService::toView)
                .orElseThrow(OrderNotFound::new);
    }

    public OrderPageView listOwnOrders(Jwt jwt, String encodedCursor, int limit) {
        Identity identity = identity(jwt);
        Cursor cursor = decodeCursor(encodedCursor);
        List<Order> page = orders.findOwnerPage(identity.tenantId(), identity.userId(),
                cursor == null ? null : cursor.createdAt(), cursor == null ? "" : cursor.orderId(),
                PageRequest.of(0, limit + 1));
        boolean hasMore = page.size() > limit;
        List<Order> visible = hasMore ? page.subList(0, limit) : page;
        String next = hasMore ? encodeCursor(visible.get(visible.size() - 1)) : null;
        return new OrderPageView(visible.stream().map(OrderApplicationService::toView).toList(), next);
    }

    static OrderView toView(Order order) {
        return new OrderView(order.getId(), order.getStatus(), order.getTotalMinor(),
                order.getCurrency(), order.getAggregateVersion(), order.getCreatedAt());
    }

    static String fingerprint(List<RequestedLine> lines) {
        String canonical = lines.stream()
                .map(line -> line.productId() + ":" + line.quantity())
                .reduce((left, right) -> left + "|" + right)
                .orElseThrow();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private List<RequestedLine> normalizedLines(List<RequestedLine> requestedLines) {
        if (requestedLines == null || requestedLines.isEmpty() || requestedLines.size() > 100) {
            throw new BadOrderRequest("An order must contain between 1 and 100 items");
        }
        List<RequestedLine> lines = requestedLines.stream()
                .sorted(Comparator.comparingLong(RequestedLine::productId))
                .toList();
        HashSet<Long> products = new HashSet<>();
        for (RequestedLine line : lines) {
            if (line.productId() <= 0 || line.quantity() < 1 || line.quantity() > 100) {
                throw new BadOrderRequest("Each item needs a positive productId and quantity from 1 to 100");
            }
            if (!products.add(line.productId())) {
                throw new BadOrderRequest("Duplicate productId " + line.productId() + " is not allowed");
            }
        }
        return lines;
    }

    private CatalogProduct validatedProduct(CatalogProduct product, long requestedProductId) {
        if (product.id() != requestedProductId || product.priceMinor() < 0 || product.version() < 0
                || product.title() == null || product.title().isBlank() || product.title().length() > 200
                || product.currency() == null
                || !product.currency().matches("[A-Z]{3}")) {
            throw new BadOrderRequest("Catalog returned an invalid product snapshot for " + requestedProductId);
        }
        return product;
    }

    private String validatedIdempotencyKey(String key) {
        if (key == null || key.length() < 16 || key.length() > 128 || !key.equals(key.trim())) {
            throw new BadOrderRequest("Idempotency-Key must contain 16 to 128 non-padding characters");
        }
        return key;
    }

    private Identity identity(Jwt jwt) {
        String userId = jwt.getSubject();
        if (userId == null || userId.isBlank() || userId.length() > 255) {
            throw new BadOrderRequest("Access token has no usable subject");
        }
        Object tenantClaim = jwt.getClaim("tenant_id");
        String tenantId = tenantClaim == null ? defaultTenant : tenantClaim.toString();
        if (tenantId.isBlank() || tenantId.length() > 100) {
            throw new BadOrderRequest("Access token has no usable tenant_id");
        }
        return new Identity(tenantId.toLowerCase(Locale.ROOT), userId);
    }

    private String encodeCursor(Order order) {
        String value = order.getCreatedAt() + "|" + order.getId();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private Cursor decodeCursor(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException();
            }
            return new Cursor(Instant.parse(parts[0]), parts[1]);
        } catch (RuntimeException invalid) {
            throw new BadOrderRequest("cursor is invalid");
        }
    }

    private record Identity(String tenantId, String userId) {
    }

    private record Cursor(Instant createdAt, String orderId) {
    }
}
