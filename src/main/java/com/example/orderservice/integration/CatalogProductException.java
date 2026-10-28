package com.example.orderservice.integration;

public class CatalogProductException extends RuntimeException {
    public CatalogProductException(String message) {
        super(message);
    }

    public CatalogProductException(String message, Throwable cause) {
        super(message, cause);
    }
}
