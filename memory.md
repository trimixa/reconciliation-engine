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
- Completed Phase 2 Visibility (REST API) with Swagger UI and `/api/anomalies/stats` endpoint
- Implemented Phase 4 Quality Assurance (Testcontainers Integration Tests, GitHub Actions CI/CD)

### 🔄 In Progress
- Finalize system scaling and disaster recovery strategies (Phase 5)

### ⏳ TODO
- High-volume load testing

## Last Session
- **Date:** [05-06-2026]
- **What I did:** Completed Phase 4 (Quality Assurance). Wrote `ReconciliationIntegrationTest` utilizing Testcontainers for Kafka, Redis, and Postgres. Added GitHub Actions CI pipeline (`ci.yml`) to automatically test the `consumer` and `producer` applications on pull requests.
- **Where I stopped:** The CI pipeline and Integration Tests are in place. Next up: building horizontal scaling strategies and disaster recovery (Phase 5).

## Key Decisions
- **Architecture:** Redis TTL = 5 minutes; Orphaned transactions → PostgreSQL vault. If vault is offline → Kafka DLQ (`anomaly-dlq`).
- **Workflow:** Use AI to learn and review (not just vibe code). DeepSeek R1 is reserved for Chat/Reasoning mode.
- **Tooling:** Sticking with IntelliJ IDEA by utilizing free, local LLMs to replace expensive cloud subscriptions, achieving a ₹0 setup cost.

## Recent Work

### Phase 2: Visibility
**Accomplishments:**
- Implemented Swagger/OpenAPI documentation and verified it at http://localhost:8081/swagger-ui/index.html.
- The `GET /api/anomalies` endpoint is now visible and interactable through the Swagger UI.
- Implemented the `GET /api/anomalies/stats` endpoint to return system metrics (total anomalies, open vs resolved, resolution rate).

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