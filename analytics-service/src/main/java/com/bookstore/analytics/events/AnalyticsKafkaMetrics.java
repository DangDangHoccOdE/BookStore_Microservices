package com.bookstore.analytics.events;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsKafkaMetrics {

    private final Counter consumedEvents;
    private final Counter processedEvents;
    private final Counter duplicateSkippedEvents;
    private final Counter failedBeforeProcessingEvents;
    private final Counter dltEvents;

    public AnalyticsKafkaMetrics(MeterRegistry meterRegistry) {
        this.consumedEvents = Counter.builder("bookstore.kafka.analytics.events.consumed")
                .description("Number of Kafka OrderCreatedEvent records consumed by analytics-service")
                .register(meterRegistry);

        this.processedEvents = Counter.builder("bookstore.kafka.analytics.events.processed")
                .description("Number of Kafka OrderCreatedEvent records successfully processed by analytics-service")
                .register(meterRegistry);

        this.duplicateSkippedEvents = Counter.builder("bookstore.kafka.analytics.events.duplicates.skipped")
                .description("Number of duplicate Kafka OrderCreatedEvent records skipped by idempotent consumer")
                .register(meterRegistry);

        this.failedBeforeProcessingEvents = Counter.builder("bookstore.kafka.analytics.events.failed.before.processing")
                .description("Number of simulated Kafka consumer failures before business processing")
                .register(meterRegistry);

        this.dltEvents = Counter.builder("bookstore.kafka.analytics.events.dlt")
                .description("Number of Kafka OrderCreatedEvent records handled from dead letter topic")
                .register(meterRegistry);
    }

    public void incrementConsumedEvents() {
        consumedEvents.increment();
    }

    public void incrementProcessedEvents() {
        processedEvents.increment();
    }

    public void incrementDuplicateSkippedEvents() {
        duplicateSkippedEvents.increment();
    }

    public void incrementFailedBeforeProcessingEvents() {
        failedBeforeProcessingEvents.increment();
    }

    public void incrementDltEvents() {
        dltEvents.increment();
    }
}
