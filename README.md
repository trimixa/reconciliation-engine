🚀 Real-Time Data Reconciliation Engine

> A distributed, event-driven microservices architecture built to simulate high-volume banking transactions and autonomously detect data loss in real-time across decoupled systems.

## 🏗️ Architecture Overview

In a typical financial data ecosystem, transactions flow from a Core Banking System (CBS) to downstream analytical DataMarts. Network blips, server crashes, or faulty ETL jobs can cause dropped records, leading to severe regulatory and financial discrepancies. 

This system solves that by ingesting live data streams and performing sub-millisecond reconciliation.

### Microservices Breakdown:

1. **The Producer (Data Generator)**
   * Simulates a high-throughput banking environment.
   * Publishes guaranteed events to the `cbs-logs` Kafka topic.
   * Introduces controlled chaos (a 10% simulated failure rate) when publishing to the `datamart-logs` Kafka topic to mimic real-world data drops.

2. **The Message Broker (Apache Kafka)**
   * Acts as the decoupled conveyor belt, providing fault-tolerant, scalable event streaming.

3. **The Consumer (Reconciliation Engine)**
   * Concurrently consumes both `cbs-logs` and `datamart-logs` streams.
   * Utilizes **Redis** as a high-speed, temporary matching cache. When a CBS log arrives, it is stored in Redis with a 60-second TTL (Time-to-Live). 
   * When a DataMart log arrives, it queries Redis. A match results in a successful reconciliation (cache eviction).
   * If the TTL expires or a DataMart log arrives without a CBS counterpart, a race condition or data drop has occurred. The system instantly flags this and routes the orphaned transaction to the permanent vault.

4. **The Vault (PostgreSQL)**
   * Automatically managed via **Spring Data JPA** (Hibernate).
   * Permanently persists `system_anomalies` for the operations team to audit and investigate.

---

## 🛠️ Tech Stack

* **Language:** Java 21
* **Framework:** Spring Boot (Web, Kafka, Data Redis, Data JPA)
* **Message Broker:** Apache Kafka & Zookeeper
* **In-Memory Cache:** Redis
* **Relational Database:** PostgreSQL
* **Containerization:** Docker & Docker Compose

---

## ⚙️ How to Run Locally

### Prerequisites
* Docker Desktop installed and running
* Java 21+ installed
* Maven installed (or use your IDE's built-in Maven)

### Step 1: Spin up the Engine Room (Infrastructure)
Open your terminal in the root directory (where `docker-compose.yml` is located) and start the containers:
```bash
docker-compose up -d
