package com.maang.reconciliation.consumer.job;

import com.maang.reconciliation.consumer.model.Anomaly;
import com.maang.reconciliation.consumer.model.BufferedAnomaly;
import com.maang.reconciliation.consumer.repository.AnomalyRepository;
import com.maang.reconciliation.consumer.service.AnomalyBufferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AnomalyDrainJob {

    private static final Logger logger = LoggerFactory.getLogger(AnomalyDrainJob.class);
    private final AnomalyBufferService bufferService;
    private final AnomalyRepository anomalyRepository;

    public AnomalyDrainJob(AnomalyBufferService bufferService, AnomalyRepository anomalyRepository) {
        this.bufferService = bufferService;
        this.anomalyRepository = anomalyRepository;
    }

    /**
     * Every 5 seconds, try to drain buffered anomalies to the database
     */
    @Scheduled(fixedRate = 5000)
    public void drainBuffer() {
        if (!bufferService.hasBufferedAnomalies()) {
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

                anomalyRepository.save(anomaly);
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
                        bufferedAnomaly.getRetryCount(),
                        e);

                // Break the loop—if one fails, probably all will
                break;
            }
        }

        if (savedCount > 0) {
            logger.info("✅ [DRAIN COMPLETE] Saved {} anomalies from buffer. {} failed.",
                    savedCount, failedCount);
        }
    }
}