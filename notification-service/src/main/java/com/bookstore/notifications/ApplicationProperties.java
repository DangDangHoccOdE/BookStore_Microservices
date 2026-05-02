package com.bookstore.notifications;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notifications")
public record ApplicationProperties(
        String orderEventsExchange,
        String newOrdersQueue,
        String deliveredOrdersQueue,
        String cancelledOrdersQueue,
        String errorOrdersQueue,
        String supportEmail,
        String deadLetterExchange,
        String newOrdersQueueRetry,
        String newOrdersQueueDlq,
        String deliveredQueueRetry,
        String deliveredQueueDlq,
        String cancelledQueueRetry,
        String cancelledQueueDlq,
        String errorQueueRetry,
        String errorQueueDlq,
        Long retryTtl) {}
