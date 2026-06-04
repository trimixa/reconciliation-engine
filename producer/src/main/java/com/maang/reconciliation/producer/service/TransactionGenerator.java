package com.maang.reconciliation.producer.service;

import com.maang.reconciliation.producer.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.NonNull;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class TransactionGenerator {

    private static final Logger logger = LoggerFactory.getLogger(TransactionGenerator.class);
    private final KafkaTemplate<String, Transaction> kafkaTemplate;
    @Value("${kafka.topic.cbs.logs}")
    private String cbsLogsTopic;
    @Value("${kafka.topic.datamart.logs}")
    private String datamartLogsTopic;
    @Value("${account.id.default}")
    private String defaultAccountId;

    public TransactionGenerator(KafkaTemplate<String, Transaction> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedRate = 2000)
    @SuppressWarnings("null")
    public void generateTransactions() {

        // 1. Create the base transaction data
        String sharedId = "TXN-" + UUID.randomUUID().toString().substring(0, 8);
        BigDecimal randomAmount = BigDecimal.valueOf(Math.random() * 1000).setScale(2, RoundingMode.HALF_UP);
        long currentTimestamp = System.currentTimeMillis();

        // 2. ALWAYS send to the Core Banking System (The Source of Truth)
        try {
            Transaction cbsLog = new Transaction(sharedId, defaultAccountId, randomAmount, "CBS", currentTimestamp);
            sendMessageToKafka(cbsLogsTopic, cbsLog.transactionId(), cbsLog);
            logger.info("-> Pushed to CBS:      {}", cbsLog.transactionId());
        } catch (Exception e) {
            logger.error("Failed to send transaction to CBS for ID: {}", sharedId, e);
        }

        // 3. Simulate the DataMart ETL (with a 10% failure rate)
        if (Math.random() > 0.10) {
            // 90% of the time, the downstream system successfully records it
            try {
                Transaction datamartLog = new Transaction(sharedId, defaultAccountId, randomAmount, "DATAMART", currentTimestamp);
                sendMessageToKafka(datamartLogsTopic, datamartLog.transactionId(), datamartLog);
                logger.info("-> Pushed to DATAMART: {}", datamartLog.transactionId());
            } catch (Exception e) {
                logger.error("Failed to send transaction to DataMart for ID: {}", sharedId, e);
            }
        } else {
            // 10% of the time, simulate a massive real-world data drop!
            logger.error("-> [SIMULATED ERROR] Transaction lost before reaching DataMart: {}", sharedId);
        }

        logger.info("---------------------------------------------------");
    }

    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000)
    )
    private void sendMessageToKafka(@NonNull String topic, @NonNull String key, @NonNull Transaction transaction) {
        kafkaTemplate.send(topic, key, transaction);
    }
}