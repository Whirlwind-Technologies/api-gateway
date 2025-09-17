package com.nnipa.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter for response transformation
 * Adds security headers and transforms responses
 */
@Slf4j
@Component
public class ResponseTransformationFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            HttpHeaders headers = response.getHeaders();

            // Add security headers
            headers.add("X-Content-Type-Options", "nosniff");
            headers.add("X-Frame-Options", "DENY");
            headers.add("X-XSS-Protection", "1; mode=block");
            headers.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

            // Add custom headers
            headers.add("X-Gateway-Response-Time", String.valueOf(System.currentTimeMillis()));
            headers.add("X-Powered-By", "NNIPA API Gateway");

            // Remove sensitive headers from response
            headers.remove("Server");
            headers.remove("X-Powered-By");
            headers.remove("X-AspNet-Version");
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}