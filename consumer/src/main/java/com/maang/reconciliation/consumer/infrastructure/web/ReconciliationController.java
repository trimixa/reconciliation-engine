package com.maang.reconciliation.consumer.infrastructure.web;

import com.maang.reconciliation.consumer.domain.Anomaly;
import com.maang.reconciliation.consumer.domain.AnomalyStats;
import com.maang.reconciliation.consumer.infrastructure.persistence.AnomalyRepository;
import com.maang.reconciliation.consumer.application.service.RemediationService;
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
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api")
@Tag(name = "Reconciliation API", description = "Endpoints for managing and resolving data anomalies")
public class ReconciliationController {

    private final AnomalyRepository anomalyRepository;
    private final RemediationService remediationService;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // Spring automatically injects the dependencies here
    public ReconciliationController(AnomalyRepository anomalyRepository, RemediationService remediationService) {
        this.anomalyRepository = anomalyRepository;
        this.remediationService = remediationService;
    }

    @Operation(summary = "Get all anomalies", description = "Retrieves a paginated list of all orphaned transactions/anomalies from the vault.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of anomalies")
    @GetMapping("/anomalies")
    public ResponseEntity<org.springframework.data.domain.Page<Anomaly>> getOrphanedTransactions(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(anomalyRepository.findAll(org.springframework.data.domain.PageRequest.of(page, size)));
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

    @GetMapping(path = "/anomalies/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAnomalies() {
        SseEmitter emitter = new SseEmitter(120000L); // 2 minutes timeout
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));
        return emitter;
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 2000)
    public void broadcastStats() {
        if (emitters.isEmpty()) return;
        try {
            AnomalyStats stats = getStats().getBody();
            if (stats != null) {
                for (SseEmitter emitter : emitters) {
                    try {
                        emitter.send(SseEmitter.event().name("stats").data(stats));
                    } catch (Exception e) {
                        emitters.remove(emitter);
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    @Operation(summary = "Resolve an anomaly", description = "Marks a specific anomaly as RESOLVED and publishes a correction event to Kafka.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully resolved anomaly"),
            @ApiResponse(responseCode = "400", description = "Invalid transaction ID or anomaly not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error during resolution")
    })
    @PostMapping("/anomalies/{id}/resolve")
    public ResponseEntity<String> resolveAnomaly(
            @PathVariable String id,
            @org.springframework.web.bind.annotation.RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        try {
            remediationService.resolveAnomaly(id, idempotencyKey);
            return ResponseEntity.ok("Successfully resolved anomaly for transaction: " + id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to resolve anomaly: " + e.getMessage());
        }
    }
    @Operation(summary = "Resolve all anomalies", description = "Marks all open anomalies as RESOLVED and publishes correction events.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully resolved anomalies"),
            @ApiResponse(responseCode = "500", description = "Internal server error during resolution")
    })
    @PostMapping("/anomalies/resolve-all")
    public ResponseEntity<String> resolveAllAnomalies(
            @org.springframework.web.bind.annotation.RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        try {
            remediationService.resolveAllAnomalies(idempotencyKey);
            return ResponseEntity.ok("Successfully resolved all open anomalies");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to resolve anomalies: " + e.getMessage());
        }
    }
}
