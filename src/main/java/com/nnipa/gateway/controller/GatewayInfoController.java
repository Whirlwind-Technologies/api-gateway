package com.nnipa.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for gateway information and management
 */
@Slf4j
@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
public class GatewayInfoController {

    private final RouteLocator routeLocator;

    @GetMapping("/info")
    public Mono<Map<String, Object>> getGatewayInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "API Gateway");
        info.put("version", "1.0.0");
        info.put("timestamp", Instant.now().toString());
        info.put("status", "UP");

        return Mono.just(info);
    }

    @GetMapping("/routes")
    public Flux<Map<String, Object>> getRoutes() {
        return routeLocator.getRoutes()
                .map(this::routeToMap);
    }

    private Map<String, Object> routeToMap(Route route) {
        Map<String, Object> routeMap = new HashMap<>();
        routeMap.put("id", route.getId());
        routeMap.put("uri", route.getUri().toString());
        routeMap.put("order", route.getOrder());
        routeMap.put("predicates", route.getPredicate().toString());

        return routeMap;
    }
}