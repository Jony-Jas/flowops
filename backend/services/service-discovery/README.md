## 🧭 FlowOps Service Discovery

### Overview

The **Service Discovery** module provides a central **Eureka registry** for all FlowOps microservices.

It enables dynamic service **registration and lookup** for core services, including the API Gateway, Auth Service, Plugin Service, and Execution Service.

### ⚙️ Features

* **Technology:** Built using **Spring Cloud Netflix Eureka Server**.
* **Centralized Registry:** Acts as the single source of truth for all FlowOps backend services.
* **Service Health:** Supports **auto-registration** and **heartbeats** to monitor service status.
* **Deployment:** Features a **Docker-ready configuration**.