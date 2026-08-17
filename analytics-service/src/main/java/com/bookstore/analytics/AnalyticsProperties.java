package com.bookstore.analytics;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "analytics")
public record AnalyticsProperties(Kafka kafka) {

    public record Kafka(
            String orderEventsTopic,
            boolean failureSimulationEnabled,
            String failureEventId,
            String transientFailureEventId,
            String permanentFailureEventId) {}
}
