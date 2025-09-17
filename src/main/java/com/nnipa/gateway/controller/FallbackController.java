package com.nnipa.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Fallback controller for circuit breaker
 * Provides graceful degradation when services are unavailable
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping(value = "/auth", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public Mono<ResponseEntity<Map<String, Object>>> authServiceFallback() {
        log.warn("Auth service circuit breaker activated");
        return createFallbackResponse("Authentication Service",
                "The authentication service is temporarily unavailable. Please try again later.");
    }

    @RequestMapping(value = "/tenant", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public Mono<ResponseEntity<Map<String, Object>>> tenantServiceFallback() {
        log.warn("Tenant service circuit breaker activated");
        return createFallbackResponse("Tenant Service",
                "The tenant service is temporarily unavailable. Please try again later.");
    }

    @RequestMapping(value = "/authorization", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public Mono<ResponseEntity<Map<String, Object>>> authorizationServiceFallback() {
        log.warn("Authorization service circuit breaker activated");
        return createFallbackResponse("Authorization Service",
                "The authorization service is temporarily unavailable. Please try again later.");
    }

    @RequestMapping(value = "/user", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public Mono<ResponseEntity<Map<String, Object>>> userServiceFallback() {
        log.warn("User service circuit breaker activated");
        return createFallbackResponse("User Management Service",
                "The user management service is temporarily unavailable. Please try again later.");
    }

    @RequestMapping(value = "/notification", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public Mono<ResponseEntity<Map<String, Object>>> notificationServiceFallback() {
        log.warn("Notification service circuit breaker activated");
        return createFallbackResponse("Notification Service",
                "The notification service is temporarily unavailable. Please try again later.");
    }

    @RequestMapping(value = "/storage", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public Mono<ResponseEntity<Map<String, Object>>> storageServiceFallback() {
        log.warn("Storage service circuit breaker activated");
        return createFallbackResponse("Storage Service",
                "The storage service is temporarily unavailable. Please try again later.");
    }

    private Mono<ResponseEntity<Map<String, Object>>> createFallbackResponse(
            String service, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Service Unavailable");
        response.put("service", service);
        response.put("message", message);
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("timestamp", Instant.now().toString());

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }
}