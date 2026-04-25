package com.maang.reconciliation.producer.service;

import com.maang.reconciliation.producer.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class TransactionGenerator {

    private static final Logger logger = LoggerFactory.getLogger(TransactionGenerator.class);
    private final KafkaTemplate<String, Transaction> kafkaTemplate;

    public TransactionGenerator(KafkaTemplate<String, Transaction> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedRate = 2000)
    public void generateTransactions() {

        // 1. Create the base transaction data
        String sharedId = "TXN-" + UUID.randomUUID().toString().substring(0, 8);
        BigDecimal randomAmount = BigDecimal.valueOf(Math.random() * 1000).setScale(2, RoundingMode.HALF_UP);
        long currentTimestamp = System.currentTimeMillis();

        // 2. ALWAYS send to the Core Banking System (The Source of Truth)
        Transaction cbsLog = new Transaction(sharedId, "ACC-998877", randomAmount, "CBS", currentTimestamp);
        kafkaTemplate.send("cbs-logs", cbsLog.transactionId(), cbsLog);
        logger.info("-> Pushed to CBS:      {}", cbsLog.transactionId());

        // 3. Simulate the DataMart ETL (with a 10% failure rate)
        if (Math.random() > 0.10) {
            // 90% of the time, the downstream system successfully records it
            Transaction datamartLog = new Transaction(sharedId, "ACC-998877", randomAmount, "DATAMART", currentTimestamp);
            kafkaTemplate.send("datamart-logs", datamartLog.transactionId(), datamartLog);
            logger.info("-> Pushed to DATAMART: {}", datamartLog.transactionId());
        } else {
            // 10% of the time, simulate a massive real-world data drop!
            logger.error("-> [SIMULATED ERROR] Transaction lost before reaching DataMart: {}", sharedId);
        }

        logger.info("---------------------------------------------------");
    }
}