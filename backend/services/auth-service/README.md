## 🛡️ FlowOps Auth Service

The FlowOps Auth Service is a lightweight **authentication microservice** that uses **Keycloak** as the Identity Provider (IdP). It is responsible for handling **JWT verification**, token creation, and token refresh for both users and services within the FlowOps ecosystem.

---

### ✨ Core Features

| Feature | Description |
| :--- | :--- |
| 🔑 **JWT Verification** | Validates access tokens using Keycloak’s **JWKS endpoint**. |
| 🧍‍♂️ **User Authentication** | Issues tokens for Keycloak users via **password grant**. |
| 🤖 **Service Authentication** | Issues tokens for microservices via **client credentials grant**. |
| 🔁 **Token Refresh** | Supports **refresh token exchange** for long-lived sessions. |
| 🧾 **Unified Verification API** | `/auth/verify` acts as a centralized token introspection point for all FlowOps services. |

---

### 🔁 API Endpoints

The service exposes the following API endpoints:

1.  **Generate Token** (`/auth/token`)
    * **Method:** `POST`
    * *Purpose:* Request a token for a user or service.
2.  **Verify Token** (`/auth/verify`)
    * **Method:** `POST`
    * *Purpose:* Verify if a given JWT is valid and extract user/service information.
3.  **Refresh Token** (`/auth/refresh`)
    * **Method:** `POST`
    * *Purpose:* Exchange a refresh token for a new access token.
4.  **Health Check** (`/auth/health`)
    * **Method:** `GET`
    * *Response:* `AuthService is running 🚀`

---

### 🧪 Running Locally

#### 1. Environment Configuration

Create a **`.env`** file in the root directory with the following variables:

```bash
AUTH_SERVICE_PORT=8084
KEYCLOAK_IMAGE=quay.io/keycloak/keycloak:25.0.0
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin
KEYCLOAK_REALM=flowops
KEYCLOAK_CLIENT_ID=flowops-auth
KEYCLOAK_CLIENT_SECRET=super-secret-key
KEYCLOAK_BASE_URL=http://keycloak:8080
KEYCLOAK_REALM_URL=http://keycloak:8080/realms/${KEYCLOAK_REALM}
KEYCLOAK_TOKEN_URL=${KEYCLOAK_BASE_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token
KEYCLOAK_JWKS_URL=${KEYCLOAK_BASE_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/certs
```

#### 2. Start via Docker Compose
Run the following command:
```
docker compose up --build
```
Services Started:
🧩 AuthService: → http://localhost:8084
🔑 Keycloak: → http://localhost:9080 (Admin user: admin / Password: admin)

## 🧩 Future Enhancements

- [ ] Add **caching layer** for verified tokens (to reduce repeated decoding/Keycloak lookups).
- [ ] Add **role-based access checks (RBAC)** per endpoint.
- [ ] Support Keycloak **token introspection** for opaque tokens.

## 📖 License
GPL-3.0 License. See **[LICENSE](LICENSE)** for details.