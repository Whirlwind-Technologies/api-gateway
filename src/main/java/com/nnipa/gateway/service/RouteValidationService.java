package com.nnipa.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Service for validating routes and paths
 */
@Slf4j
@Service
public class RouteValidationService {

    private final List<String> publicPaths;

    public RouteValidationService() {
        // Hardcode public paths to avoid injection issues
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

    /**
     * Check if the path is public (no authentication required)
     */
    public boolean isPublicPath(String path) {
        if (publicPaths == null || publicPaths.isEmpty()) {
            return false;
        }

        return publicPaths.stream()
                .anyMatch(publicPath -> pathMatches(path, publicPath));
    }

    /**
     * Check if the path requires authentication
     */
    public boolean isSecuredPath(String path) {
        return !isPublicPath(path);
    }

    /**
     * Match path with pattern (supports wildcards)
     */
    private boolean pathMatches(String path, String pattern) {
        // Convert pattern to regex
        String regex = pattern
                .replace("**", ".*")
                .replace("*", "[^/]*");

        return path.matches(regex);
    }
}