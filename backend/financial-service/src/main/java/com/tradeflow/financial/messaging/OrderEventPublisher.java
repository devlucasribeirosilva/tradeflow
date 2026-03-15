package com.tradeflow.financial.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publishOrderValidated(UUID orderId, UUID buyerId) {
        String payload = String.format(
                "{\"orderId\":\"%s\",\"buyerId\":\"%s\",\"status\":\"VALIDATED\"}",
                orderId, buyerId
        );
        kafkaTemplate.send("order.validated", orderId.toString(), payload);
        log.info("Published order.validated for order: {}", orderId);
    }
}