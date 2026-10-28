package com.example.orderservice.api;

import com.example.orderservice.api.OrderApiModels.OrderResponse;
import com.example.orderservice.domain.OrderStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPaymentContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void orderSnapshotJsonContainsFieldsConsumedByPaymentService() throws Exception {
        UUID orderId = UUID.fromString("d7a8ba4c-76fb-4f7c-9567-f4d38fa9ee4f");
        OrderResponse response = new OrderResponse(orderId, OrderStatus.PENDING_PAYMENT, 2599, "INR", 3);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(response));

        assertThat(json.get("orderId").asText()).isEqualTo(orderId.toString());
        assertThat(json.get("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(json.get("totalMinor").asLong()).isEqualTo(2599);
        assertThat(json.get("currency").asText()).isEqualTo("INR");
        assertThat(json.get("version").asLong()).isEqualTo(3);
    }
}
