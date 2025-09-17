package com.nnipa.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nnipa.gateway.service.RateLimitService;
import com.nnipa.gateway.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Main gateway configuration class
 */
@Slf4j
@Configuration
public class GatewayConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Key resolver for rate limiting
     * Resolves based on tenant ID, user ID, or IP address
     */
    @Bean
    @Primary
    public KeyResolver keyResolver() {
        return exchange -> {
            // Try tenant ID first
            String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
            if (tenantId != null && !tenantId.isEmpty()) {
                return Mono.just("tenant:" + tenantId);
            }

            // Then try user ID
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just("user:" + userId);
            }

            // Default to IP address
            String ip = exchange.getRequest().getRemoteAddress() != null ?
                    exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() :
                    "unknown";
            return Mono.just("ip:" + ip);
        };
    }
}