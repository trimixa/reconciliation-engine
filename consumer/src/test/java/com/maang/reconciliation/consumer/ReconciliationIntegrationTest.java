package com.maang.reconciliation.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maang.reconciliation.consumer.model.Anomaly;
import com.maang.reconciliation.consumer.model.Transaction;
import com.maang.reconciliation.consumer.repository.AnomalyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Optional;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@SuppressWarnings("resource")
class ReconciliationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.0"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private AnomalyRepository anomalyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testDatamartOrphanBecomesAnomaly() throws Exception {
        // Given a mock transaction ID that does NOT exist in CBS (Redis)
        String orphanTxId = "TXN-TEST-999";
        Transaction mockTx = new Transaction(orphanTxId, "ACC-123", new java.math.BigDecimal("100.50"), "DATAMART", System.currentTimeMillis());
        String payload = objectMapper.writeValueAsString(mockTx);

        // When we push it directly to datamart-logs
        kafkaTemplate.send("datamart-logs", payload);

        // Then the consumer should process it, fail to find it in Redis, 
        // and vault it as an Anomaly within a few seconds.
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Optional<Anomaly> anomalyOpt = anomalyRepository.findById(orphanTxId);
            assertTrue(anomalyOpt.isPresent(), "Anomaly should be saved in DB");
            
            Anomaly anomaly = anomalyOpt.get();
            assertEquals("OPEN", anomaly.getStatus(), "Status should default to OPEN");
        });
    }
}
