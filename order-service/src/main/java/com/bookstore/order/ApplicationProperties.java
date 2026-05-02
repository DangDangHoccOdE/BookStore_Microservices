package com.bookstore.order;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "orders")
public record ApplicationProperties(
        String catalogServiceUrl,
        String orderEventsExchange,
        String newOrdersQueue,
        String deliveredOrdersQueue,
        String cancelledOrdersQueue,
        String errorOrdersQueue,
        String deadLetterExchange,
        String newOrdersQueueRetry,
        String newOrdersQueueDlq,
        String deliveredQueueRetry,
        String deliveredQueueDlq,
        String cancelledQueueRetry,
        String cancelledQueueDlq,
        String errorQueueRetry,
        String errorQueueDlq,
        Long retryTtl,
        Keycloak keycloak) {

    public record Keycloak(String clientId) {}
}
