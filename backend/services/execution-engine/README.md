# ⚙️ Execution Engine Service

The **Execution Engine** is the core runtime service of **FlowOps**, responsible for orchestrating and executing workflow steps in a distributed, event-driven environment.  
It consumes execution commands from Kafka, builds a directed acyclic graph (DAG) of steps, manages concurrent execution, tracks runtime state in Redis, and emits live status updates back to Kafka.

---

## 🚀 Features

- **Event-Driven Orchestration**
  - Consumes `ExecutionCommandEvent` messages from Kafka (START / STOP / PAUSE / RESUME).
  - Publishes `FlowStatusEvent` and `StepStatusEvent` to Kafka for real-time progress updates.

- **DAG-Based Execution**
  - Treats each workflow as a Directed Acyclic Graph (DAG) of steps.
  - Supports dependency resolution between steps (`stepId.outputKey` references).
  - Executes independent steps in parallel.

- **Runtime State Management**
  - Persists flow and step state in **Redis** (indegree, dependents, context, outputs).
  - Supports **pause / resume / stop** lifecycle operations.
  - Automatically recovers and resumes incomplete flows after restarts.

- **Dynamic Plugin Execution**
  - Fetches plugin JARs dynamically via **gRPC** from the `TaskService`.
  - Executes plugins using the **FlowOps SDK** (`BaseTask`, `@Input`, `@Output`).
  - Injects step inputs and extracts outputs via reflection.

- **Kafka Integration**
  - `<String, String>` JSON-based messaging for easy inspection and interoperability.
  - Built-in producer (`StatusEventProducer`) and consumer (`ExecutionCommandConsumer`).

- **Resilient Execution Engine**
  - Thread-safe concurrency control.
  - Graceful shutdown and restart recovery.
  - Fault isolation and atomic progress tracking.

---

## 🧩 Architecture Overview
```
┌──────────────────────────────┐
│ ExecutionService │
│ (sends ExecutionCommandEvent) │
└──────────────┬───────────────┘
│ Kafka (execution.commands)
▼
┌─────────────────────────────────────────────┐
│ Execution Engine Service │
│─────────────────────────────────────────────│
│ 1️⃣ Kafka Consumer → ExecutionScheduler │
│ 2️⃣ DAGBuilder + Validator (build flow DAG) │
│ 3️⃣ RedisPersistence (track run state) │
│ 4️⃣ StepExecutor (fetch + run plugin JARs) │
│ 5️⃣ Kafka Producer (emit step/flow status) │
└─────────────────────────────────────────────┘
│
│ Kafka (execution.status)
▼
┌──────────────────────────────┐
│ Monitoring / UI / API │
└──────────────────────────────┘
```

**Core Components**

| Component | Responsibility |
|------------|----------------|
| `ExecutionCommandConsumer` | Listens for start/stop/pause/resume commands from Kafka |
| `ExecutionScheduler` | Coordinates step execution, concurrency, and lifecycle |
| `RedisExecutionRepository` | Persists run state (flow meta, step statuses, outputs, context) |
| `DAGBuilder` & `DAGValidator` | Build and validate workflow DAG topology |
| `StepExecutor` | Dynamically fetches and runs plugin JARs via gRPC |
| `StatusEventProducer` | Emits live status events for monitoring |
| `PluginServiceClient` | gRPC client for fetching plugins from TaskService |

---

## 🔄 API & Event Flow

### 🔹 Command Events (Input)
**Topic:** `execution.commands`  
Produced by: `ExecutionService`

| EventType | Description |
|------------|-------------|
| `START` | Triggers a new flow run |
| `STOP` | Immediately stops a running flow |
| `PAUSE` | Suspends new step scheduling, keeps running steps |
| `RESUME` | Resumes execution of a paused flow |


### 🔹 Status Events (Output)
**Topic:** `execution.status`

`Step Status Event`
```
{
  "eventType": "STEP_STATUS",
  "flowId": "flow1",
  "runId": "run1",
  "stepId": "s1",
  "status": "COMPLETED",
  "error": null,
  "timestamp": "2025-10-03T12:00:05Z"
}
```
`Flow Status Event`
```
{
  "eventType": "FLOW_STATUS",
  "flowId": "flow1",
  "runId": "run1",
  "status": "COMPLETED",
  "error": null,
  "timestamp": "2025-10-03T12:00:06Z"
}
```

## 🛠 Running Locally

### Steps
1. Configure `.env` with ports and DB names:
2. Start dependencies and service
```
docker-compose up --build
```

## 🧠 Future Enhancements

- [ ] **Out-of-Process Plugin Execution:** Run plugins in isolated JVMs or containers for stronger fault and security isolation.
- [ ] **Distributed Execution Workers:** Horizontal scaling of the execution engine nodes across Kafka partitions.
- [ ] **Step Retry & Backoff Policies:** Configurable retries, timeouts, and error-handling strategies.
- [ ] **Observability & Metrics**
    - [ ] Prometheus metrics (running steps, latency, errors).
    - [ ] OpenTelemetry tracing integration.

## 📖 License
GPL-3.0 License. See **[LICENSE](LICENSE)** for details.