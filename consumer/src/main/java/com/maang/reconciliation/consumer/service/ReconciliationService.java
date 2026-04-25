package com.maang.reconciliation.consumer.service;

import com.maang.reconciliation.consumer.model.Anomaly;
import com.maang.reconciliation.consumer.model.Transaction;
import com.maang.reconciliation.consumer.repository.AnomalyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ReconciliationService {

    private static final Logger logger = LoggerFactory.getLogger(ReconciliationService.class);

    private final StringRedisTemplate redisTemplate;
    private final AnomalyRepository anomalyRepository;

    // Injecting both our Cache (Redis) and our Vault (Postgres)
    public ReconciliationService(StringRedisTemplate redisTemplate, AnomalyRepository anomalyRepository) {
        this.redisTemplate = redisTemplate;
        this.anomalyRepository = anomalyRepository;
    }

    @KafkaListener(topics = "cbs-logs", groupId = "reconciliation-group")
    public void consumeCbsLog(Transaction transaction) {
        redisTemplate.opsForValue().set(transaction.transactionId(), "PENDING", Duration.ofSeconds(60));
        logger.info("⏳ [PENDING in Redis] Waiting for DataMart match: {}", transaction.transactionId());
    }

    @KafkaListener(topics = "datamart-logs", groupId = "reconciliation-group")
    public void consumeDatamartLog(Transaction transaction) {
        Boolean wasFoundAndDeleted = redisTemplate.delete(transaction.transactionId());

        if (Boolean.TRUE.equals(wasFoundAndDeleted)) {
            logger.info("✅ [MATCHED] Transaction safely reconciled: {}", transaction.transactionId());
        } else {
            logger.warn("⚠️ [ORPHAN] DataMart log has no matching CBS record! Saving to Vault: {}", transaction.transactionId());

            // 1. Create the Anomaly Record
            Anomaly anomaly = new Anomaly(
                    transaction.transactionId(),
                    "Missing in Core Banking System",
                    System.currentTimeMillis()
            );

            // 2. Permanently save it to PostgreSQL
            anomalyRepository.save(anomaly);
        }
    }
}