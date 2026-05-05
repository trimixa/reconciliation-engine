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
- Paused development to lock in project documentation and AI context memory before moving to the frontend tier.

### ⏳ TODO
- Build REST API layer (`ReconciliationController` with `GET /api/anomalies`)
- Setup Swagger/OpenAPI documentation
- Build React/Chart.js front-end dashboard
- High-volume testing

## Last Session
- **Date:** May 1, 2026
- **What I did:** Completed the core backend architecture (ingesting Kafka streams, sub-millisecond matching in Redis, persisting orphans to PostgreSQL). Solved agentic session limits by building a fully local AI setup inside IntelliJ using Ollama and Continue.dev, confirming the RTX 5070 Ti can handle 32B models fully in VRAM.
- **Where I stopped:** Backend engine is verified. Taking a break before starting the "Full-Stack" API phase.
- **Next step:** Resume session using the prompt: `@Codebase @Files memory.md — read both and tell me where I left off`. Then, begin developing the REST API layer.

## Key Decisions
- **Architecture:** Redis TTL = 60s; Orphaned transactions → PostgreSQL vault.
- **Workflow:** Use AI to learn and review (not just vibe code). DeepSeek R1 is reserved for Chat/Reasoning mode.
- **Tooling:** Sticking with IntelliJ IDEA by utilizing free, local LLMs to replace expensive cloud subscriptions, achieving a ₹0 setup cost.