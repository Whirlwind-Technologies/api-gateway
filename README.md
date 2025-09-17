# NNIPA API Gateway Service

## Overview

The API Gateway Service is the single entry point for all client requests to the NNIPA platform. It provides request routing, load balancing, rate limiting, authentication, correlation ID generation, and API documentation aggregation.

## Key Features

### 1. **Request Routing & Load Balancing**
- Routes requests to appropriate microservices
- Supports path-based and header-based routing
- Built-in load balancing with Spring Cloud Gateway

### 2. **Rate Limiting & Throttling**
- Token bucket algorithm using Bucket4j
- Per-tenant, per-user, and per-IP rate limiting
- Configurable limits per service route
- Rate limit headers in responses

### 3. **API Versioning & Documentation**
- Supports API versioning through path prefixes
- Aggregates OpenAPI documentation from all services
- Swagger UI integration for API exploration

### 4. **Request/Response Transformation**
- Adds correlation IDs to all requests
- Strips sensitive headers
- Adds security headers to responses
- Request/response logging

### 5. **Service Integration Pattern**

#### Correlation ID Flow:
1. **API Gateway (Generator)**
    - Generates `correlation_id` if not present
    - Validates existing `correlation_id`
    - Adds to all downstream requests
    - Logs request entry

2. **Auth Service (Consumer)**
    - Receives `correlation_id` from API Gateway
    - Uses for all authentication operations
    - Publishes to Kafka with `correlation_id` as key
    - Does NOT generate new `correlation_id`

3. **All Other Services (Propagators)**
    - Consume `correlation_id` from headers
    - Include in all logging and downstream calls
    - Never generate new `correlation_id` for the same request flow

## Architecture

```
Internet
    |
    v
[API Gateway :4000]
    |
    +---> [Auth Service :4002]
    |
    +---> [Tenant Service :4001]
    |
    +---> [Authorization Service :4003]
    |
    +---> [User Management Service :4004]
    |
    +---> [Notification Service :4402]
    |
    +---> [Storage Service :4102]
```

## Prerequisites

- Java 21
- Maven 3.8+
- Docker (optional)

## Configuration

### Environment Variables

| Variable | Description | Default                 |
|----------|-------------|-------------------------|
| `AUTH_SERVICE_URL` | Auth service URL | `http://localhost:4002` |
| `TENANT_SERVICE_URL` | Tenant service URL | `http://localhost:4001` |
| `AUTHZ_SERVICE_URL` | Authorization service URL | `http://localhost:4003` |
| `USER_SERVICE_URL` | User management service URL | `http://localhost:4004` |
| `NOTIFICATION_SERVICE_URL` | Notification service URL | `http://localhost:4402` |
| `STORAGE_SERVICE_URL` | Storage service URL | `http://localhost:4102` |
| `JWT_SECRET` | JWT signing secret | (see application.yml)   |
| `JWT_ISSUER` | JWT issuer | `https://nnipa.cloud`   |

### Rate Limiting Configuration

```yaml
rate-limiting:
  enabled: true
  default:
    requests-per-second: 100
    burst-capacity: 200
  per-tenant:
    enabled: true
    cache-size: 10000
```

### Public Paths (No Authentication Required)

- `/api/v1/auth/register`
- `/api/v1/auth/login`
- `/api/v1/auth/refresh`
- `/api/v1/auth/forgot-password`
- `/api/v1/auth/reset-password`
- `/actuator/health`
- `/actuator/prometheus`
- `/swagger-ui/**`
- `/v3/api-docs/**`

## Running the Service

### Local Development

```bash
# Build the project
mvn clean package

# Run with default profile
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring.profiles.active=dev
```

### Docker

```bash
# Build Docker image
docker build -t nnipa/api-gateway:latest .

# Run with Docker
docker run -p 4000:4000 \
  -e AUTH_SERVICE_URL=http://auth-service:4002 \
  -e TENANT_SERVICE_URL=http://tenant-service:4001 \
  nnipa/api-gateway:latest

# Run with Docker Compose
docker-compose up -d
```

## API Endpoints

### Gateway Management

| Method | Path | Description |
|--------|------|-------------|
| GET | `/gateway/info` | Gateway information |
| GET | `/gateway/routes` | List all configured routes |

