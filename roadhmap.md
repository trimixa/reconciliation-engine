# AI Context Document: Real-Time Reconciliation Engine

## 1. Project Overview
This project is an enterprise-grade, event-driven reconciliation system designed to validate data integrity between core banking systems and downstream analytics platforms. It detects anomalous or "orphaned" transactions in real-time, replacing slow, manual nightly batch processes with a sub-millisecond automated control loop.

## 2. Technical Stack
* **Languages:** Java 21+
* **Frameworks:** Spring Boot (Data JPA, Redis, Web), Hibernate
* **Event Streaming:** Apache Kafka
* **Caching & State Management:** Redis (In-Memory Data Structure Store)
* **Persistent Storage:** PostgreSQL (Relational Vault)
* **Infrastructure:** Docker, Docker Compose
* **Target Frontend:** React, Chart.js

## 3. System Architecture & Data Flow
1. **Producer Service:** Simulates a banking environment. It emits valid transactions and intentionally introduces a 10% failure rate to simulate network drops, pushing events to a Kafka topic.
2. **Kafka Broker:** Decouples the data ingestion, handling high-throughput event streams.
3. **Consumer Service (The Engine):** Listens to the Kafka topics.
4. **Redis Matching (The Brain):** Temporarily stores transaction IDs with a **60-second Time-To-Live (TTL)**. It performs cross-stream matching to pair incoming records.
5. **PostgreSQL Vault (The Record):** If a transaction is not matched within the 60s TTL, it is classified as an "orphan/anomaly" and permanently persisted to the `system_anomalies` table using Spring Data JPA.

## 4. Current Project State: Phase 1 Complete
The core backend processing loop is fully operational.
- Kafka infrastructure is up and running via Docker Compose.
- Producer is successfully generating data streams.
- Consumer is successfully performing sub-millisecond matching in Redis.
- Unmatched records are actively being written to the PostgreSQL database.

## 5. Next Steps Roadmap (Full-Stack Evolution)

### Phase 2: Visibility (REST API)
* **Goal:** Expose the data stored in the PostgreSQL vault.
* **Tasks:**
    - Create a `ReconciliationController` in the Spring Boot Consumer app.
    - Implement `GET /api/anomalies` to fetch records from the `AnomalyRepository`.
    - Implement `GET /api/anomalies/stats` to return metrics (total orphans, resolution rate).
    - Integrate `springdoc-openapi` for Swagger API documentation.
    - Ensure comprehensive endpoint documentation and error handling.

### Phase 3: Visualization (Frontend Dashboard)
* **Goal:** Provide operational intelligence to stakeholders.
* **Tasks:**
    - Scaffold a React application.
    - Build a dashboard using Chart.js to visualize real-time transaction throughput and current anomaly counts.
    - Display a data grid of the latest orphaned transactions fetched from the REST API.
    - Implement user-friendly filters, search functionalities, and drill-down capabilities.

### Phase 4: Remediation (Business Logic)
* **Goal:** Allow users to fix bad data.
* **Tasks:**
    - Implement a `POST /api/anomalies/{id}/resolve` endpoint.
    - Update the database state upon resolution.
    - Trigger correction events back into Kafka for system consistency.
    - Ensure proper transaction management and error handling.

### Phase 5: Quality Assurance
* **Goal:** Prove system reliability.
* **Tasks:**
    - Implement automated testing using **Testcontainers** to spin up ephemeral Kafka and PostgreSQL instances.
    - Write integration tests verifying the anomaly detection logic.
    - Establish a CI/CD pipeline for automated builds, tests, and deployments.

### Phase 6: Scalability and Fault Tolerance
* **Goal:** Ensure high availability and fault tolerance in production.
* **Tasks:**
    - Design horizontal scaling strategies using load balancers and auto-scaling groups.
    - Implement circuit breakers (e.g., Hystrix) to handle unexpected loads gracefully.
    - Plan for disaster recovery and backup strategies.

### Phase 7: Security Enhancements
* **Goal:** Secure the application from potential threats.
* **Tasks:**
    - Integrate Spring Security for authentication and authorization.
    - Implement role-based access control (RBAC).
    - Encrypt sensitive data at rest and in transit.

### Phase 8: Monitoring and Logging
* **Goal:** Implement robust monitoring and logging.
* **Tasks:**
    - Integrate monitoring tools like Prometheus and Grafana.
    - Set up log aggregation using tools like ELK Stack for efficient troubleshooting.

### Phase 9: User Interface Enhancements
* **Goal:** Improve the dashboard's usability and aesthetics.
* **Tasks:**
    - Implement responsive design considerations.
    - Add interactive elements and real-time updates using WebSocket or Server-Sent Events (SSE).
    - Ensure accessibility compliance with standards like WCAG.

### Phase 10: Documentation and Maintenance
* **Goal:** Maintain comprehensive documentation for long-term sustainability.
* **Tasks:**
    - Document the system architecture, API endpoints, and operational procedures.
    - Establish a process for regular updates and maintenance of the documentation.
    - Foster knowledge sharing within the team to ensure continuity.

## 6. AI Assistant Instructions
When assisting with this codebase, act as a Senior Software Development Engineer. Prioritize code quality, scalability, and modern architectural patterns. Explain the *why* behind architectural decisions to ensure deep understanding of distributed systems.