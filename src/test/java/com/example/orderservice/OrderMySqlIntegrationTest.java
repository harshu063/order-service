package com.example.orderservice;

import com.example.orderservice.integration.CatalogClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasLength;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class OrderMySqlIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("order_service")
            .withUsername("order_user")
            .withPassword("container-only-password");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @MockBean
    CatalogClient catalog;

    @Test
    void createsOrderOnceWritesOutboxAndProtectsOwnerReads() throws Exception {
        when(catalog.getProduct(42, "test-token"))
                .thenReturn(new CatalogClient.CatalogProduct(42, "Keyboard", 2500, "USD", 7));
        String body = "{\"items\":[{\"productId\":42,\"quantity\":2}]}";
        var writer = jwt().jwt(token -> token.tokenValue("test-token").subject("user-1")
                        .claim("tenant_id", "tenant-a"))
                .authorities(new SimpleGrantedAuthority("SCOPE_orders.write"));

        String first = mvc.perform(post("/orders")
                        .with(writer)
                        .header("Idempotency-Key", "checkout-request-0001")
                        .header("X-Correlation-Id", "corr-1")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "false"))
                .andExpect(jsonPath("$.totalMinor").value(5000))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andReturn().getResponse().getContentAsString();

        mvc.perform(post("/orders")
                        .with(writer)
                        .header("Idempotency-Key", "checkout-request-0001")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.orderId").value(org.hamcrest.Matchers.is(
                        new com.fasterxml.jackson.databind.ObjectMapper().readTree(first).get("orderId").asText())));

        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("select count(*) from orders", Long.class))
                .isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("select count(*) from outbox_events", Long.class))
                .isEqualTo(1);
        String payload = jdbc.queryForObject("select payload from outbox_events", String.class);
        org.assertj.core.api.Assertions.assertThat(payload)
                .contains("order.created.v1", "corr-1", "tenant-a");

        mvc.perform(get("/orders")
                        .with(jwt().jwt(token -> token.subject("user-1").claim("tenant_id", "tenant-a"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_orders.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasLength(1)));

        mvc.perform(get("/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsIdempotencyKeyReusedForDifferentRequest() throws Exception {
        when(catalog.getProduct(42, "test-token"))
                .thenReturn(new CatalogClient.CatalogProduct(42, "Keyboard", 2500, "USD", 7));
        var writer = jwt().jwt(token -> token.tokenValue("test-token").subject("user-2"))
                .authorities(new SimpleGrantedAuthority("SCOPE_orders.write"));

        mvc.perform(post("/orders").with(writer)
                        .header("Idempotency-Key", "checkout-request-0002")
                        .contentType("application/json")
                        .content("{\"items\":[{\"productId\":42,\"quantity\":1}]}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/orders").with(writer)
                        .header("Idempotency-Key", "checkout-request-0002")
                        .contentType("application/json")
                        .content("{\"items\":[{\"productId\":42,\"quantity\":2}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.correlationId").exists());
    }
}
