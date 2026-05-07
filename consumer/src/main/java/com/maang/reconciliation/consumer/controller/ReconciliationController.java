package com.maang.reconciliation.consumer.controller;

import com.maang.reconciliation.consumer.model.Anomaly;
import com.maang.reconciliation.consumer.model.AnomalyStats;
import com.maang.reconciliation.consumer.repository.AnomalyRepository;
import com.maang.reconciliation.consumer.service.RemediationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Reconciliation API", description = "Endpoints for managing and resolving data anomalies")
public class ReconciliationController {

    private final AnomalyRepository anomalyRepository;
    private final RemediationService remediationService;

    // Spring automatically injects the dependencies here
    public ReconciliationController(AnomalyRepository anomalyRepository, RemediationService remediationService) {
        this.anomalyRepository = anomalyRepository;
        this.remediationService = remediationService;
    }

    @Operation(summary = "Get all anomalies", description = "Retrieves a list of all orphaned transactions/anomalies from the vault.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of anomalies")
    @GetMapping("/anomalies")
    public ResponseEntity<List<Anomaly>> getOrphanedTransactions() {
        return ResponseEntity.ok(anomalyRepository.findAll());
    }

    @Operation(summary = "Get anomaly statistics", description = "Retrieves metrics such as total anomalies, open anomalies, and resolution rate.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved statistics")
    @GetMapping("/anomalies/stats")
    public ResponseEntity<AnomalyStats> getStats() {
        long total = anomalyRepository.count();
        long resolved = anomalyRepository.countByStatus("RESOLVED");
        long open = total - resolved;
        double rate = total == 0 ? 0.0 : ((double) resolved / total) * 100.0;
        
        AnomalyStats stats = new AnomalyStats(total, resolved, open, rate);
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Resolve an anomaly", description = "Marks a specific anomaly as RESOLVED and publishes a correction event to Kafka.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully resolved anomaly"),
            @ApiResponse(responseCode = "400", description = "Invalid transaction ID or anomaly not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error during resolution")
    })
    @PostMapping("/anomalies/{id}/resolve")
    public ResponseEntity<String> resolveAnomaly(@PathVariable String id) {
        try {
            remediationService.resolveAnomaly(id);
            return ResponseEntity.ok("Successfully resolved anomaly for transaction: " + id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to resolve anomaly: " + e.getMessage());
        }
    }
}
