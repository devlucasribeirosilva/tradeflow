package com.tradeflow.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name("order.created")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderValidatedTopic() {
        return TopicBuilder.name("order.validated")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderSettledTopic() {
        return TopicBuilder.name("order.settled")
                .partitions(3)
                .replicas(1)
                .build();
    }
}