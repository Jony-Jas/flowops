# FlowOps Plugin Service

The Plugin Service is a Spring Boot microservice responsible for managing plugin JAR files and metadata.
It exposes REST and gRPC APIs, persists metadata in MongoDB, and stores JAR files in MinIO (S3-compatible).

## ✨ Features

- Upload, list, fetch, and delete plugins and their versions
- Store plugin JARs in MinIO with metadata in MongoDB
- REST API for plugin management
- gRPC API for execution engine / other services
- DTO mapping with MapStruct
- File storage abstraction via FileService
- Docker-ready (configured via environment variables)

## 🔌 REST API Endpoints
| Method | Endpoint                           | Description                     |
| ------ | ---------------------------------- | ------------------------------- |
| GET    | `/api/plugins`                     | Get all plugins                 |
| POST   | `/api/plugins`                     | Upload plugin metadata + JAR    |
| GET    | `/api/plugins/{pluginId}`          | Get all versions of a plugin    |
| GET    | `/api/plugins/{pluginId}/{id}`     | Get specific plugin version     |
| GET    | `/api/plugins/{pluginId}/{id}/jar` | Download JAR file               |
| DELETE | `/api/plugins/{pluginId}`          | Delete all versions of a plugin |
| DELETE | `/api/plugins/{pluginId}/{id}`     | Delete specific plugin version  |

## 🎯 gRPC API
Service: `PluginService`
- `GetPluginMetadata(PluginRequest) returns (PluginMetadata)`
- `GetPluginJar(PluginRequest) returns (PluginJarResponse)`
Generated from grpc-contracts project.

## 🚀 Running Locally

```
./gradlew clean build
docker-compose up --build
```
- REST API → http://localhost:8001/api/plugins
- gRPC API → grpc://localhost:9091/

### ⚙️ Configuration
Configured via environment variables (Docker or .env):
```
# Plugin Service
PLUGIN_SERVICE_PORT=8001
PLUGIN_SERVICE_GRPC_PORT=9091

# Mongo
MONGO_URI=mongodb://mongodb:27017/plugin-db

# MinIO
MINIO_URL=http://minio:9000
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin
MINIO_BUCKET_NAME=plugin-bucket
```

## 📌 TODO
- [ ] Add ownerId (userId) in PluginMetaData for multi-user isolation & auth
- [ ] Improve gRPC GetPluginJar → support streaming for large JARs
- [ ] Add unit tests & integration tests (esp. for MinIO + gRPC)
- [ ] Add health-check endpoint (/api/health) for readiness/liveness probes
- [ ] Add security (JWT/OAuth2) → restrict plugin access by user
- [ ] Enhance error responses with error codes across REST & gRPC consistently
- [ ] Add CI/CD pipeline (GitHub Actions / GitLab CI) with Docker build + tests
- [ ] Optimize file download (currently loads whole JAR into memory → switch to streaming)
- [ ] Add API docs (Swagger/OpenAPI for REST, reflection for gRPC)

## 📖 License
GPL-3.0 License. See **[LICENSE](LICENSE)** for details.