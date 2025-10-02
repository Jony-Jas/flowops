# Execution Service

The **Execution Service** is part of FlowOps and is responsible for managing and executing workflow runs.  
It persists execution flows and runs in MongoDB, publishes lifecycle commands to Kafka, and streams real-time status updates to clients via Server-Sent Events (SSE).  

This design closely follows an **event-sourcing pattern**, where execution state is updated based on Kafka events from the execution engine.

## 🚀 Features
- CRUD operations for **Flows** (workflow definitions).
- Manage **Runs** (workflow execution instances).
- Event-sourcing style updates via Kafka:
  - Commands: start, pause, resume, stop.
  - Status: flow-level and step-level updates.
- **Flattened step execution tracking** for easy updates.
- **Server-Sent Events (SSE)** for progressive client updates.
- Pluggable **EventPublisher** abstraction (currently backed by Kafka).
- MongoDB persistence with auditing.
- Docker-based local environment (MongoDB, Kafka in KRaft mode, Execution Service).

## 📡 API Endpoints

### REST (Flows)
- **POST /api/executions/flows** -> create a new flow
- **GET /api/executions/flows** -> list all flows (filtered by ownerId)
- **GET /api/executions/flows/{id}** -> get flow details
- **PUT /api/executions/flows/{id}** -> update flow (including steps)
- **DELETE /api/executions/flows/{id}** -> delete a flow

### REST (Runs)
- **POST /api/executions/flows/{id}/start** -> start execution, returns runId
- **POST /api/executions/runs/{runId}/pause** -> pause execution
- **POST /api/executions/runs/{runId}/resume** -> resume execution
- **POST /api/executions/runs/{runId}/stop** -> stop execution
- **GET /api/executions/runs/{runId}/status** -> fetch latest run status

### SSE (Streaming)
- **GET /api/executions/stream/{runId}** -> subscribe to real-time run updates

## 🛠 Running Locally

### Steps
1. Configure `.env` with ports and DB names:
2. Start dependencies and service
```
docker-compose up --build
```
3. Service runs at: http://localhost:${EXECUTION_SERVICE_PORT}
4. Kafka UI available at: http://localhost:8085

## 📌 TODO
- [ ] Update REST controllers to return appropriate HTTP status codes for different operations  
- [ ] Improve exception handling to surface clear and user-friendly validation error messages  
- [ ] Implement scheduling for flows (currently only placeholder)  
- [ ] Ensure created timestamps (`createdAt`) are properly persisted during flow/run creation  
- [ ] Add indexes on `flowId` and `steps.stepId` fields for efficient MongoDB queries at scale  


## 📖 License
GPL-3.0 License. See **[LICENSE](LICENSE)** for details.