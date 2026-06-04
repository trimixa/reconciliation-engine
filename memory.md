# Project: Real-Time Reconciliation Engine

## Stack & Environment
- **Backend:** Java + Spring Boot (Producer & Consumer)
- **Frontend:** React 19 + TypeScript + Vite + Vanilla CSS
- **Infrastructure:** Apache Kafka (Message Broker), Redis (Matching Cache, 5-minute TTL), PostgreSQL (Permanent Vault), Docker Compose, Kubernetes (Helm Charts)
- **Monitoring:** Prometheus
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
- Built REST API layer (`ReconciliationController` with `GET /api/anomalies`, `stats`, `resolve`)
- Setup Swagger/OpenAPI documentation
- Fixed catastrophic code loss (restored `consumeCbsLog`)
- Replaced volatile memory buffer with robust Kafka DLQ (`anomaly-dlq`)
- Standardized entire repository to Java 21 LTS
- Resolved strict IDE static analyzer warnings for Null Type Safety across services
- Cleaned up obsolete test files (`AnomalyBufferTest.java`)
- Phase 2: Visibility (REST API & Swagger)
- Phase 3: Remediation API (resolving anomalies, `@Transactional` DB updates, bulk resolve-all, interactive React dashboard)
- Phase 4: Quality Assurance (Testcontainers Integration Tests, GitHub Actions CI/CD)
- Phase 5: Scalability & Outbox Pattern for reliable transactional messaging between database and Kafka. Kubernetes Helm charts.
- Phase 6: Security Enhancements (Spring Security, Basic Auth, RBAC Admin/User roles)
- Phase 7 & 8: Monitoring with Prometheus, and Advanced Pagination for the REST APIs.
- Phase 9: Comprehensive documentation (README.md) overhaul.

### 🔄 In Progress
- Finalize system disaster recovery strategies (Phase 5).
- Grafana dashboard visualization (Phase 7).

### ⏳ TODO
- High-volume load testing

## Last Session
- **Date:** [05-06-2026] (Current updates)
- **What I did:** Analyzed the latest codebase updates. Added the missing "Resolve All" button to the static HTML UI. Updated the `README.md` to comprehensively document the new React dashboard, Outbox Pattern, Kubernetes setup, and Spring Security integrations. Updated `roadmap.md` and `memory.md` to reflect all completed phases (5, 6, 7, 8, 9).
- **Where I stopped:** The project is functionally very complete. Security is in place, K8s charts are ready, and both UIs are fully functional.

## Key Decisions
- **Architecture:** Redis TTL = 5 minutes; Orphaned transactions → PostgreSQL vault. If vault is offline → Kafka DLQ (`anomaly-dlq`). Outbox Pattern ensures eventual consistency between the database and Kafka.
- **Workflow:** Use AI to learn and review (not just vibe code). DeepSeek R1 is reserved for Chat/Reasoning mode.
- **Tooling:** Sticking with IntelliJ IDEA by utilizing free, local LLMs to replace expensive cloud subscriptions, achieving a ₹0 setup cost.

## Recent Work

### Phase 3: Remediation
**Accomplishments:**
- Implemented bulk resolution via `POST /api/anomalies/resolve-all`.
- Integrated "Resolve All" button in both React and Static Vanilla JS dashboards.

### Phase 5 & 6: Scalability & Security
**Progress:**
- Deployed Helm charts in `k8s/helm-chart`.
- Implemented Outbox Pattern to guarantee message delivery to Kafka after DB transaction commits.
- Secured API endpoints with Spring Security and Role-Based Access Control (RBAC).

### Phase 7 & 8: Monitoring & Tuning
**Progress:**
- Added `prometheus.yml` for metrics scraping.
- Successfully implemented paginated results (`?page=X&size=Y`) for anomalies to handle large datasets cleanly.