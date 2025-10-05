## 🧩 FlowOps API Gateway

### Overview

The **API Gateway** is the single entry point for all client requests in FlowOps.

It routes traffic to backend microservices (e.g., auth-service, plugin-service, execution-service), handles **authentication**, and propagates user identity via custom headers.

### ⚙️ Features

* **Dynamic Routing:** Achieved via **Spring Cloud Gateway**.
* **Service Discovery:** Uses **Eureka** for service registration and discovery.
* **JWT Validation:** Handled in coordination with the **auth-service**.
* **Identity Propagation:** Adds the **`X-User-Id`** header to all downstream calls.
* **CORS:** Enabled for all origins.