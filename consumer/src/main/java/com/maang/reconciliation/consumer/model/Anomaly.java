package com.maang.reconciliation.consumer.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// @Entity tells Hibernate: "Please magically turn this class into a Postgres database table!"
@Entity
@Table(name = "system_anomalies")
public class Anomaly {

    // @Id tells Postgres that this is the Primary Key
    @Id
    private String transactionId;

    private String failureReason;
    private long detectedTimestamp;

    // RULE: Hibernate absolutely requires an empty constructor to work behind the scenes
    public Anomaly() {
    }

    // Our constructor for creating new anomalies
    public Anomaly(String transactionId, String failureReason, long detectedTimestamp) {
        this.transactionId = transactionId;
        this.failureReason = failureReason;
        this.detectedTimestamp = detectedTimestamp;
    }

    // --- Getters and Setters below ---
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public long getDetectedTimestamp() { return detectedTimestamp; }
    public void setDetectedTimestamp(long detectedTimestamp) { this.detectedTimestamp = detectedTimestamp; }
}