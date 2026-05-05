package com.maang.reconciliation.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maang.reconciliation.consumer.model.Anomaly;
import com.maang.reconciliation.consumer.model.Transaction;
import com.maang.reconciliation.consumer.repository.AnomalyRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ReconciliationService {

    private static final Logger logger = LoggerFactory.getLogger(ReconciliationService.class);
    private final StringRedisTemplate redisTemplate;
    private final AnomalyRepository anomalyRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    @Lazy
    private ReconciliationService self;

    public ReconciliationService(StringRedisTemplate redisTemplate, 
                                 AnomalyRepository anomalyRepository,
                                 KafkaTemplate<String, String> kafkaTemplate,
                                 ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.anomalyRepository = anomalyRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "cbs-logs", groupId = "reconciliation-group")
    public void consumeCbsLog(Transaction transaction) {
        logger.info("⬇️ [CBS] Received valid transaction: {}", transaction.transactionId());
        redisTemplate.opsForValue().set(transaction.transactionId(), "CBS", 5, TimeUnit.MINUTES);
    }

    @KafkaListener(topics = "datamart-logs", groupId = "reconciliation-group")
    public void consumeDatamartLog(Transaction transaction) {
        Boolean wasFoundAndDeleted = redisTemplate.delete(transaction.transactionId());

        if (Boolean.TRUE.equals(wasFoundAndDeleted)) {
            logger.info("✅ [MATCHED] Transaction safely reconciled: {}", transaction.transactionId());
        } else {
            logger.warn("⚠️ [ORPHAN] DataMart log has no matching CBS record! Saving to Vault: {}", transaction.transactionId());

            Anomaly anomaly = new Anomaly(transaction.transactionId(), "Missing in Core Banking System", System.currentTimeMillis());

            self.saveAnomalyWithCircuitBreaker(anomaly);
        }
    }

    @CircuitBreaker(name = "databaseService", fallbackMethod = "saveAnomalyFallback")
    public void saveAnomalyWithCircuitBreaker(Anomaly anomaly) {
        anomalyRepository.save(anomaly);
    }

    public void saveAnomalyFallback(Anomaly anomaly, Throwable t) {
        logger.error("🚨 [CIRCUIT OPEN] Database unavailable. Sending TXN {} to DLQ. Reason: {}", anomaly.getTransactionId(), t.getMessage());
        try {
            String anomalyJson = objectMapper.writeValueAsString(anomaly);
            kafkaTemplate.send("anomaly-dlq", anomalyJson);
        } catch (Exception e) {
            logger.error("🔥 [FATAL] Failed to serialize or send anomaly to DLQ: {}", anomaly.getTransactionId(), e);
        }
    }
}
