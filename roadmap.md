# AI Context Document: Real-Time Reconciliation Engine

## 1. Project Overview
This project is an enterprise-grade, event-driven reconciliation system designed to validate data integrity between core banking systems and downstream analytics platforms. It detects anomalous or "orphaned" transactions in real-time, replacing slow, manual nightly batch processes with a sub-millisecond automated control loop.

## 2. Technical Stack
* **Languages:** Java 21 LTS (Standardized)
* **Frameworks:** Spring Boot (Data JPA, Redis, Web), Hibernate
* **Event Streaming:** Apache Kafka
* **Caching & State Management:** Redis (In-Memory Data Structure Store)
* **Persistent Storage:** PostgreSQL (Relational Vault)
* **Infrastructure:** Docker, Docker Compose
* **API Documentation:** Swagger UI / OpenAPI

## 3. System Architecture & Data Flow
1. **Producer Service:** Simulates a banking environment. It emits valid transactions and intentionally introduces a 10% failure rate to simulate network drops, pushing events to a Kafka topic.
2. **Kafka Broker:** Decouples the data ingestion, handling high-throughput event streams.
3. **Consumer Service (The Engine):** Listens to the Kafka topics.
4. **Redis Matching (The Brain):** Temporarily stores transaction IDs with a **5-minute Time-To-Live (TTL)**. It performs cross-stream matching to pair incoming records.
5. **PostgreSQL Vault (The Record):** If a transaction is not matched within the 5m TTL, it is classified as an "orphan/anomaly" and permanently persisted to the `system_anomalies` table using Spring Data JPA. If the database goes down, anomalies are safely routed to a **Kafka Dead Letter Queue (DLQ)**.

## 4. Current Project State: Phase 1 Complete
The core backend processing loop is fully operational.
- Kafka infrastructure is up and running via Docker Compose.
- Producer is successfully generating data streams.
- Consumer is successfully performing sub-millisecond matching in Redis.
- Unmatched records are actively being written to the PostgreSQL database.

## 5. Next Steps Roadmap (Full-Stack Evolution)

### Phase 2: Visibility (REST API) ✅ COMPLETE
* **Goal:** Expose the data stored in the PostgreSQL vault.
* **Tasks:**
    - ✅ Create a `ReconciliationController` in the Spring Boot Consumer app.
    - ✅ Implement `GET /api/anomalies` to fetch records from the `AnomalyRepository`.
    - ✅ Implement `GET /api/anomalies/stats` to return metrics (total orphans, resolution rate).
    - ✅ Integrate `springdoc-openapi` for Swagger API documentation.
    - ✅ Ensure comprehensive endpoint documentation and error handling.

### Phase 3: Remediation (Business Logic) ✅ COMPLETE
* **Goal:** Allow users or automated scripts to fix bad data.
* **Tasks:**
    - ✅ Implement a `POST /api/anomalies/{id}/resolve` endpoint.
    - ✅ Implement a bulk `POST /api/anomalies/resolve-all` endpoint.
    - ✅ Built interactive dashboards (React & Static HTML) with "Resolve All" functionalities.
    - ✅ Update the PostgreSQL database state upon resolution.
    - ✅ Trigger correction events back into Kafka for system consistency (via Outbox Pattern).
    - ✅ Ensure proper transaction management (`@Transactional`) and error handling.

### Phase 4: Quality Assurance ✅ COMPLETE
* **Goal:** Prove system reliability.
* **Tasks:**
    - ✅ Implement automated testing using **Testcontainers** to spin up ephemeral Kafka and PostgreSQL instances.
    - ✅ Write integration tests verifying the anomaly detection logic.
    - ✅ Establish a CI/CD pipeline for automated builds, tests, and deployments.

### Phase 5: Scalability and Fault Tolerance (⏳ IN PROGRESS)
* **Goal:** Ensure high availability and fault tolerance in production.
* **Tasks:**
    - ✅ Implement Kubernetes deployment via Helm charts (`k8s/helm-chart`).
    - Implement circuit breakers (Resilience4j) to handle unexpected loads gracefully (✅ DLQ Fallback implemented).
    - ✅ Implement Outbox Pattern for reliable transactional messaging between database and Kafka.
    - Plan for disaster recovery and backup strategies.

### Phase 6: Security Enhancements (✅ COMPLETE)
* **Goal:** Secure the application from potential threats.
* **Tasks:**
    - ✅ Integrate Spring Security for authentication and authorization (Basic Auth implemented).
    - ✅ Implement role-based access control (RBAC - ADMIN/USER roles configured).
    - Encrypt sensitive data at rest and in transit (Left for future scope/infra layer).

### Phase 7: Monitoring and Logging (⏳ IN PROGRESS)
* **Goal:** Implement robust monitoring and logging.
* **Tasks:**
    - ✅ Integrate monitoring tools like Prometheus (`prometheus.yml`).
    - Integrate Grafana for metric visualization.
    - Set up log aggregation using tools like ELK Stack for efficient troubleshooting.

### Phase 8: API & System Tuning (⏳ IN PROGRESS)
* **Goal:** Improve API performance and system throughput.
* **Tasks:**
    - ✅ Implement advanced pagination for the REST APIs.
    - Tune Kafka consumer concurrency settings.
    - Optimize PostgreSQL indexes for fast querying of anomalies.

### Phase 9: Documentation and Maintenance (✅ COMPLETE)
* **Goal:** Maintain comprehensive documentation for long-term sustainability.
* **Tasks:**
    - ✅ Document the system architecture, API endpoints, and operational procedures (Done via README.md update).
    - ✅ Establish a process for regular updates and maintenance of the documentation.
    - ✅ Foster knowledge sharing within the team to ensure continuity.

## 6. AI Assistant Instructions
When assisting with this codebase, act as a Senior Software Development Engineer. Prioritize code quality, scalability, and modern architectural patterns. Explain the *why* behind architectural decisions to ensure deep understanding of distributed systems.