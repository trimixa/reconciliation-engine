package com.maang.reconciliation.consumer.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Statistics about system anomalies")
public record AnomalyStats(
        @Schema(description = "Total number of anomalies recorded")
        long totalAnomalies,
        
        @Schema(description = "Number of anomalies that have been resolved")
        long resolvedAnomalies,
        
        @Schema(description = "Number of anomalies that are still open")
        long openAnomalies,
        
        @Schema(description = "Percentage of anomalies that have been resolved (0.0 to 100.0)")
        double resolutionRate
) {}
