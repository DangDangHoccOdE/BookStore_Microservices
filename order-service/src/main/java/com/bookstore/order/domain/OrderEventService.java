package com.bookstore.order.domain;

import com.bookstore.order.domain.models.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderEventService {
    private static final Logger log = LoggerFactory.getLogger(OrderEventService.class);

    private final OrderEventRepository orderEventRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${orders.outbox.max-retry}")
    private int maxRetry;

    OrderEventService(
            OrderEventRepository orderEventRepository,
            OrderEventPublisher orderEventPublisher,
            ObjectMapper objectMapper) {
        this.orderEventRepository = orderEventRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.objectMapper = objectMapper;
    }

    void save(OrderCreatedEvent event) {
        OrderEventEntity orderEvent = new OrderEventEntity();
        orderEvent.setEventId(event.eventId());
        orderEvent.setEventType(OrderEventType.ORDER_CREATED);
        orderEvent.setOrderNumber(event.orderNumber());
        orderEvent.setCreatedAt(event.createdAt());
        orderEvent.setPayload(toJsonPayload(event));
        this.orderEventRepository.save(orderEvent);
    }

    void save(OrderDeliveredEvent event) {
        OrderEventEntity orderEvent = new OrderEventEntity();
        orderEvent.setEventId(event.eventId());
        orderEvent.setEventType(OrderEventType.ORDER_DELIVERED);
        orderEvent.setOrderNumber(event.orderNumber());
        orderEvent.setCreatedAt(event.createdAt());
        orderEvent.setPayload(toJsonPayload(event));
        this.orderEventRepository.save(orderEvent);
    }

    void save(OrderCancelledEvent event) {
        OrderEventEntity orderEvent = new OrderEventEntity();
        orderEvent.setEventId(event.eventId());
        orderEvent.setEventType(OrderEventType.ORDER_CANCELLED);
        orderEvent.setOrderNumber(event.orderNumber());
        orderEvent.setCreatedAt(event.createdAt());
        orderEvent.setPayload(toJsonPayload(event));
        this.orderEventRepository.save(orderEvent);
    }

    void save(OrderErrorEvent event) {
        OrderEventEntity orderEvent = new OrderEventEntity();
        orderEvent.setEventId(event.eventId());
        orderEvent.setEventType(OrderEventType.ORDER_PROCESSING_FAILED);
        orderEvent.setOrderNumber(event.orderNumber());
        orderEvent.setCreatedAt(event.createdAt());
        orderEvent.setPayload(toJsonPayload(event));
        this.orderEventRepository.save(orderEvent);
    }

    //    lấy batch PENDING
    //    mark thành PROCESSING
    //    publish từng event
    //    publish xong thì mark PUBLISHED
    //    nếu lỗi thì mark FAILED và tăng retry
    public void publishOrderEvents() {
        List<OrderEventEntity> events = orderEventRepository.lockPendingEvents(50);
        log.info("Found {} Order Events to be published", events.size());

        for (OrderEventEntity event : events) {
            try {
                event.setStatus(OutboxStatus.PROCESSING);
                event.setLockedAt(LocalDateTime.now());
                event.setLockedBy("order-service");
                // Đã có job chạy để update về PENDING nếu PROCESSING quá lâu -> crash app
                orderEventRepository.save(event);

                publishEvent(event);

                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(LocalDateTime.now());
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);

                // set biến này để cho phép gửi lại
                if (event.getRetryCount() < maxRetry) {
                    event.setStatus(OutboxStatus.PENDING);
                } else {
                    event.setStatus(OutboxStatus.FAILED);
                }

                log.error("Publish failed for eventId={}, reason={}", event.getEventId(), e.getMessage(), e);
            } finally {
                orderEventRepository.save(event);
            }
        }
    }

    private void publishEvent(OrderEventEntity orderEvent) {
        OrderEventType orderEventType = orderEvent.getEventType();
        switch (orderEventType) {
            case ORDER_CREATED:
                OrderCreatedEvent orderCreatedEvent = fromJsonPayload(orderEvent.getPayload(), OrderCreatedEvent.class);
                orderEventPublisher.publish(orderCreatedEvent);
                break;
            case ORDER_DELIVERED:
                OrderDeliveredEvent orderDeliveredEvent =
                        fromJsonPayload(orderEvent.getPayload(), OrderDeliveredEvent.class);
                orderEventPublisher.publish(orderDeliveredEvent);
                break;
            case ORDER_CANCELLED:
                OrderCancelledEvent orderCancelledEvent =
                        fromJsonPayload(orderEvent.getPayload(), OrderCancelledEvent.class);
                orderEventPublisher.publish(orderCancelledEvent);
                break;
            case ORDER_PROCESSING_FAILED:
                OrderErrorEvent orderErrorEvent = fromJsonPayload(orderEvent.getPayload(), OrderErrorEvent.class);
                orderEventPublisher.publish(orderErrorEvent);
                break;
            default:
                log.warn("Unsupported OrderEventType: {}", orderEventType);
        }
    }

    private String toJsonPayload(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T fromJsonPayload(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
