# FlowOps SDK

The **FlowOps SDK** provides a simple framework for building custom tasks that can be dynamically loaded and executed by the FlowOps Execution Engine.  
It is designed to reduce boilerplate, enforce consistent patterns, and make task development straightforward.

---

## ✨ Features

- **Task Lifecycle API** – start, stop, pause, resume
- **Annotation-based inputs/outputs** – declare task parameters without boilerplate
- **Automatic logger injection** – every task comes with an `SLF4J` logger
- **Task metadata** – describe tasks with `@TaskType`
- **Reflection-based injector** – auto-map inputs and collect outputs

---

## 🚀 Getting Started

### 1. Install the SDK
Build and publish to your local Maven repo:

```bash
./gradlew :sdk:publishToMavenLocal
```
Add it as a dependency in your task module:
```gradle
dependencies {
    implementation 'com.flowops:sdk:0.1.0-SNAPSHOT'
}
```

### 2. Create a Task
```java
import com.flowops.sdk.annotations.*;
import com.flowops.sdk.core.BaseTask;

@TaskType(name = "HttpCallTask", description = "Makes an HTTP request and returns the response")
public class HttpCallTask extends BaseTask {

    @Input(name = "url", description = "Target URL")
    private String url;

    @Output(name = "response", description = "Response body")
    private String response;

    @Override
    public void start() {
        logger.info("Calling URL: {}", url);
        // perform HTTP call
        this.response = "OK";
    }
}
```

### 3. Test in Local by Injecting Inputs and Collecting Outputs
```java
import com.flowops.sdk.utils.AnnotationInjector;

import java.util.Map;

public class TaskRunner {
    public static void main(String[] args) {
        HttpCallTask task = new HttpCallTask();

        // Inject inputs
        AnnotationInjector.injectInputs(task, Map.of("url", "https://flowops.com"));

        // Run the task
        task.start();

        // Collect outputs
        Map<String, Object> outputs = AnnotationInjector.getOutputs(task);
        System.out.println(outputs); // {response=OK}
    }
}
```

## 📋 TODOs (Planned Improvements)
### Core API
    - [ ] Introduce TaskContext to carry logger, metadata, and input/output maps
    - [ ] Update Task interface to accept TaskContext

### Annotations
     - [ ] Make name() in @Input/@Output optional (fallback to field name)
     - [ ] Add lifecycle annotations (@OnStart, @OnStop, @OnPause, @OnResume)
     - [ ]  Add required flag in @Input

### Utilities
    - [ ] Improve AnnotationInjector error handling (replace System.out with proper exceptions/logging)
    - [ ] Support type conversion for inputs (e.g., "123" → int)
    - [ ] Add TaskMetadataReader to extract info from @TaskType
    - [ ] Add @InjectLogger support for non-BaseTask tasks

### Logging
    - [ ] Remove slf4j-simple from api
    - [ ] Keep only slf4j-api as api and use slf4j-simple only for tests

### Quality
    - [ ] Add validation for required inputs
    - [ ] Write unit tests for AnnotationInjector and sample tasks
    - [ ] Provide an example task module (HttpCallTask) for end-to-end demo

## 📖 License
GPL-3.0 License. See **[LICENSE](LICENSE)** for details.