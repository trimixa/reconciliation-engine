// File: consumer/src/main/java/com/maang/reconciliation/consumer/service/AnomalyBufferService.java

package com.maang.reconciliation.consumer.service;

import com.maang.reconciliation.consumer.model.BufferedAnomaly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class AnomalyBufferService {

    private static final Logger logger = LoggerFactory.getLogger(AnomalyBufferService.class);
    private final Queue<BufferedAnomaly> anomalyBuffer = new ConcurrentLinkedQueue<>();

    /**
     * Add an anomaly to the buffer when database is unavailable
     */
    public void bufferAnomaly(BufferedAnomaly anomaly) {
        anomalyBuffer.add(anomaly);
        logger.warn("📦 [BUFFERED] TXN {} added to memory buffer. Queue size: {}",
                anomaly.getTransactionId(), anomalyBuffer.size());
    }

    /**
     * Get the next anomaly from buffer (FIFO order)
     */
    public BufferedAnomaly getNextAnomaly() {
        return anomalyBuffer.poll();
    }

    /**
     * Check if buffer has items waiting
     */
    public boolean hasBufferedAnomalies() {
        return !anomalyBuffer.isEmpty();
    }

    /**
     * Get current buffer size
     */
    public int getBufferSize() {
        return anomalyBuffer.size();
    }

    /**
     * Clear all buffered anomalies (emergency only)
     */
    public void clearBuffer() {
        logger.error("🗑️ [EMERGENCY] Clearing entire anomaly buffer!");
        anomalyBuffer.clear();
    }
}