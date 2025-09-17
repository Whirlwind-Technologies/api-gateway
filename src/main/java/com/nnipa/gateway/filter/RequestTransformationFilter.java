package com.nnipa.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Global filter for request transformation
 * Adds common headers and metadata to all requests
 */
@Slf4j
@Component
public class RequestTransformationFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Add common headers including timestamp
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Gateway-Timestamp", String.valueOf(System.currentTimeMillis()))
                .header("X-Gateway-Version", "v1")
                .header("X-Original-Host", request.getHeaders().getFirst("Host"))
                .header("X-Original-Path", request.getPath().toString())
                .header("X-Original-Method", request.getMethod().toString())
                .build();

        // Remove sensitive headers before forwarding
        mutatedRequest = removeSensitiveHeaders(mutatedRequest);

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        return chain.filter(mutatedExchange);
    }

    private ServerHttpRequest removeSensitiveHeaders(ServerHttpRequest request) {
        return request.mutate()
                .headers(headers -> {
                    // Remove sensitive headers that shouldn't be forwarded
                    headers.remove("Cookie");
                    headers.remove("Set-Cookie");
                })
                .build();
    }

    @Override
    public int getOrder() {
        // Run after authentication but before routing
        return 10;
    }
}