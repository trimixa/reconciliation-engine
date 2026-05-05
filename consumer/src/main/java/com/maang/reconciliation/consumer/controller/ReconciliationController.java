package com.maang.reconciliation.consumer.controller;

import com.maang.reconciliation.consumer.model.Anomaly;
import com.maang.reconciliation.consumer.repository.AnomalyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReconciliationController {

    private final AnomalyRepository anomalyRepository;

    // Spring automatically injects the repository here
    public ReconciliationController(AnomalyRepository anomalyRepository) {
        this.anomalyRepository = anomalyRepository;
    }

    @GetMapping("/anomalies")
    public ResponseEntity<List<Anomaly>> getOrphanedTransactions() {
        return ResponseEntity.ok(anomalyRepository.findAll());
    }
}
