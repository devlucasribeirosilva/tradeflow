package com.tradeflow.order.messaging;

import com.tradeflow.order.domain.entity.OutboxEvent;
import com.tradeflow.order.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents();

        if (pendingEvents.isEmpty()) return;

        log.info("Processing {} outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send(event.getEventType(), event.getAggregateId().toString(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to send event {} to Kafka", event.getId(), ex);
                                event.markAsFailed();
                            } else {
                                log.info("Event {} sent to topic {}", event.getId(), event.getEventType());
                                event.markAsProcessed();
                            }
                            outboxEventRepository.save(event);
                        });
            } catch (Exception e) {
                log.error("Error processing outbox event: {}", event.getId(), e);
                event.markAsFailed();
                outboxEventRepository.save(event);
            }
        }
    }
}