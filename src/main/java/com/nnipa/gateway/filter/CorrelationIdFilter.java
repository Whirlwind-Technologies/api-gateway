package com.nnipa.gateway.filter;

import com.nnipa.gateway.util.CorrelationIdUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Global filter that ensures correlation ID is present in all requests
 * This filter runs FIRST to establish correlation context
 */
@Slf4j
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Try to extract correlation ID from headers
        String correlationId = request.getHeaders()
                .getFirst(CorrelationIdUtils.getHeaderName());

        // Validate and generate if necessary
        if (!CorrelationIdUtils.isValid(correlationId)) {
            correlationId = CorrelationIdUtils.generateCorrelationId();
            log.debug("Generated new correlation ID: {}", correlationId);
        } else {
            log.debug("Using existing correlation ID: {}", correlationId);
        }

        // Add correlation ID to request headers for downstream services
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CorrelationIdUtils.getHeaderName(), correlationId)
                .build();

        // Add correlation ID to response headers
        exchange.getResponse().getHeaders()
                .add(CorrelationIdUtils.getHeaderName(), correlationId);

        // Create new exchange with modified request
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        // Log request entry with correlation ID
        log.info("Request: {} {} [correlationId: {}]",
                request.getMethod(),
                request.getPath(),
                correlationId);

        String finalCorrelationId = correlationId;

        // Continue filter chain with correlation context
        return chain.filter(mutatedExchange)
                .contextWrite(Context.of(CORRELATION_ID_KEY, finalCorrelationId))
                .doOnSuccess(aVoid -> {
                    log.info("Request completed successfully [correlationId: {}]", finalCorrelationId);
                })
                .doOnError(throwable -> {
                    log.error("Request failed [correlationId: {}]: {}",
                            finalCorrelationId, throwable.getMessage());
                });
    }

    @Override
    public int getOrder() {
        // Run this filter first to establish correlation context
        return Ordered.HIGHEST_PRECEDENCE;
    }
}