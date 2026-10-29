package com.example.orderservice.application;

import com.example.orderservice.domain.IdempotencyRecord;
import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OutboxEvent;
import com.example.orderservice.domain.PaymentOutcomeInbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface OrderRepository extends JpaRepository<Order, String> {
    Optional<Order> findByIdAndTenantId(String id, String tenantId);

    Optional<Order> findByIdAndTenantIdAndUserId(String id, String tenantId, String userId);

    @Query("""
            select o from Order o
            where o.tenantId = :tenantId and o.userId = :userId
              and (:cursorTime is null or o.createdAt < :cursorTime
                   or (o.createdAt = :cursorTime and o.id < :cursorId))
            order by o.createdAt desc, o.id desc
            """)
    List<Order> findOwnerPage(@Param("tenantId") String tenantId,
                              @Param("userId") String userId,
                              @Param("cursorTime") Instant cursorTime,
                              @Param("cursorId") String cursorId,
                              Pageable pageable);
}

interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {
    @EntityGraph(attributePaths = "order")
    Optional<IdempotencyRecord> findByTenantIdAndUserIdAndIdempotencyKey(
            String tenantId, String userId, String idempotencyKey);
}

interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
}

interface PaymentOutcomeInboxRepository extends JpaRepository<PaymentOutcomeInbox, String> {
    @Modifying
    @Query(value = """
            INSERT IGNORE INTO payment_outcome_inbox
                (event_id, tenant_id, order_id, payment_id, event_type, processed_at)
            VALUES (:eventId, :tenantId, :orderId, :paymentId, :eventType, :processedAt)
            """, nativeQuery = true)
    int claim(@Param("eventId") String eventId,
              @Param("tenantId") String tenantId,
              @Param("orderId") String orderId,
              @Param("paymentId") String paymentId,
              @Param("eventType") String eventType,
              @Param("processedAt") Instant processedAt);
}
