package com.maang.reconciliation.consumer.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maang.reconciliation.consumer.domain.Anomaly;
import com.maang.reconciliation.consumer.domain.OutboxEvent;
import com.maang.reconciliation.consumer.infrastructure.persistence.AnomalyRepository;
import com.maang.reconciliation.consumer.infrastructure.persistence.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class RemediationService {

    private static final Logger logger = LoggerFactory.getLogger(RemediationService.class);
    private final AnomalyRepository anomalyRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RemediationService(AnomalyRepository anomalyRepository, 
                              OutboxEventRepository outboxEventRepository, 
                              StringRedisTemplate redisTemplate,
                              ObjectMapper objectMapper) {
        this.anomalyRepository = anomalyRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void resolveAnomaly(String transactionId, String idempotencyKey) {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID must not be null");
        }
        
        // 1. Check Idempotency Key
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String redisKey = "idempotency:resolve:" + idempotencyKey;
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, "processed", 24, TimeUnit.HOURS);
            if (Boolean.FALSE.equals(isNew)) {
                logger.info("ℹ️ [REMEDIATION] Request with idempotency key {} already processed.", idempotencyKey);
                return;
            }
        }

        logger.info("🛠️ [REMEDIATION] Attempting to resolve anomaly for transaction: {}", transactionId);
        
        // 2. Fetch from database
        Anomaly anomaly = anomalyRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Anomaly not found for ID: " + transactionId));

        if ("RESOLVED".equals(anomaly.getStatus())) {
            logger.info("ℹ️ [REMEDIATION] Transaction {} is already resolved.", transactionId);
            return;
        }

        // 3. Update status and save Anomaly
        anomaly.setStatus("RESOLVED");
        anomalyRepository.save(anomaly);

        // 4. Save Outbox Event (Transactional Outbox Pattern) instead of direct Kafka publish
        try {
            String payload = objectMapper.writeValueAsString(anomaly);
            OutboxEvent event = new OutboxEvent(transactionId, "Anomaly", "resolved-transactions", payload);
            outboxEventRepository.save(event);
            logger.info("✅ [REMEDIATION] Successfully resolved transaction {} and saved OutboxEvent.", transactionId);
        } catch (Exception e) {
            logger.error("🔥 [FATAL] Failed to serialize OutboxEvent payload for transaction {}. Rolling back database transaction.", transactionId, e);
            throw new RuntimeException("Outbox Event creation failed, triggering rollback", e);
        }
    }
}
