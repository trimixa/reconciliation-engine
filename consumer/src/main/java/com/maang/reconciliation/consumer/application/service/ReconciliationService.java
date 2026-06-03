package com.maang.reconciliation.consumer.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maang.reconciliation.consumer.domain.Anomaly;
import com.maang.reconciliation.consumer.domain.Transaction;
import com.maang.reconciliation.consumer.infrastructure.persistence.AnomalyRepository;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;

@Service
public class ReconciliationService {

    private static final Logger logger = LoggerFactory.getLogger(ReconciliationService.class);
    private final StringRedisTemplate redisTemplate;
    private final AnomalyRepository anomalyRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private final Executor taskExecutor;

    @Autowired
    @Lazy
    private ReconciliationService self;

    public ReconciliationService(StringRedisTemplate redisTemplate, 
                                 AnomalyRepository anomalyRepository,
                                 KafkaTemplate<String, String> kafkaTemplate,
                                 ObjectMapper objectMapper,
                                 @Qualifier("applicationTaskExecutor") Executor taskExecutor) {
        this.redisTemplate = redisTemplate;
        this.anomalyRepository = anomalyRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
    }

    @KafkaListener(topics = "cbs-logs", groupId = "reconciliation-group")
    public void consumeCbsLog(Transaction transaction) {
        String txId = transaction.transactionId();
        if (txId == null) {
            logger.warn("⚠️ [CBS] Received transaction with null ID, skipping.");
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            logger.info("⬇️ [CBS] Received valid transaction: {}", txId);
            redisTemplate.opsForValue().set(txId, "CBS", 5, TimeUnit.MINUTES);
        }, taskExecutor).exceptionally(ex -> {
            logger.error("🔥 [CBS ERROR] Failed to process transaction: {}", txId, ex);
            return null;
        });
    }

    @KafkaListener(topics = "datamart-logs", groupId = "reconciliation-group")
    public void consumeDatamartLog(Transaction transaction) {
        String txId = transaction.transactionId();
        if (txId == null) {
            logger.warn("⚠️ [DataMart] Received transaction with null ID, skipping.");
            return;
        }

        CompletableFuture.supplyAsync(() -> redisTemplate.delete(txId), taskExecutor)
            .thenAcceptAsync(wasFoundAndDeleted -> {
                if (Boolean.TRUE.equals(wasFoundAndDeleted)) {
                    logger.info("✅ [MATCHED] Transaction safely reconciled: {}", txId);
                } else {
                    logger.warn("⚠️ [ORPHAN] DataMart log has no matching CBS record! Saving to Vault: {}", txId);
                    Anomaly anomaly = new Anomaly(txId, "Missing in Core Banking System", System.currentTimeMillis());
                    self.saveAnomalyWithCircuitBreaker(anomaly);
                }
            }, taskExecutor)
            .exceptionally(ex -> {
                logger.error("🔥 [DATAMART ERROR] Failed to process transaction: {}", txId, ex);
                return null;
            });
    }

    @CircuitBreaker(name = "databaseService", fallbackMethod = "saveAnomalyFallback")
    public void saveAnomalyWithCircuitBreaker(Anomaly anomaly) {
        if (anomaly == null) return;
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
