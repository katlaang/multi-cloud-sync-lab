# Multi-Cloud Sync Lab

A small reference implementation for demonstrating real-time synchronization behavior across **simulated AWS, Azure and GCP provider endpoints**.

The application is designed for a conference demo. The provider stores are simulated locally so that the demo is deterministic. The synchronization behavior is implemented in Kotlin/Spring Boot code.

## What Stage 1 demonstrates

- canonical synchronization events
- version tracking
- same-version conflict detection
- deterministic conflict resolution
- target outage isolation
- queued events
- recovery and replay
- duplicate suppression / idempotency
- append-only in-memory audit trail

## Technology

- Java 21
- Kotlin 2.4.10
- Spring Boot 4.1.1
- Gradle Kotlin DSL
- HTML/JavaScript dashboard
- Docker / Docker Compose
- Terraform directory reserved for optional real-cloud deployment later

## Run from IntelliJ IDEA

1. Open this folder as a Gradle project.
2. Allow Gradle to download dependencies.
3. Run `MultiCloudSyncApplication.kt`.
4. Open:

   http://localhost:8080

## Run with Gradle installed

```bash
gradle bootRun
```

## Run with Docker

```bash
docker compose up --build
```

Then open:

http://localhost:8080

## Conference sequence

1. **Normal sync**
2. **Create conflict**
3. **Take GCP offline**
4. **Update during outage**
5. **Restore GCP**
6. **Re-send duplicate**

## Demo data

The sample entity is:

```text
FLIGHT-CYC2026
```

Initial state:

```text
Status: ON_TIME
Gate: C20
Version: 1
```

## Architecture evolution

### Stage 1
One Spring Boot application, three independent in-memory provider stores.

### Stage 2
Split AWS, Azure and GCP into separate Spring Boot services and introduce Kafka/Redpanda.

### Stage 3
Add optional Terraform modules that can deploy the provider services to actual AWS, Azure and GCP resources.

### Stage 4
Add observability, CI/CD, security profiles and repeatable performance experiments.

## Important scope statement

This repository is a reference prototype demonstrating synchronization mechanics. It is not a reproduction of published benchmark results and it does not claim that the local provider stores are actual AWS, Azure or GCP services.
