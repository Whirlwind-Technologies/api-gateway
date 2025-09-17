package com.nnipa.gateway.filter;

import com.nnipa.gateway.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Authentication filter for protected routes
 * Validates JWT tokens and adds user context headers
 */
@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtUtils jwtUtils;
    private final List<String> publicPaths;

    public AuthenticationFilter(JwtUtils jwtUtils) {
        super(Config.class);
        this.jwtUtils = jwtUtils;
        // Hardcode public paths for now to avoid injection issues
        this.publicPaths = Arrays.asList(
                "/api/v1/auth/register",
                "/api/v1/auth/login",
                "/api/v1/auth/refresh",
                "/api/v1/auth/forgot-password",
                "/api/v1/auth/reset-password",
                "/actuator/health",
                "/actuator/prometheus",
                "/swagger-ui/**",
                "/v3/api-docs/**"
        );
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Skip authentication for public paths
            if (isPublicPath(request.getPath().toString())) {
                return chain.filter(exchange);
            }

            // Extract Authorization header
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                // Validate token and extract claims
                if (!jwtUtils.isValidToken(token)) {
                    return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
                }

                UUID userId = jwtUtils.extractUserId(token);
                UUID tenantId = jwtUtils.extractTenantId(token);
                String correlationId = jwtUtils.extractCorrelationId(token);

                // Add user context to headers for downstream services
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", userId != null ? userId.toString() : "")
                        .header("X-Tenant-Id", tenantId != null ? tenantId.toString() : "")
                        .build();

                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(mutatedRequest)
                        .build();

                log.debug("Authenticated request for user: {} tenant: {}", userId, tenantId);

                return chain.filter(mutatedExchange);

            } catch (Exception e) {
                log.error("Token validation failed: {}", e.getMessage());
                return onError(exchange, "Token validation failed", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private boolean isPublicPath(String path) {
        return publicPaths != null && publicPaths.stream()
                .anyMatch(publicPath -> path.matches(publicPath.replace("**", ".*")));
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().add("Content-Type", "application/json");

        String body = String.format("{\"error\": \"%s\", \"status\": %d}", err, httpStatus.value());

        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    public static class Config {
        // Configuration properties can be added here if needed
    }
}