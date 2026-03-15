package com.tradeflow.order.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeflow.order.domain.entity.OutboxEvent;
import com.tradeflow.order.repository.OutboxEventRepository;
import com.tradeflow.order.web.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void publishOrderCreated(OrderResponse order) {
        try {
            String payload = objectMapper.writeValueAsString(order);
            OutboxEvent event = new OutboxEvent(
                    order.id(),
                    "Order",
                    "order.created",
                    payload
            );
            outboxEventRepository.save(event);
            log.info("Outbox event saved for order: {}", order.id());
        } catch (Exception e) {
            log.error("Failed to save outbox event for order: {}", order.id(), e);
            throw new RuntimeException("Failed to publish order event", e);
        }
    }
}