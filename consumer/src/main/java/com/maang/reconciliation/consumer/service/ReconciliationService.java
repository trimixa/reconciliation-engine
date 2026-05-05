package com.maang.reconciliation.consumer.service;

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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.maang.reconciliation.consumer.model.BufferedAnomaly;

import java.util.Queue;

@Service
public class ReconciliationService {

    private static final Logger logger = LoggerFactory.getLogger(ReconciliationService.class);
    private final StringRedisTemplate redisTemplate;
    private final AnomalyRepository anomalyRepository;

    // Self-injection to trigger Proxy (AOP) for the Circuit Breaker
    @Autowired
    @Lazy
    private ReconciliationService self;

    public ReconciliationService(StringRedisTemplate redisTemplate, AnomalyRepository anomalyRepository) {
        this.redisTemplate = redisTemplate;
        this.anomalyRepository = anomalyRepository;
    }

    // ... consumeCbsLog remains the same ...

    @KafkaListener(topics = "datamart-logs", groupId = "reconciliation-group")
    public void consumeDatamartLog(Transaction transaction) {
        Boolean wasFoundAndDeleted = redisTemplate.delete(transaction.transactionId());

        if (Boolean.TRUE.equals(wasFoundAndDeleted)) {
            logger.info("✅ [MATCHED] Transaction safely reconciled: {}", transaction.transactionId());
        } else {
            logger.warn("⚠️ [ORPHAN] DataMart log has no matching CBS record! Saving to Vault: {}", transaction.transactionId());

            Anomaly anomaly = new Anomaly(transaction.transactionId(), "Missing in Core Banking System", System.currentTimeMillis());

            // CRITICAL: Call via 'self' to trigger the Circuit Breaker proxy
            self.saveAnomalyWithCircuitBreaker(anomaly);
        }
    }

    // Must be PUBLIC for the Proxy to see it
    @CircuitBreaker(name = "databaseService", fallbackMethod = "saveAnomalyFallback")
    public void saveAnomalyWithCircuitBreaker(Anomaly anomaly) {
        // This call will now be intercepted by Resilience4j
        anomalyRepository.save(anomaly);
    }

    // Fallback must be PUBLIC or PROTECTED and match signature
//    public void saveAnomalyFallback(Anomaly anomaly, Throwable t) {
//        logger.error("🚨 CIRCUIT OPEN: Database unavailable. TXN {} cached in logs: {}",
//                anomaly.getTransactionId(), t.getMessage());
//    }
    @Autowired
    private AnomalyBufferService anomalyBufferService;

    // REPLACE THE OLD FALLBACK METHOD WITH THIS:
    public void saveAnomalyFallback(Anomaly anomaly, Throwable t) {
        logger.error("🚨 [CIRCUIT OPEN] Database unavailable. Buffering TXN {} {} in memory", anomaly.getTransactionId(), t.getMessage());

        // Convert Anomaly to BufferedAnomaly and store
        BufferedAnomaly bufferedAnomaly = new BufferedAnomaly(
                anomaly.getTransactionId(),
                anomaly.getFailureReason(),
                anomaly.getDetectedTimestamp()
        );

        anomalyBufferService.bufferAnomaly(bufferedAnomaly);
    }
}
