package com.nnipa.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "api-gateway")
public class ApiGatewayProperties {

    private Security security = new Security();
    private RateLimiting rateLimiting = new RateLimiting();
    private Correlation correlation = new Correlation();

    @Data
    public static class Security {
        private String jwtSecret;
        private String jwtIssuer;
        private List<String> publicPaths;
    }

    @Data
    public static class RateLimiting {
        private boolean enabled = true;
        private DefaultLimits defaultLimits = new DefaultLimits();
        private boolean perTenantEnabled = true;
        private int cacheSize = 10000;

        @Data
        public static class DefaultLimits {
            private int requestsPerSecond = 100;
            private int burstCapacity = 200;
        }
    }

    @Data
    public static class Correlation {
        private String headerName = "X-Correlation-Id";
        private boolean generateIfMissing = true;
    }
}