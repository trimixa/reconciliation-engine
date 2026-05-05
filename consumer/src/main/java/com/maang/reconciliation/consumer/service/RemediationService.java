package com.maang.reconciliation.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maang.reconciliation.consumer.model.Anomaly;
import com.maang.reconciliation.consumer.repository.AnomalyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RemediationService {

    private static final Logger logger = LoggerFactory.getLogger(RemediationService.class);
    private final AnomalyRepository anomalyRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public RemediationService(AnomalyRepository anomalyRepository, 
                              KafkaTemplate<String, String> kafkaTemplate, 
                              ObjectMapper objectMapper) {
        this.anomalyRepository = anomalyRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void resolveAnomaly(String transactionId) {
        logger.info("🛠️ [REMEDIATION] Attempting to resolve anomaly for transaction: {}", transactionId);
        
        // 1. Fetch from database
        Anomaly anomaly = anomalyRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Anomaly not found for ID: " + transactionId));

        if ("RESOLVED".equals(anomaly.getStatus())) {
            logger.info("ℹ️ [REMEDIATION] Transaction {} is already resolved.", transactionId);
            return;
        }

        // 2. Update status and save
        anomaly.setStatus("RESOLVED");
        anomalyRepository.save(anomaly);

        // 3. Publish to Kafka
        try {
            String payload = objectMapper.writeValueAsString(anomaly);
            kafkaTemplate.send("resolved-transactions", payload);
            logger.info("✅ [REMEDIATION] Successfully resolved transaction {} and published to Kafka.", transactionId);
        } catch (Exception e) {
            logger.error("🔥 [FATAL] Failed to publish resolved transaction {} to Kafka. Rolling back database transaction.", transactionId, e);
            throw new RuntimeException("Kafka publish failed, triggering rollback", e);
        }
    }
}
