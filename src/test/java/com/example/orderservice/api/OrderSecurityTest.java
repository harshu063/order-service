package com.example.orderservice.api;

import com.example.orderservice.application.OrderApplicationService;
import com.example.orderservice.application.OrderModels;
import com.example.orderservice.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({SecurityConfiguration.class, CorrelationIdFilter.class, ApiExceptionHandler.class})
@TestPropertySource(properties = {
        "security.jwt.jwk-set-uri=http://localhost/unused-jwks",
        "security.jwt.issuer=http://localhost/issuer",
        "security.jwt.audience=platform-api"
})
class OrderSecurityTest {
    @Autowired
    MockMvc mvc;

    @MockBean
    OrderApplicationService orders;

    @Test
    void requiresReadScopeForOrderQueries() throws Exception {
        when(orders.listOwnOrders(any(), isNull(), anyInt()))
                .thenReturn(new OrderModels.OrderPageView(List.of(), null));

        mvc.perform(get("/orders"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/orders").with(jwt()
                        .authorities(new SimpleGrantedAuthority("SCOPE_orders.write"))))
                .andExpect(status().isForbidden());

        mvc.perform(get("/orders").with(jwt()
                        .authorities(new SimpleGrantedAuthority("SCOPE_orders.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }
}
