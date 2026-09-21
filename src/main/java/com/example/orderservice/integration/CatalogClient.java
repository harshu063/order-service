package com.example.orderservice.integration;

public interface CatalogClient {
    CatalogProduct getProduct(long productId, String bearerToken);

    record CatalogProduct(long id, String title, long priceMinor, String currency, long version) {
    }
}
