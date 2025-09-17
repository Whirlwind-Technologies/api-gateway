package com.nnipa.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Global filter for logging requests and responses
 */
@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst("X-Correlation-Id");
        Instant startTime = Instant.now();

        String path = request.getPath().toString();
        String method = request.getMethod().toString();
        String query = request.getURI().getRawQuery();

        log.info("[{}] Incoming request: {} {} {}",
                correlationId,
                method,
                path,
                query != null ? "?" + query : "");

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            Duration duration = Duration.between(startTime, Instant.now());

            log.info("[{}] Response: {} {} - Status: {} - Duration: {}ms",
                    correlationId,
                    method,
                    path,
                    response.getStatusCode(),
                    duration.toMillis());
        }));
    }

    @Override
    public int getOrder() {
        // Run after correlation ID filter but before other filters
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}