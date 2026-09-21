package com.example.orderservice.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
class CorrelationIdFilter extends OncePerRequestFilter {
    static final String HEADER = "X-Correlation-Id";
    static final String ATTRIBUTE = CorrelationIdFilter.class.getName() + ".correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String supplied = request.getHeader(HEADER);
        String correlationId = supplied == null || supplied.isBlank() || supplied.length() > 128
                ? UUID.randomUUID().toString()
                : supplied;
        request.setAttribute(ATTRIBUTE, correlationId);
        response.setHeader(HEADER, correlationId);
        filterChain.doFilter(request, response);
    }
}
