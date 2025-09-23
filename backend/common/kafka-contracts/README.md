# kafka_contracts

Centralized **Kafka topic definitions** and **event schemas** for FlowOps services.  
This module ensures all services (ExecutionService, Scheduler, UI Gateway, etc.) share the same contracts.


## 🔄 Usage
Add dependency in your service `build.gradle`:

```gradle
implementation project(":kafka_contracts")
```

Then in your code
```java
kafkaTemplate.send(Topics.EXECUTION_COMMANDS,
    runId,
    ExecutionCommandEvent.builder()
        .eventType(ExecutionCommandEvent.EventType.EXECUTION_START)
        .flowId(flowId)
        .runId(runId)
        .triggeredBy(userId)
        .timestamp(Instant.now())
        .build()
);
```

## 🚀 Roadmap / TODOs
- [ ] Add codegen step to generate Topics.java and POJOs from topics.yml
- [ ] Add Avro/JSON Schema support for stricter serialization
- [ ] Add schema validation utilities for runtime event checking
