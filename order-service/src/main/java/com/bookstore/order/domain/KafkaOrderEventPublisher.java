package com.bookstore.order.domain;

import com.bookstore.order.ApplicationProperties;
import com.bookstore.order.domain.models.OrderCreatedEvent;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
public class KafkaOrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderEventPublisher.class);

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final ApplicationProperties properties;

    KafkaOrderEventPublisher(KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate, ApplicationProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    public void publish(OrderCreatedEvent event) {
        String topic = properties.kafka().orderEventsTopic();
        String key = event.orderNumber();

        try {
            SendResult<String, OrderCreatedEvent> result =
                    kafkaTemplate.send(topic, key, event).get();

            log.info(
                    "Published OrderCreatedEvent to Kafka topic={}, partition={}, offset={}, key={}, eventId={}, orderNumber={}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    key,
                    event.eventId(),
                    event.orderNumber());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while publishing OrderCreatedEvent to Kafka", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to publish OrderCreatedEvent to Kafka", e);
        }
    }
}
