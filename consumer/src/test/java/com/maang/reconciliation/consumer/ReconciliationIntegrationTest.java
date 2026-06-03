package com.maang.reconciliation.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maang.reconciliation.consumer.domain.Anomaly;
import com.maang.reconciliation.consumer.domain.Transaction;
import com.maang.reconciliation.consumer.infrastructure.persistence.AnomalyRepository;
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

import java.math.BigDecimal;
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
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("bank_reconciliation")
            .withUsername("admin")
            .withPassword("password");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.0"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:alpine")
            .withExposedPorts(6379);

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
    void testAnomalyCreationForUnmatchedDatamartLog() throws Exception {
        // Arrange
        String txId = "TEST-TX-12345";
        Transaction datamartTx = new Transaction(txId, "ACC-999", new BigDecimal("500.00"), "DATAMART", System.currentTimeMillis());
        String payload = objectMapper.writeValueAsString(datamartTx);

        // Act - Send to datamart-logs without sending to cbs-logs first
        kafkaTemplate.send("datamart-logs", payload);

        // Assert - Verify that an anomaly is created within 10 seconds
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Optional<Anomaly> anomalyOpt = anomalyRepository.findById(txId);
            assertTrue(anomalyOpt.isPresent(), "Anomaly should be created in the database");
            
            Anomaly anomaly = anomalyOpt.get();
            assertEquals("Missing in Core Banking System", anomaly.getFailureReason());
            assertEquals("OPEN", anomaly.getStatus());
        });
    }
}
