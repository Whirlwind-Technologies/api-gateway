package com.nnipa.gateway.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom health indicator for gateway and downstream services
 */
@Component
@RequiredArgsConstructor
public class GatewayHealthIndicator implements HealthIndicator {

    private final WebClient.Builder webClientBuilder;

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        details.put("gateway", "UP");
        details.put("timestamp", System.currentTimeMillis());

        // Check downstream services health
        checkServiceHealth("auth-service", "http://localhost:4002/actuator/health", details);
        checkServiceHealth("tenant-service", "http://localhost:8080/actuator/health", details);
        checkServiceHealth("authz-service", "http://localhost:8082/actuator/health", details);

        return Health.up().withDetails(details).build();
    }

    private void checkServiceHealth(String serviceName, String healthUrl, Map<String, Object> details) {
        try {
            WebClient webClient = webClientBuilder.build();

            Mono<String> healthMono = webClient.get()
                    .uri(healthUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(2))
                    .onErrorReturn("DOWN");

            String health = healthMono.block();
            details.put(serviceName, health != null && health.contains("UP") ? "UP" : "DOWN");

        } catch (Exception e) {
            details.put(serviceName, "DOWN");
        }
    }
}