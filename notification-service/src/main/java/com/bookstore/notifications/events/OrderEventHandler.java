package com.bookstore.notifications.events;

import com.bookstore.notifications.ApplicationProperties;
import com.bookstore.notifications.domain.EmailService;
import com.bookstore.notifications.domain.NotificationService;
import com.bookstore.notifications.domain.OrderEventRepository;
import com.bookstore.notifications.domain.models.*;
import com.rabbitmq.client.Channel;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventHandler {
    private static final Logger log = LoggerFactory.getLogger(OrderEventHandler.class);
    private final NotificationService notificationService;
    private final OrderEventRepository orderEventRepository;
    private final EmailService emailService;
    private final RabbitTemplate rabbitTemplate;
    private final ApplicationProperties applicationProperties;

    public OrderEventHandler(
            NotificationService notificationService,
            OrderEventRepository orderEventRepository,
            EmailService emailService,
            RabbitTemplate rabbitTemplate,
            ApplicationProperties applicationProperties) {
        this.notificationService = notificationService;
        this.orderEventRepository = orderEventRepository;
        this.emailService = emailService;
        this.rabbitTemplate = rabbitTemplate;
        this.applicationProperties = applicationProperties;
    }

    private <T> void handleEvent(
            T event, String eventId, String eventType, Message message, Channel channel, Runnable businessLogic)
            throws Exception {
        long tag = message.getMessageProperties().getDeliveryTag();

        try {
            process(eventId, eventType, businessLogic);

            // SUCCESS
            channel.basicAck(tag, false);
            log.info("Order event {} has been processed", eventId);
        } catch (Exception e) {
            int retry = getRetryCount(message);
            log.warn("Retry count from Rabbit = {}", retry);

            if (retry >= 5) {
                // Push to DLQ
                log.error("Send to FINAL DLQ eventId={}", eventId);

                rabbitTemplate.convertAndSend(
                        applicationProperties.deadLetterExchange(), applicationProperties.newOrdersQueueDlq(), message);

                channel.basicAck(tag, false);
            } else {
                // Push to retry Queue
                log.info("Retrying eventId={}, retryCount={}", eventId, retry);
                channel.basicNack(tag, false, false);
            }
        }
    }

    @RabbitListener(queues = "${notifications.new-orders-queue}")
    @Transactional
    void handleOrderCreatedEvent(OrderCreatedEvent event, Message message, Channel channel) throws Exception {

        handleEvent(event, event.eventId(), "OrderCreatedEvent", message, channel, () -> {
            // Đã test ok, nó sẽ vào queue retry và retry
            // Quá fail thì thì vào dlq
            //                    if (true) {
            //                        throw new RuntimeException("Simulate crash before ACK");
            //                    }
            log.info("Received order created event {}", event.orderNumber());

            EmailMessage emailMessage = notificationService.sendOrderCreatedNotification(event);
            emailService.sendEmail(emailMessage); // async
        });
    }

    @RabbitListener(queues = "${notifications.delivered-orders-queue}")
    @Transactional
    void handleOrderDeliveredEvent(OrderDeliveredEvent event, Message message, Channel channel) throws Exception {
        handleEvent(event, event.eventId(), "OrderDeliveredEvent", message, channel, () -> {
            log.info("Received OrderDeliveredEvent {}", event.orderNumber());

            EmailMessage email = notificationService.sendOrderDeliveredNotification(event);
            emailService.sendEmail(email);
        });
    }

    @RabbitListener(queues = "${notifications.cancelled-orders-queue}")
    @Transactional
    void handleOrderCancelledEvent(OrderCancelledEvent event, Message message, Channel channel) throws Exception {
        handleEvent(event, event.eventId(), "OrderCancelledEvent", message, channel, () -> {
            log.info("Received OrderCancelledEvent {}", event.orderNumber());

            EmailMessage email = notificationService.sendOrderCancelledNotification(event);
            emailService.sendEmail(email);
        });
    }

    @RabbitListener(queues = "${notifications.error-orders-queue}")
    @Transactional
    void handleOrderErrorEvent(OrderErrorEvent event, Message message, Channel channel) throws Exception {
        handleEvent(event, event.eventId(), "OrderErrorEvent", message, channel, () -> {
            log.info("Received OrderErrorEvent {}", event.orderNumber());

            EmailMessage email = notificationService.sendOrderErrorEventNotification(event);
            emailService.sendEmail(email);
        });
    }

    private void process(String eventId, String eventType, Runnable sendAction) {
        int inserted = orderEventRepository.insertIfNotExists(eventId);
        boolean canProcess = false;

        if (inserted == 1) {
            // first time processing
            canProcess = true;
        } else {
            // 2. Nếu đã tồn tại → check retry (FAILED)
            int updated = orderEventRepository.retryIfFailed(eventId);

            if (updated == 1) {
                log.info("Retrying FAILED {} with eventId={}", eventType, eventId);
                canProcess = true;
            }
        }

        if (!canProcess) {
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

    private int getRetryCount(Message message) {
        var xDeath = message.getMessageProperties().getHeaders().get("x-death");
        log.info("x-death = {}", message.getMessageProperties().getHeaders().get("x-death"));

        if (xDeath == null) return 0;

        String currentQueue = message.getMessageProperties().getConsumerQueue();

        return ((List<Map<String, Object>>) xDeath)
                .stream()
                        .filter(entry -> currentQueue.equals(entry.get("queue")))
                        .map(entry -> (Long) entry.get("count"))
                        .findFirst()
                        .orElse(0L)
                        .intValue();
    }
}
