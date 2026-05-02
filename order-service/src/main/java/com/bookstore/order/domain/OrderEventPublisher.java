package com.bookstore.order.domain;

import com.bookstore.order.ApplicationProperties;
import com.bookstore.order.domain.models.OrderCancelledEvent;
import com.bookstore.order.domain.models.OrderCreatedEvent;
import com.bookstore.order.domain.models.OrderDeliveredEvent;
import com.bookstore.order.domain.models.OrderErrorEvent;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ApplicationProperties properties;

    OrderEventPublisher(RabbitTemplate rabbitTemplate, ApplicationProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    public void publish(OrderCreatedEvent orderCreatedEvent) {
        this.send(properties.newOrdersQueue(), orderCreatedEvent);
    }

    public void publish(OrderDeliveredEvent event) {
        this.send(properties.deliveredOrdersQueue(), event);
    }

    public void publish(OrderCancelledEvent event) {
        this.send(properties.cancelledOrdersQueue(), event);
    }

    public void publish(OrderErrorEvent event) {
        this.send(properties.errorOrdersQueue(), event);
    }

    @Retry(name = "rabbit", fallbackMethod = "fallbackSend")
    private void send(String routingKey, Object payload) {
        rabbitTemplate.convertAndSend(properties.orderEventsExchange(), routingKey, payload, message -> {
            message.getMessageProperties().setCorrelationId(UUID.randomUUID().toString());
            return message;
        });
    }

    private void fallbackSend(String routingKey, Object payload, Throwable ex) {
        log.error("Failed to publish message after retries: {}", payload, ex);
    }
}
