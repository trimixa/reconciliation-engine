# Project: Real-Time Reconciliation Engine

## Stack & Environment
- **Backend:** Java + Spring Boot (Producer & Consumer)
- **Infrastructure:** Apache Kafka (Message Broker), Redis (Matching Cache, 5-minute TTL), PostgreSQL (Permanent Vault), Docker Compose
- **IDE:** IntelliJ IDEA
- **Local AI Command Center (Continue.dev):**
  - Main coding model: Qwen 2.5 Coder 32B (local, Ollama via RTX 5070 Ti VRAM)
  - Reasoning/debug model: DeepSeek R1 14B (local, Ollama)
  - Autocomplete: Codestral 22B (local, Ollama)
  - Codebase embedding: Nomic Embed (local, Ollama)

## Current State

### ✅ Done
- Producer service (with 10% simulated failure rate)
- Kafka setup & configuration
- Docker Compose infrastructure
- Consumer service (Reconciliation Engine)
- Redis cross-stream matching logic & 5m TTL expiry handler
- Orphaned transaction vault (PostgreSQL via Spring Data JPA)
- Zero-cost local AI coding setup inside IntelliJ to bypass cloud limits
- Built REST API layer (`ReconciliationController` with `GET /api/anomalies`)
- Setup Swagger/OpenAPI documentation
- Fixed catastrophic code loss (restored `consumeCbsLog`)
- Replaced volatile memory buffer with robust Kafka DLQ (`anomaly-dlq`)
- Standardized entire repository to Java 21 LTS
- Resolved strict IDE static analyzer warnings for Null Type Safety across services
- Cleaned up obsolete test files (`AnomalyBufferTest.java`)
- Implemented Phase 3 Remediation API (resolving anomalies, `@Transactional` DB updates, Kafka events)

### 🔄 In Progress
- Implement Integration Tests (Testcontainers)

### ⏳ TODO
- High-volume load testing

## Last Session
- **Date:** [05-05-2026]
- **What I did:** Completely implemented Phase 3 (Remediation API). Added state tracking to the `Anomaly` entity (`"OPEN"` / `"RESOLVED"`). Created a `RemediationService` with a `@Transactional` method to safely update PostgreSQL and publish resolution events back to a new Kafka topic (`resolved-transactions`) atomically. Exposed this via `POST /api/anomalies/{id}/resolve`. 
- **Where I stopped:** The backend is fully capable of real-time matching and manual remediation. Next up: building automated Integration Tests using Testcontainers to spin up ephemeral Kafka and Postgres instances during testing.

## Key Decisions
- **Architecture:** Redis TTL = 5 minutes; Orphaned transactions → PostgreSQL vault. If vault is offline → Kafka DLQ (`anomaly-dlq`).
- **Workflow:** Use AI to learn and review (not just vibe code). DeepSeek R1 is reserved for Chat/Reasoning mode.
- **Tooling:** Sticking with IntelliJ IDEA by utilizing free, local LLMs to replace expensive cloud subscriptions, achieving a ₹0 setup cost.

## Recent Work

### Phase 2: Visibility
**Accomplishments:**
- Implemented Swagger/OpenAPI documentation and verified it at http://localhost:8081/swagger-ui/index.html.
- The `GET /api/anomalies` endpoint is now visible and interactable through the Swagger UI.

### Phase 3: Resiliency
**Progress:**
- Added Resilience4j for circuit breaking in `ReconciliationService`.
- Refactored the `saveAnomaly` method to ensure the circuit breaker trips immediately if the database is down.
- The engine now survives database outages by tripping the "databaseService" circuit breaker and logging fallbacks to the console.

**Tech Stack Updates:**
- Confirmed the use of Java 21 and Spring Boot 3.2.5.
- Added the correct dependencies for Spring Kafka (`org.springframework.kafka:spring-kafka`) and Springdoc OpenAPI.

### Lessons Learned:
- Maven dependency resolution quirks: The Kafka starter uses `org.springframework.kafka:spring-kafka` instead of the spring-boot-starter naming convention.