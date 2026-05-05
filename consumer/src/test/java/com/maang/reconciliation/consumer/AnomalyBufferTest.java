package com.maang.reconciliation.consumer;

import com.maang.reconciliation.consumer.model.Anomaly;
import com.maang.reconciliation.consumer.model.BufferedAnomaly;
import com.maang.reconciliation.consumer.repository.AnomalyRepository;
import com.maang.reconciliation.consumer.service.AnomalyBufferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootTest
class AnomalyBufferTest {

    private static final Logger logger = LoggerFactory.getLogger(AnomalyBufferTest.class);

    @Autowired
    private AnomalyBufferService bufferService;

    @Scheduled(fixedRate = 5000)
    public void drainBuffer() {
        logger.debug("🔄 [DRAIN JOB] Scheduled task started.");

        if (!bufferService.hasBufferedAnomalies()) {
            logger.debug("🔄 [DRAIN JOB] No buffered anomalies to process.");
            return;  // Nothing to do
        }

        logger.info("🔄 [DRAIN JOB] Attempting to drain {} buffered anomalies...",
                bufferService.getBufferSize());

        int savedCount = 0;
        int failedCount = 0;

        // Try to save each buffered anomaly
        while (bufferService.hasBufferedAnomalies()) {
            BufferedAnomaly bufferedAnomaly = bufferService.getNextAnomaly();

            try {
                // Convert back to Anomaly and save
                Anomaly anomaly = new Anomaly(
                        bufferedAnomaly.getTransactionId(),
                        bufferedAnomaly.getFailureReason(),
                        bufferedAnomaly.getDetectedTimestamp()
                );

                // Assuming there's an anomalyRepository bean available for saving anomalies
                AnomalyRepository.save(anomaly);
                savedCount++;

                logger.info("✅ [DRAINED] TXN {} successfully saved to vault",
                        bufferedAnomaly.getTransactionId());

            } catch (Exception e) {
                // Database still down, put it back in buffer
                failedCount++;
                bufferedAnomaly.incrementRetry();
                bufferService.bufferAnomaly(bufferedAnomaly);

                logger.error("⚠️ [DRAIN FAILED] Could not save TXN {}, retrying next cycle (attempt {})",
                        bufferedAnomaly.getTransactionId(),
                        bufferedAnomaly.getRetryCount(), e);

                // Break the loop—if one fails, probably all will
                break;
            }
        }

        if (savedCount > 0) {
            logger.info("✅ [DRAIN COMPLETE] Saved {} anomalies from buffer. {} failed.",
                    savedCount, failedCount);
        } else {
            logger.debug("🔄 [DRAIN JOB] No anomalies were saved.");
        }
    }

    @Test
    public void testBufferAndDrain() {
        BufferedAnomaly anomaly = new BufferedAnomaly("TXN-123", "Test", System.currentTimeMillis());

        // Add to buffer
        bufferService.bufferAnomaly(anomaly);
        assert bufferService.getBufferSize() == 1;

        // Retrieve from buffer
        BufferedAnomaly retrieved = bufferService.getNextAnomaly();
        assert retrieved.getTransactionId().equals("TXN-123");
        assert bufferService.getBufferSize() == 0;
    }
}
