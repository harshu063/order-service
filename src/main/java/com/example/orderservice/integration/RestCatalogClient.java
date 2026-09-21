package com.example.orderservice.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

@Component
class RestCatalogClient implements CatalogClient {
    private final RestClient client;

    RestCatalogClient(RestClient.Builder builder,
                      @Value("${catalog.base-url}") String baseUrl,
                      @Value("${catalog.connect-timeout}") Duration connectTimeout,
                      @Value("${catalog.read-timeout}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.client = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public CatalogProduct getProduct(long productId, String bearerToken) {
        try {
            CatalogProduct product = client.get()
                    .uri("/products/{id}", productId)
                    .headers(headers -> headers.setBearerAuth(bearerToken))
                    .retrieve()
                    .body(CatalogProduct.class);
            if (product == null) {
                throw new CatalogProductException("Catalog returned no product for id " + productId);
            }
            return product;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new CatalogProductException("Product " + productId + " does not exist");
            }
            throw new CatalogProductException("Catalog could not validate product " + productId, exception);
        } catch (CatalogProductException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new CatalogProductException("Catalog is unavailable", exception);
        }
    }
}
