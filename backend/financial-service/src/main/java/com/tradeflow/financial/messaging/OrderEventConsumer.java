package com.tradeflow.financial.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeflow.financial.service.BalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final BalanceService balanceService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.created", groupId = "financial-service")
    public void onOrderCreated(String message) {
        try {
            log.info("Received order.created event: {}", message);

            Map<String, Object> event = objectMapper.readValue(message, Map.class);

            UUID orderId = UUID.fromString((String) event.get("id"));
            UUID buyerId = UUID.fromString((String) event.get("buyerId"));
            Double totalAmount = (Double) event.get("totalAmount");
            String tenantId = (String) event.get("tenantId");

            balanceService.reserveBalance(
                    buyerId, "BUYER", orderId,
                    BigDecimal.valueOf(totalAmount)
            );

            log.info("Balance reserved successfully for order: {}", orderId);

        } catch (Exception e) {
            log.error("Failed to process order.created event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process event", e);
        }
    }
}