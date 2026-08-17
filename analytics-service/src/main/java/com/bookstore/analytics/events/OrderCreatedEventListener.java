package com.bookstore.analytics.events;

import com.bookstore.analytics.AnalyticsProperties;
import com.bookstore.analytics.domain.FailedKafkaEventService;
import com.bookstore.analytics.domain.OrderAnalyticsService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventListener.class);

    private final Map<String, Integer> transientFailures = new ConcurrentHashMap<>();

    private final OrderAnalyticsService orderAnalyticsService;
    private final FailedKafkaEventService failedKafkaEventService;
    private final AnalyticsKafkaMetrics metrics;
    private final AnalyticsProperties properties;

    public OrderCreatedEventListener(
            OrderAnalyticsService orderAnalyticsService,
            FailedKafkaEventService failedKafkaEventService,
            AnalyticsKafkaMetrics metrics,
            AnalyticsProperties properties) {
        this.orderAnalyticsService = orderAnalyticsService;
        this.failedKafkaEventService = failedKafkaEventService;
        this.metrics = metrics;
        this.properties = properties;
    }

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 5000),
            retryTopicSuffix = "-retry",
            dltTopicSuffix = "-dlt",
            kafkaTemplate = "kafkaTemplate")
    @KafkaListener(topics = "${analytics.kafka.order-events-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCreatedEvent(OrderCreatedEvent event, ConsumerRecord<String, OrderCreatedEvent> record) {
        metrics.incrementConsumedEvents();

        log.info(
                "Consumed OrderCreatedEvent topic={}, partition={}, offset={}, key={}, eventId={}, orderNumber={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                event.eventId(),
                event.orderNumber());

        simulateRetryableFailuresBeforeProcessing(event);

        long itemCount = event.items().stream().mapToLong(OrderItem::quantity).sum();

        boolean processed = orderAnalyticsService.recordOrderCreated(event.eventId(), itemCount);

        if (!processed) {
            metrics.incrementDuplicateSkippedEvents();
            log.info(
                    "OrderCreatedEvent already processed, committing offset without updating analytics. eventId={}, orderNumber={}",
                    event.eventId(),
                    event.orderNumber());
            return;
        }

        metrics.incrementProcessedEvents();

        log.info(
                "Processed OrderCreatedEvent eventId={}, orderNumber={}, itemCount={}",
                event.eventId(),
                event.orderNumber(),
                itemCount);

        simulateCrashAfterDatabaseProcessing(event);
    }

    @DltHandler
    public void handleDltOrderCreatedEvent(OrderCreatedEvent event, ConsumerRecord<String, OrderCreatedEvent> record) {
        metrics.incrementDltEvents();

        String reason = "Retry attempts exhausted; record moved to DLT";

        log.error(
                "OrderCreatedEvent moved to DLT topic={}, partition={}, offset={}, key={}, eventId={}, reason={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                event.eventId(),
                reason);

        failedKafkaEventService.recordFailedOrderCreatedEvent(event, record, reason);
    }

    private void simulateRetryableFailuresBeforeProcessing(OrderCreatedEvent event) {
        if (properties.kafka().permanentFailureEventId().equals(event.eventId())) {
            metrics.incrementFailedBeforeProcessingEvents();
            log.warn("Simulating permanent failure before processing eventId={}", event.eventId());
            throw new RuntimeException("Permanent failure simulation for eventId=" + event.eventId());
        }

        if (!properties.kafka().transientFailureEventId().equals(event.eventId())) {
            return;
        }

        int failureCount = transientFailures.merge(event.eventId(), 1, Integer::sum);

        if (failureCount <= 2) {
            metrics.incrementFailedBeforeProcessingEvents();
            log.warn(
                    "Simulating transient failure attempt={} before processing eventId={}",
                    failureCount,
                    event.eventId());
            throw new RuntimeException(
                    "Transient failure simulation attempt=" + failureCount + " for eventId=" + event.eventId());
        }

        log.info(
                "Transient failure recovered for eventId={} after {} failed attempts",
                event.eventId(),
                failureCount - 1);
    }

    private void simulateCrashAfterDatabaseProcessing(OrderCreatedEvent event) {
        if (!properties.kafka().failureSimulationEnabled()) {
            return;
        }

        if (!properties.kafka().failureEventId().equals(event.eventId())) {
            return;
        }

        log.error(
                "Simulating consumer failure after database processing for eventId={}. Offset will not be committed as success.",
                event.eventId());

        throw new RuntimeException("Simulated consumer failure after database processing");
    }
}