### Health & Monitoring

| Method | Path | Description |
|--------|------|-------------|
| GET | `/actuator/health` | Health check |
| GET | `/actuator/prometheus` | Prometheus metrics |
| GET | `/actuator/metrics` | Application metrics |

### API Documentation

| Method | Path | Description |
|--------|------|-------------|
| GET | `/swagger-ui.html` | Swagger UI |
| GET | `/v3/api-docs` | OpenAPI documentation |

## Request Headers

### Required Headers (for secured endpoints)

| Header | Description | Example |
|--------|-------------|---------|
| `Authorization` | Bearer token | `Bearer eyJhbGc...` |

### Optional Headers

| Header | Description | Example |
|--------|-------------|---------|
| `X-Correlation-Id` | Request correlation ID | `550e8400-e29b-41d4-a716-446655440000` |
| `X-Tenant-Id` | Tenant identifier | `123e4567-e89b-12d3-a456-426614174000` |

### Response Headers

| Header | Description |
|--------|-------------|
| `X-Correlation-Id` | Request correlation ID |
| `X-RateLimit-Limit` | Rate limit maximum |
| `X-RateLimit-Remaining` | Remaining requests |
| `X-RateLimit-Reset` | Reset timestamp |
| `X-Gateway-Response-Time` | Processing time |

## Circuit Breaker Configuration

The gateway implements circuit breakers for all downstream services:

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowSize: 10
        permittedNumberOfCallsInHalfOpenState: 3
        minimumNumberOfCalls: 5
        waitDurationInOpenState: 10000
        failureRateThreshold: 50
```

## Monitoring

### Metrics

The gateway exposes the following custom metrics:

- `gateway.requests.total` - Total number of requests
- `gateway.errors.total` - Total number of errors
- `gateway.ratelimit.exceeded` - Rate limit exceeded events
- `gateway.request.duration` - Request duration histogram

### Health Indicators

- Gateway health
- Downstream service health
- Circuit breaker status

## Troubleshooting

### Common Issues

1. **Service Unavailable (503)**
    - Check if downstream services are running
    - Verify service URLs in configuration
    - Check circuit breaker status

2. **Rate Limit Exceeded (429)**
    - Check rate limit configuration
    - Verify tenant/user limits
    - Monitor rate limit metrics

3. **Unauthorized (401)**
    - Verify JWT token is valid
    - Check token expiration
    - Ensure JWT secret matches auth service

### Logging

Enable debug logging for troubleshooting:

```yaml
logging:
  level:
    com.nnipa.gateway: DEBUG
    org.springframework.cloud.gateway: DEBUG
    io.github.resilience4j: DEBUG
```

## Development

### Project Structure

```
api-gateway/
├── src/main/java/com/nnipa/gateway/
│   ├── config/          # Configuration classes
│   ├── controller/      # REST controllers
│   ├── exception/       # Exception handlers
│   ├── filter/          # Gateway filters
│   ├── health/          # Health indicators
│   ├── metrics/         # Metrics collectors
│   ├── model/           # Data models
│   ├── service/         # Business services
│   └── util/            # Utility classes
├── src/main/resources/
│   ├── application.yml  # Main configuration
│   └── application-*.yml # Profile configurations
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

### Adding New Routes

To add a new service route, update `application.yml`:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: new-service
          uri: ${NEW_SERVICE_URL:http://localhost:PORT}
          predicates:
            - Path=/api/v1/new-service/**
          filters:
            - name: CircuitBreaker
              args:
                name: new-service
                fallbackUri: forward:/fallback/new-service
            - name: RateLimiter
              args:
                requestsPerSecond: 50
                burstCapacity: 100
            - AuthenticationFilter
            - StripPrefix=2
```

## Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Test with curl
curl -X GET http://localhost:4000/gateway/info

# Test with authentication
curl -X GET http://localhost:4000/api/v1/users/profile \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Security Considerations

1. **JWT Validation**: All protected routes validate JWT tokens
2. **Rate Limiting**: Prevents abuse and DDoS attacks
3. **CORS Configuration**: Configurable per environment
4. **Security Headers**: Adds security headers to all responses
5. **Sensitive Header Stripping**: Removes sensitive headers before forwarding

## License

Copyright © 2025 NNIPA. All rights reserved.