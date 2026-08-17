package com.bookstore.analytics.domain;

import com.bookstore.analytics.events.OrderCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FailedKafkaEventService {

    private final FailedKafkaEventRepository failedKafkaEventRepository;
    private final ObjectMapper objectMapper;

    FailedKafkaEventService(FailedKafkaEventRepository failedKafkaEventRepository, ObjectMapper objectMapper) {
        this.failedKafkaEventRepository = failedKafkaEventRepository;
        this.objectMapper = objectMapper;
    }

    public void recordFailedOrderCreatedEvent(
            OrderCreatedEvent event, ConsumerRecord<String, OrderCreatedEvent> record, String exceptionMessage) {
        FailedKafkaEventEntity failedEvent = new FailedKafkaEventEntity();
        failedEvent.setEventId(event.eventId());
        failedEvent.setEventType("OrderCreatedEvent");
        failedEvent.setTopic(record.topic());
        failedEvent.setPartitionId(record.partition());
        failedEvent.setOffsetValue(record.offset());
        failedEvent.setMessageKey(record.key());
        failedEvent.setExceptionMessage(exceptionMessage);
        failedEvent.setPayload(toJson(event));
        failedEvent.setFailedAt(LocalDateTime.now());

        failedKafkaEventRepository.save(failedEvent);
    }

    private String toJson(OrderCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize failed Kafka event", e);
        }
    }
}
