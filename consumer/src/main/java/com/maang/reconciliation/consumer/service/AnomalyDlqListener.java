package com.maang.reconciliation.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maang.reconciliation.consumer.model.Anomaly;
import com.maang.reconciliation.consumer.repository.AnomalyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class AnomalyDlqListener {

    private static final Logger logger = LoggerFactory.getLogger(AnomalyDlqListener.class);
    private final AnomalyRepository anomalyRepository;
    private final ObjectMapper objectMapper;

    public AnomalyDlqListener(AnomalyRepository anomalyRepository, ObjectMapper objectMapper) {
        this.anomalyRepository = anomalyRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "anomaly-dlq", groupId = "dlq-reconciliation-group")
    public void consumeDlq(String anomalyJson) throws Exception {
        logger.info("📥 [DLQ CONSUMER] Received message from DLQ: {}", anomalyJson);
        
        Anomaly anomaly = objectMapper.readValue(anomalyJson, Anomaly.class);
        
        // Attempt to save to vault
        // If the database is still down, this will throw a DataAccessException
        // and Spring Kafka will automatically retry with backoff.
        anomalyRepository.save(anomaly);
        
        logger.info("✅ [DLQ RECOVERED] Successfully saved previously failed TXN {} to vault", anomaly.getTransactionId());
    }
}
