package com.bookstore.order.config;

import com.bookstore.order.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    private final ApplicationProperties applicationProperties;

    public RabbitMQConfig(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    // ================= EXCHANGE =================
    @Bean
    DirectExchange exchange() {
        return new DirectExchange(applicationProperties.orderEventsExchange());
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(applicationProperties.deadLetterExchange());
    }

    // ================= NEW ORDERS =================
    @Bean
    Queue newOrdersQueue() {
        return QueueBuilder.durable(applicationProperties.newOrdersQueue())
                .withArgument("x-dead-letter-exchange", applicationProperties.deadLetterExchange())
                .withArgument("x-dead-letter-routing-key", applicationProperties.newOrdersQueueRetry())
                .build();
    }

    @Bean
    Binding newOrdersQueueBinding() {
        return BindingBuilder.bind(newOrdersQueue()).to(exchange()).with(applicationProperties.newOrdersQueue());
    }

    @Bean
    Queue newOrdersRetryQueue() {
        return QueueBuilder.durable(applicationProperties.newOrdersQueueRetry())
                .withArgument("x-message-ttl", applicationProperties.retryTtl())
                .withArgument("x-dead-letter-exchange", applicationProperties.orderEventsExchange())
                .withArgument("x-dead-letter-routing-key", applicationProperties.newOrdersQueue())
                .build();
    }

    @Bean
    Binding newOrdersRetryQueueBinding() {
        return BindingBuilder.bind(newOrdersRetryQueue())
                .to(deadLetterExchange())
                .with(applicationProperties.newOrdersQueueRetry());
    }

    @Bean
    Queue newOrdersDLQ() {
        return QueueBuilder.durable(applicationProperties.newOrdersQueueDlq()).build();
    }

    @Bean
    Binding newOrdersDLQBinding() {
        return BindingBuilder.bind(newOrdersDLQ())
                .to(deadLetterExchange())
                .with(applicationProperties.newOrdersQueueDlq());
    }

    // ================= DELIVERED =================
    @Bean
    Queue deliveredOrdersQueue() {
        return QueueBuilder.durable(applicationProperties.deliveredOrdersQueue())
                .withArgument("x-dead-letter-exchange", applicationProperties.deadLetterExchange())
                .withArgument("x-dead-letter-routing-key", applicationProperties.deliveredQueueRetry())
                .build();
    }

    @Bean
    Binding deliveredOrdersQueueBinding() {
        return BindingBuilder.bind(deliveredOrdersQueue())
                .to(exchange())
                .with(applicationProperties.deliveredOrdersQueue());
    }

    @Bean
    Queue deliveredOrdersRetryQueue() {
        return QueueBuilder.durable(applicationProperties.deliveredQueueRetry())
                .withArgument("x-message-ttl", applicationProperties.retryTtl())
                .withArgument("x-dead-letter-exchange", applicationProperties.orderEventsExchange())
                .withArgument("x-dead-letter-routing-key", applicationProperties.deliveredOrdersQueue())
                .build();
    }

    @Bean
    Binding deliveredOrdersRetryQueueBinding() {
        return BindingBuilder.bind(deliveredOrdersRetryQueue())
                .to(deadLetterExchange())
                .with(applicationProperties.deliveredQueueRetry());
    }

    @Bean
    Queue deliveredOrdersDLQ() {
        return QueueBuilder.durable(applicationProperties.deliveredQueueDlq()).build();
    }

    @Bean
    Binding deliveredOrdersDLQBinding() {
        return BindingBuilder.bind(deliveredOrdersDLQ())
                .to(deadLetterExchange())
                .with(applicationProperties.deliveredQueueDlq());
    }

    // ================= CANCELLED =================
    @Bean
    Queue cancelledOrdersQueue() {
        return QueueBuilder.durable(applicationProperties.cancelledOrdersQueue())
                .withArgument("x-dead-letter-exchange", applicationProperties.deadLetterExchange())
                .withArgument("x-dead-letter-routing-key", applicationProperties.cancelledQueueRetry())
                .build();
    }

    @Bean
    Binding cancelledOrdersQueueBinding() {
        return BindingBuilder.bind(cancelledOrdersQueue())
                .to(exchange())
                .with(applicationProperties.cancelledOrdersQueue());
    }

    @Bean
    Queue cancelledRetryQueue() {
        return QueueBuilder.durable(applicationProperties.cancelledQueueRetry())
                .withArgument("x-message-ttl", applicationProperties.retryTtl())
                .withArgument("x-dead-letter-exchange", applicationProperties.orderEventsExchange())
                .withArgument("x-dead-letter-routing-key", applicationProperties.cancelledOrdersQueue())
                .build();
    }

    @Bean
    Binding cancelledRetryBinding() {
        return BindingBuilder.bind(cancelledRetryQueue())
                .to(deadLetterExchange())
                .with(applicationProperties.cancelledQueueRetry());
    }

    @Bean
    Queue cancelledDLQ() {
        return QueueBuilder.durable(applicationProperties.cancelledQueueDlq()).build();
    }

    @Bean
    Binding cancelledDLQBinding() {
        return BindingBuilder.bind(cancelledDLQ())
                .to(deadLetterExchange())
                .with(applicationProperties.cancelledQueueDlq());
    }

    // ================= ERROR =================
    @Bean
    Queue errorOrdersQueue() {
        return QueueBuilder.durable(applicationProperties.errorOrdersQueue())
                .withArgument("x-dead-letter-exchange", applicationProperties.deadLetterExchange())
                .withArgument("x-dead-letter-routing-key", applicationProperties.errorQueueRetry())
                .build();
    }

    @Bean
    Binding errorOrdersQueueBinding() {
        return BindingBuilder.bind(errorOrdersQueue()).to(exchange()).with(applicationProperties.errorOrdersQueue());
    }

    @Bean
    Queue errorRetryQueue() {
        return QueueBuilder.durable(applicationProperties.errorQueueRetry())
                .withArgument("x-message-ttl", applicationProperties.retryTtl())
                .withArgument("x-dead-letter-exchange", applicationProperties.orderEventsExchange())
                .withArgument("x-dead-letter-routing-key", applicationProperties.errorOrdersQueue())
                .build();
    }

    @Bean
    Binding errorRetryBinding() {
        return BindingBuilder.bind(errorRetryQueue())
                .to(deadLetterExchange())
                .with(applicationProperties.errorQueueRetry());
    }

    @Bean
    Queue errorDLQ() {
        return QueueBuilder.durable(applicationProperties.errorQueueDlq()).build();
    }

    @Bean
    Binding errorDLQBinding() {
        return BindingBuilder.bind(errorDLQ()).to(deadLetterExchange()).with(applicationProperties.errorQueueDlq());
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);

        // confirm exchange receive message
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("Message NOT delivered to exchange: {}", cause);
            }
        });

        // return routing fail
        template.setReturnsCallback(returned -> {
            log.error("Message returned: routingKey={}, message={}", returned.getRoutingKey(), returned.getMessage());
        });

        template.setMandatory(true);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
