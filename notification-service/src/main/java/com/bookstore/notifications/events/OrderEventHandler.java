package com.bookstore.notifications.events;

import com.bookstore.notifications.domain.NotificationService;
import com.bookstore.notifications.domain.OrderEventRepository;
import com.bookstore.notifications.domain.models.OrderCancelledEvent;
import com.bookstore.notifications.domain.models.OrderCreatedEvent;
import com.bookstore.notifications.domain.models.OrderDeliveredEvent;
import com.bookstore.notifications.domain.models.OrderErrorEvent;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventHandler {
    private static final Logger log = LoggerFactory.getLogger(OrderEventHandler.class);
    private final NotificationService notificationService;
    private final OrderEventRepository orderEventRepository;

    public OrderEventHandler(NotificationService notificationService, OrderEventRepository orderEventRepository) {
        this.notificationService = notificationService;
        this.orderEventRepository = orderEventRepository;
    }

    @RabbitListener(queues = "${notifications.new-orders-queue}")
    @Transactional
    void handleOrderCreatedEvent(OrderCreatedEvent event) {
        process(event.eventId(), "OrderCreatedEvent", () -> {
            log.info("Received a OrderCreatedEvent with orderNumber:{}: ", event.orderNumber());
            notificationService.sendOrderCreatedNotification(event);
        });
    }

    @RabbitListener(queues = "${notifications.delivered-orders-queue}")
    @Transactional
    void handleOrderDeliveredEvent(OrderDeliveredEvent event) {
        process(event.eventId(), "OrderDeliveredEvent", () -> {
            log.info("Received a OrderDeliveredEvent with orderNumber:{}: ", event.orderNumber());
            notificationService.sendOrderDeliveredNotification(event);
        });
    }

    @RabbitListener(queues = "${notifications.cancelled-orders-queue}")
    @Transactional
    void handleOrderCancelledEvent(OrderCancelledEvent event) {
        process(event.eventId(), "OrderCancelledEvent", () -> {
            log.info("Received a OrderCancelledEvent with orderNumber:{}: ", event.orderNumber());
            notificationService.sendOrderCancelledNotification(event);
        });
    }

    @RabbitListener(queues = "${notifications.error-orders-queue}")
    @Transactional
    void handleOrderErrorEvent(OrderErrorEvent event) {
        process(event.eventId(), "OrderErrorEvent", () -> {
            log.info("Received a OrderErrorEvent with orderNumber:{}: ", event.orderNumber());
            notificationService.sendOrderErrorEventNotification(event);
        });
    }

    private void process(String eventId, String eventType, Runnable sendAction) {
        int started = orderEventRepository.tryStartProcessing(eventId);

        if (started == 0) {
            log.warn("Duplicate {} with eventId={} -> skipped", eventType, eventId);
            return;
        }

        try {
            sendAction.run();
            orderEventRepository.markSent(eventId);
            log.info("{} with eventId={} processed successfully", eventType, eventId);
        } catch (Exception ex) {
            orderEventRepository.markFailed(eventId, ex.getMessage());
            log.error("Failed to process {} with eventId={}: {}", eventType, eventId, ex.getMessage());
            throw ex;
        }
    }
}
