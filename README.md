# <img width="50" height="50" alt="flowops-logo" src="https://github.com/Jony-Jas/flowops/blob/main/assets/flowops-logo-small.PNG" /> FlowOps Platform

FlowOps is a modular, event-driven workflow automation platform that enables building, orchestrating, and running pluggable tasks across distributed microservices.

It provides an extensible SDK, dynamic plugin management, execution orchestration, and full observability across services.

---

## 🧱 Architecture Overview

The FlowOps ecosystem is composed of multiple microservices communicating via **REST**, **gRPC**, and **Kafka** events.

All services are discoverable through **Eureka** and protected by a unified **Auth** layer.

---

## 📘 Diagram:
<img width="703" height="733" alt="architecture" src="https://github.com/Jony-Jas/flowops/blob/main/docs/architecture.png" />

---

## 🚀 Core Services

### 🛡️ Auth Service

Handles authentication and token management using **Keycloak**.

Provides unified JWT verification and refresh APIs.
- **Endpoints**: `/auth/token`, `/auth/verify`, `/auth/refresh`
- **Integration**: Used by API Gateway for validating tokens

📄 See detailed [README](backend/services/auth-service/README.md)

### 🧩 API Gateway

Acts as a single entry point for all client and internal requests.

Performs authentication, request routing, and header propagation (`X-User-Id`).
- Built with **Spring Cloud Gateway**
- Integrates with **Eureka** for discovery
- Propagates user identity to downstream services

📄 See detailed [README](backend/services/api-gateway/README.md)

### 🧭 Service Discovery

Central **Eureka Server** for dynamic service registration and discovery.

Monitors service health and status

📄 See [README](backend/services/service-discovery/README.md)

### ⚙️ Plugin Service

Manages plugin **JARs** and metadata, making them available to the execution engine via REST and gRPC.

- **Storage**: **MongoDB** + **MinIO**
- **gRPC** for plugin retrieval by the engine
- **REST** for upload, list, and delete operations

📄 See [README](backend/services/plugin-service/README.md)

### 🧠 Execution Service

Provides **REST** APIs for flow definitions and run management.

Implements an **event-sourcing** style architecture — commands and statuses flow through **Kafka**.
- Manages flow CRUD and run lifecycle
- Streams live status via **SSE**
- Publishes **Kafka** events for the Execution Engine

📄 See [README](backend/services/execution-service/README.md)

### ⚡ Execution Engine

The core orchestration runtime that executes workflow steps as a **DAG**.

Consumes and emits **Kafka** events, manages execution state in **Redis**, and executes plugins dynamically via **gRPC**.
- **Event-driven** architecture
- State persistence in **Redis**
- Dynamic plugin execution via SDK

📄 See [README](backend/services/execution-engine/README.md)

### 🧰 FlowOps SDK

Provides the base framework for writing reusable and testable tasks.

- Annotation-based `@Input`, `@Output`, `@TaskType` APIs
- Automatic logger injection
- Simple task lifecycle management

📄 See [README](sdk/README.md)

---

## 🔗 API Reference

All **REST** endpoints across microservices are documented in a **Postman Collection**.
📦 Import from:
`docs/REST API Documentation.postman_collection.json`
This includes:

- Auth Service APIs
- Plugin Management APIs
- Execution APIs (Flows, Runs, SSE)
- Gateway integration samples

---

## 🔄 Data & Event Flow

1.  Client authenticates via **Auth Service** (`/auth/token`).
2.  **API Gateway** validates JWT, adds `X-User-Id`, and routes request.
3.  **Plugin Service** manages plugin binaries and metadata.
4.  **Execution Service** stores workflow definitions and publishes execution commands to **Kafka**.
5.  **Execution Engine** consumes events, executes flows using **DAG**, and reports progress via **Kafka**.
6.  Clients listen to **SSE** stream for real-time updates.

---

## 🧩 Future Enhancements

-   Role-based access control (**RBAC**)
-   CI/CD automation
-   Distributed execution workers
-   Plugin isolation (run in sandbox JVMs)
-   OpenTelemetry tracing and Prometheus metrics
-   Web UI for flow design and monitoring

---

## 📖 License

Licensed under the **GPL-3.0 License**.

See [LICENSE](LICENSE) for full details.
