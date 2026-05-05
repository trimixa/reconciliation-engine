# Project: Real-Time Reconciliation Engine

## Stack & Environment
- **Backend:** Java + Spring Boot (Producer & Consumer)
- **Infrastructure:** Apache Kafka (Message Broker), Redis (Matching Cache, 60s TTL), PostgreSQL (Permanent Vault), Docker Compose
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
- Redis cross-stream matching logic & TTL expiry handler
- Orphaned transaction vault (PostgreSQL via Spring Data JPA)
- Zero-cost local AI coding setup inside IntelliJ to bypass cloud limits

### 🔄 In Progress
- Refactored `saveAnomaly` method in `ReconciliationService.java` to ensure the circuit breaker trips immediately if the database is down.

### ⏳ TODO
- Build REST API layer (`ReconciliationController` with `GET /api/anomalies`)
- Setup Swagger/OpenAPI documentation
- Build React/Chart.js front-end dashboard
- High-volume testing

## Last Session
- **Date:** [05-05-2026]
- **What I did:** Refactored the `saveAnomaly` method in `ReconciliationService.java` to ensure that the circuit breaker trips immediately if the database is down. Verified that the refactoring works as expected by simulating a database outage.
- **Where I stopped:** Backend engine is verified with the new refactoring. Ready to start developing the REST API layer next. 
- Implemented the `ReconciliationController` with the `GET /api/anomalies` endpoint.

## Key Decisions
- **Architecture:** Redis TTL = 60s; Orphaned transactions → PostgreSQL vault.
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