# Optimus API Gateway Platform

This repository defines the full project structure for a Spring Boot / Spring Cloud Gateway API platform with distributed rate limiting, JWT authentication, dynamic configuration, observability, and a React admin UI.

## 1. Required Services

Minimum service set:

- `gateway-service` — main API gateway with JWT auth, rate limiting, routing, metrics, and resilience.
- `auth-service` — login, registration, JWT generation, refresh tokens, and user management.
- `config-server` — Spring Cloud Config server for dynamic gateway and policy configuration.
- `admin-ui` — React-based admin dashboard for policy management and auditing.
- `redis` — distributed rate limiting and token blacklist storage.
- `sample-backend-service` — dummy backend for gateway routing tests.

## 2. Tech Stack

| Layer | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Gateway | Spring Cloud Gateway |
| Security | Spring Security + JWT |
| Cache | Redis |
| Config | Spring Cloud Config |
| Frontend | React |
| DB | MySQL |
| Build Tool | Maven |
| Container | Docker |
| Monitoring | Micrometer |
| Load Testing | k6 |

> Note: This platform uses MySQL for relational storage instead of PostgreSQL.

## 3. Project Architecture

Client -> API Gateway -> Backend Services

- Gateway performs JWT validation, rate limiting, logging, metrics, retry, and circuit breaking.
- Gateway talks to Redis for distributed token bucket state and JWT blacklist.
- Gateway pulls dynamic config from `config-server`.
- Admin UI manages policies via `config-server`.
- Auth service stores users, roles, and refresh tokens in MySQL.

```
Client
   |
   v
API Gateway
   |
   |---- JWT Validation
   |---- Rate Limiting
   |---- Logging
   |---- Metrics
   |
   v
Backend Services

Gateway <-----> Redis
Gateway <-----> Config Server
Admin UI -----> Config Server
Auth Service --> MySQL
```

## 4. Service Responsibilities

### A. `gateway-service`
- Route external requests to backend services.
- Validate JWT tokens and user roles.
- Apply token bucket rate limiting per user and per API.
- Log requests, latency, user info, and response status.
- Export metrics via Micrometer.
- Implement retry and circuit breaker patterns.
- Support dynamic configuration refresh from Config Server.

Dependencies should include:
- `spring-cloud-starter-gateway`
- `spring-boot-starter-security`
- `spring-boot-starter-data-redis-reactive`
- `jjwt` or `jjwt-api`
- `spring-boot-starter-actuator`
- `micrometer-registry-prometheus`
- `resilience4j-spring-boot3`

### B. `auth-service`
- User login and registration.
- JWT token creation and refresh token issuance.
- Store users, roles, and refresh tokens in MySQL.
- Expose auth endpoints for gateway clients.

### C. `config-server`
- Host dynamic gateway route and rate limit policies.
- Provide configuration refresh with `/actuator/refresh`.
- Store rate limits, route definitions, feature flags, and policy toggles.

Example config:
```yaml
rate-limit:
  free-user:
    requests: 100
  premium-user:
    requests: 1000
```

### D. `admin-ui`
- React dashboard for creating and editing policies.
- Enable/disable APIs and manage roles.
- Show rejected requests and usage statistics.
- Push configuration updates to Config Server.

### E. `sample-backend-service`
- Dummy service exposing:
  - `/orders`
  - `/payments`
  - `/products`
- Used to validate gateway routing and rate limiting.

## 5. Databases Required

- `Redis` for distributed rate limiting, token buckets, and JWT blacklist.
- `MySQL` for users, API policies, audit logs, and refresh tokens.

## 6. Redis Data Structure

Keys:

- `rate_limit:{userId}:{apiPath}`
- `jwt:blacklist:{token}`

Stored value example:
```json
{
  "tokens": 50,
  "lastRefill": 171000000
}
```

## 7. MySQL Tables

### `users`
```sql
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(50) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### `api_policies`
```sql
CREATE TABLE api_policies (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  api_path VARCHAR(255) NOT NULL,
  http_method VARCHAR(10) NOT NULL,
  role VARCHAR(50) NOT NULL,
  requests_per_minute INT NOT NULL,
  burst_capacity INT NOT NULL,
  enabled BOOLEAN DEFAULT TRUE,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### `audit_logs`
```sql
CREATE TABLE audit_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  api_path VARCHAR(255),
  status_code INT,
  latency_ms BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### `refresh_tokens`
```sql
CREATE TABLE refresh_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  token VARCHAR(500) NOT NULL,
  expiry TIMESTAMP NOT NULL
);
```

## 8. Rate Limiting Flow

1. Request arrives at gateway.
2. Gateway validates JWT.
3. Extract `userId` and `role`.
4. Build Redis key for the requested API.
5. Check token bucket state.
6. Allow or reject request.
7. Forward request to backend on success.

## 9. Token Bucket Algorithm

Store in Redis:
- `capacity`
- `tokens`
- `refill_rate`
- `last_refill_time`

Example:
```
capacity = 100
tokens = 80
refillRate = 10/sec
```

## 10. Important Gateway Filters

### JWT Filter
- validate token signature
- verify expiry
- extract user and role
- check blacklist

### Rate Limit Filter
- compute request key
- decrement bucket tokens
- reject when exceeded

### Logging Filter
- record request metadata
- measure latency
- log user and status code

## 11. Observability

Metrics to expose:
- `gateway_requests_total`
- `gateway_rejected_total`
- `redis_latency`
- `jwt_validation_failures`

Tools:
- Micrometer

## 12. Security Features

- JWT signature validation
- token expiry validation
- role-based rate limits
- token blacklisting in Redis

Example role limits:
- `FREE` -> 100 req/min
- `PREMIUM` -> 1000 req/min
- `ADMIN` -> unlimited

## 13. Failure Handling

### Redis Down
- `fail open` for normal APIs
- `fail closed` for critical APIs

### Invalid JWT
Response:
```json
{ "error": "INVALID_TOKEN" }
```

### Too Many Requests
Response:
```json
{ "error": "RATE_LIMIT_EXCEEDED" }
```
HTTP status: `429`

## 14. Configuration Management

- Use Spring Cloud Config to manage rate limits, gateway routes, and feature flags.
- Refresh dynamically with `/actuator/refresh`.

## 15. Docker Setup

Required containers:
- `gateway-service`
- `auth-service`
- `config-server`
- `redis`
- `mysql`

## 16. Load Testing

Use `k6` to simulate:
- 1000 requests/sec
- burst traffic
- multiple users
- JWT-based traffic

## 17. Build Order

1. `gateway-service`
2. `auth-service`
3. Redis integration
4. JWT validation
5. Token bucket logic
6. MySQL schemas
7. Dynamic configs
8. Admin UI
9. Metrics
10. Docker

## 18. Folder Structure

```
api-platform/
  gateway-service/
  auth-service/
  config-server/
  sample-backend-service/
  admin-ui/
  docker/
  scripts/
```

## 19. Next Step

This repository now contains the blueprint and skeleton structure for an API gateway project with MySQL, Redis, Spring Cloud Gateway, Spring Boot, React, and observability tooling.
