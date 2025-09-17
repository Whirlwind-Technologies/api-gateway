package com.nnipa.gateway.filter;

import com.nnipa.gateway.service.RateLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Rate limiting filter factory for per-route configuration
 */
@Slf4j
@Component
public class RateLimiterGatewayFilterFactory extends AbstractGatewayFilterFactory<RateLimiterGatewayFilterFactory.Config> {

    private final RateLimitService rateLimitService;

    public RateLimiterGatewayFilterFactory(RateLimitService rateLimitService) {
        super(Config.class);
        this.rateLimitService = rateLimitService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Extract tenant ID for tenant-specific rate limiting
            String tenantId = request.getHeaders().getFirst("X-Tenant-Id");
            String userId = request.getHeaders().getFirst("X-User-Id");
            String clientIp = getClientIp(request);

            // Build rate limit key
            String rateLimitKey = buildRateLimitKey(tenantId, userId, clientIp);

            // Check rate limit
            boolean allowed = rateLimitService.allowRequest(
                    rateLimitKey,
                    config.getRequestsPerSecond(),
                    config.getBurstCapacity()
            );

            if (!allowed) {
                log.warn("Rate limit exceeded for key: {}", rateLimitKey);
                return onRateLimitExceeded(exchange);
            }

            // Add rate limit headers to response
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().add("X-RateLimit-Limit", String.valueOf(config.getRequestsPerSecond()));
            response.getHeaders().add("X-RateLimit-Remaining",
                    String.valueOf(rateLimitService.getRemainingTokens(rateLimitKey)));
            response.getHeaders().add("X-RateLimit-Reset",
                    String.valueOf(rateLimitService.getResetTime(rateLimitKey)));

            return chain.filter(exchange);
        };
    }

    private String buildRateLimitKey(String tenantId, String userId, String clientIp) {
        if (tenantId != null && !tenantId.isEmpty()) {
            return "tenant:" + tenantId;
        } else if (userId != null && !userId.isEmpty()) {
            return "user:" + userId;
        } else {
            return "ip:" + clientIp;
        }
    }

    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddress() != null ?
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    private Mono<Void> onRateLimitExceeded(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", "application/json");

        String body = "{\"error\": \"Rate limit exceeded\", \"status\": 429, " +
                "\"message\": \"Too many requests. Please try again later.\"}";

        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    public static class Config {
        private int requestsPerSecond = 100;
        private int burstCapacity = 200;

        public int getRequestsPerSecond() {
            return requestsPerSecond;
        }

        public void setRequestsPerSecond(int requestsPerSecond) {
            this.requestsPerSecond = requestsPerSecond;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }
    }
}