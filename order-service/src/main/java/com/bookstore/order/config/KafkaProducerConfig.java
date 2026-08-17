package com.bookstore.order.config;

import com.bookstore.order.ApplicationProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaProducerConfig {

    private final ApplicationProperties properties;

    public KafkaProducerConfig(ApplicationProperties properties) {
        this.properties = properties;
    }

    @Bean
    NewTopic orderEventsTopic() {
        return TopicBuilder.name(properties.kafka().orderEventsTopic())
                .partitions(3)
                .replicas(1)
                .config("min.insync.replicas", "1")
                .build();
    }
}
