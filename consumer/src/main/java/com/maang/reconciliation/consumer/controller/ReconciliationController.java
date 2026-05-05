package com.maang.reconciliation.consumer.controller;

import com.maang.reconciliation.consumer.model.Anomaly;
import com.maang.reconciliation.consumer.repository.AnomalyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.maang.reconciliation.consumer.service.RemediationService;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReconciliationController {

    private final AnomalyRepository anomalyRepository;
    private final RemediationService remediationService;

    // Spring automatically injects the dependencies here
    public ReconciliationController(AnomalyRepository anomalyRepository, RemediationService remediationService) {
        this.anomalyRepository = anomalyRepository;
        this.remediationService = remediationService;
    }

    @GetMapping("/anomalies")
    public ResponseEntity<List<Anomaly>> getOrphanedTransactions() {
        return ResponseEntity.ok(anomalyRepository.findAll());
    }

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
