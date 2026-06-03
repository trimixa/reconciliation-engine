# 🚀 Real-Time Data Reconciliation Engine

> A distributed, event-driven microservices architecture built to simulate high-volume banking transactions and autonomously detect data loss in real-time across decoupled systems.

## 🏗️ Architecture Overview

In a typical financial data ecosystem, transactions flow from a Core Banking System (CBS) to downstream analytical DataMarts. Network blips, server crashes, or faulty ETL jobs can cause dropped records, leading to severe regulatory and financial discrepancies. 

This system solves that by ingesting live data streams and performing sub-millisecond reconciliation.

### System Components:

1. **The Producer (Data Generator)**
   * Built with Spring Boot & Java 21.
   * Simulates a high-throughput banking environment.
   * Publishes guaranteed events to the `cbs-logs` Kafka topic.
   * Introduces controlled chaos (a 10% simulated failure rate) when publishing to the `datamart-logs` Kafka topic to mimic real-world data drops.

2. **The Message Broker (Apache Kafka)**
   * Acts as the decoupled conveyor belt, providing fault-tolerant, scalable event streaming.

3. **The Consumer (Reconciliation Engine)**
   * Built with Spring Boot & Java 21.
   * Concurrently consumes both `cbs-logs` and `datamart-logs` streams.
   * Utilizes **Redis** as a high-speed, temporary matching cache. When a CBS log arrives, it is stored in Redis with a 5-minute TTL (Time-to-Live). 
   * When a DataMart log arrives, it queries Redis. A match results in a successful reconciliation (cache eviction).
   * If the TTL expires or a DataMart log arrives without a CBS counterpart, a race condition or data drop has occurred. The system instantly flags this and routes the orphaned transaction to the permanent vault.
   * Implements the **Outbox Pattern** to reliably publish resolution events back to Kafka even if the message broker is temporarily down.
   * Exposes REST APIs for visibility, anomaly resolution, and system metrics.

4. **The Vault (PostgreSQL)**
   * Automatically managed via **Spring Data JPA** (Hibernate).
   * Permanently persists `system_anomalies` for the operations team to audit and investigate.

5. **Frontend Dashboard**
   * A modern React application built with Vite and TypeScript.
   * Provides real-time visibility into the system anomalies and metrics.

6. **Infrastructure & DevOps**
   * Fully containerized using Docker and Docker Compose for local development.
   * Kubernetes deployment ready using Helm charts (`k8s/helm-chart`).
   * Monitored via Prometheus (`prometheus.yml`).
   * Automated CI/CD pipeline using GitHub Actions.

---

## 🛠️ Tech Stack

* **Backend Languages & Frameworks:** Java 21 LTS, Spring Boot (Web, Kafka, Data Redis, Data JPA)
* **Frontend:** React, TypeScript, Vite, CSS
* **Message Broker:** Apache Kafka & Zookeeper
* **In-Memory Cache:** Redis
* **Relational Database:** PostgreSQL
* **Infrastructure:** Docker, Docker Compose, Kubernetes, Helm
* **Monitoring:** Prometheus
* **Testing:** Testcontainers (Integration Testing)

---

## ⚙️ How to Run Locally

### Prerequisites
* Docker and Docker Compose installed and running
* Java 21 installed
* Node.js 20+ installed (for frontend)
* Maven installed (or use the provided Maven wrapper)

### Step 1: Spin up the Infrastructure
Open your terminal in the root directory (where `docker-compose.yml` is located) and start the infrastructure containers (Kafka, Zookeeper, Redis, PostgreSQL):
```bash
docker-compose up -d
```

### Step 2: Start the Producer
The Producer generates the transaction data. In a new terminal, navigate to the `producer` directory:
```bash
cd producer
./mvnw spring-boot:run
```

### Step 3: Start the Consumer (Reconciliation Engine)
The Consumer processes the data and finds anomalies. In another terminal, navigate to the `consumer` directory:
```bash
cd consumer
./mvnw spring-boot:run
```
The Consumer API will be available at `http://localhost:8081`. You can access the Swagger UI for the API at `http://localhost:8081/swagger-ui/index.html`.

### Step 4: Start the Frontend Dashboard
Navigate to the `frontend` directory:
```bash
cd frontend
npm install
npm run dev
```
The frontend dashboard will be accessible at the URL provided in the terminal (usually `http://localhost:5173`).

---

## ☁️ Kubernetes Deployment

The project includes a Helm chart for deploying the entire stack to a Kubernetes cluster.
```bash
cd k8s
helm install reconciliation-engine ./helm-chart
```

---

## 🔍 System Resiliency Features

* **Dead Letter Queue (DLQ):** If the PostgreSQL vault is offline, anomalies are safely routed to a Kafka DLQ (`anomaly-dlq`).
* **Circuit Breaking:** The Consumer uses Resilience4j to gracefully handle database outages.
* **Outbox Pattern:** Ensures atomic updates to the database and event publishing for resolved anomalies.
