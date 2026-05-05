package com.maang.reconciliation.consumer.model;

import java.io.Serializable;

public class BufferedAnomaly implements Serializable {
    private String transactionId;
    private String failureReason;
    private long detectedTimestamp;
    private int retryCount;
    private long bufferTime;

    // Constructor
    public BufferedAnomaly(String transactionId, String failureReason, long detectedTimestamp) {
        this.transactionId = transactionId;
        this.failureReason = failureReason;
        this.detectedTimestamp = detectedTimestamp;
        this.retryCount = 0;
        this.bufferTime = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public long getDetectedTimestamp() { return detectedTimestamp; }
    public void setDetectedTimestamp(long detectedTimestamp) { this.detectedTimestamp = detectedTimestamp; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public long getBufferTime() { return bufferTime; }
    public void setBufferTime(long bufferTime) { this.bufferTime = bufferTime; }

    public void incrementRetry() { this.retryCount++; }
}