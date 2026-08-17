package com.bookstore.analytics.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "failed_kafka_events")
class FailedKafkaEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "failed_kafka_event_id_generator")
    @SequenceGenerator(name = "failed_kafka_event_id_generator", sequenceName = "failed_kafka_event_id_seq")
    private Long id;

    private String eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private Integer partitionId;

    @Column(nullable = false)
    private Long offsetValue;

    private String messageKey;

    private String exceptionMessage;

    @Column(nullable = false)
    @Lob
    private String payload;

    @Column(nullable = false)
    private LocalDateTime failedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getTopic() {
        return topic;
    }

    public Integer getPartitionId() {
        return partitionId;
    }

    public Long getOffsetValue() {
        return offsetValue;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public String getPayload() {
        return payload;
    }

    public LocalDateTime getFailedAt() {
        return failedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setPartitionId(Integer partitionId) {
        this.partitionId = partitionId;
    }

    public void setOffsetValue(Long offsetValue) {
        this.offsetValue = offsetValue;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public void setFailedAt(LocalDateTime failedAt) {
        this.failedAt = failedAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
