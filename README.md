# Optimus API Gateway

Spring Boot microservices for the Optimus platform. The gateway handles JWT authentication, request routing, Redis-backed token blacklisting, and dynamic token-bucket rate limiting.

## Services

- `api-gateway`: authentication, authorization, routing, and rate limiting
- `auth-service`: user authentication and JWT creation
- `catalog-service`: catalog APIs
- `product-service`: product APIs
- `inventory-service`: inventory APIs
- `order-service`: order APIs
- `config-service`: central Spring configuration
- `eureka_server`: service discovery
- Redis: token blacklist, rate-limit buckets, and policy updates

The PR validation service is not part of the current platform.

## Local Ports

| Service | Port |
| --- | ---: |
| Eureka | 8761 |
| Config service | 8888 |
| API gateway | 8080 |
| Auth service | 8081 |
| Catalog service | 8083 |
| Product service | 8084 |
| Inventory service | 8085 |
| Order service | 8086 |
| Redis | 6379 |

## Run With Docker Compose

The Compose file starts Redis, Eureka, the config service, the gateway, and the business services:

```bash
docker compose up --build
```

MySQL is expected to be available locally for the services that use it. The MySQL container definition in `docker-compose.yml` is intentionally disabled.

## Configuration

The gateway imports configuration from the config service. Rate-limit defaults are in `config-service/config-service/src/main/resources/config/api-gateway.yml`:

```yaml
gateway:
  rate-limit:
    free-per-minute: 100
    premium-per-minute: 1000
    free-capacity: 20
    premium-capacity: 100
```

`capacity` is the maximum burst size. `free-per-minute` and `premium-per-minute` are the token refill rates. JWT secrets and database passwords must be supplied through environment-specific configuration; do not commit real secrets.

## Rate-Limit Administration

The gateway exposes these admin endpoints:

```text
GET  /api/admin/rate-limit
POST /api/admin/rate-limit
```

Example request:

```json
{
  "freePerMinute": 100,
  "premiumPerMinute": 1000,
  "freeCapacity": 20,
  "premiumCapacity": 100
}
```

The gateway updates its local policy cache and publishes changes through Redis. Admin requests require the `ADMIN` role.

## Gateway Routes

- `/api/auth/**` -> auth service
- `/api/orders/**` -> order service
- `/api/payments/**` -> order service
- `/api/products/**` -> product service
- `/api/categories/**` -> catalog service

Requests matching the configured public paths bypass JWT validation. Other requests require a valid, non-blacklisted bearer token.

## Build A Service

Each service is an independent Maven project:

```bash
cd api-gateway/api-gateway
./mvnw test
```

On Windows, use `mvnw.cmd test`.
