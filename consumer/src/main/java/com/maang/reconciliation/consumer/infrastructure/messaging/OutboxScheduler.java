package com.maang.reconciliation.consumer.infrastructure.messaging;

import com.maang.reconciliation.consumer.domain.OutboxEvent;
import com.maang.reconciliation.consumer.infrastructure.persistence.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OutboxScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OutboxScheduler.class);
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxScheduler(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "5000")
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();
        
        if (!pendingEvents.isEmpty()) {
            logger.info("📦 [OUTBOX] Found {} pending outbox events to process.", pendingEvents.size());
        }

        for (OutboxEvent event : pendingEvents) {
            try {
                // Publish to Kafka
                kafkaTemplate.send(java.util.Objects.requireNonNull(event.getTopic()), java.util.Objects.requireNonNull(event.getPayload()));
                
                // Mark as processed
                event.setProcessed(true);
                outboxEventRepository.save(event);
                
                logger.debug("✅ [OUTBOX] Processed and published event ID: {}", event.getEventId());
            } catch (Exception e) {
                logger.error("🔥 [OUTBOX ERROR] Failed to process event ID: {}", event.getEventId(), e);
            }
        }
    }
}
