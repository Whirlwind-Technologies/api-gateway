package com.nnipa.gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Custom metrics collector for gateway
 */
@Slf4j
@Component
public class GatewayMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final Counter requestCounter;
    private final Counter errorCounter;
    private final Counter rateLimitCounter;
    private final Timer requestTimer;

    public GatewayMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.requestCounter = Counter.builder("gateway.requests.total")
                .description("Total number of requests")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("gateway.errors.total")
                .description("Total number of errors")
                .register(meterRegistry);

        this.rateLimitCounter = Counter.builder("gateway.ratelimit.exceeded")
                .description("Number of rate limit exceeded events")
                .register(meterRegistry);

        this.requestTimer = Timer.builder("gateway.request.duration")
                .description("Request duration")
                .register(meterRegistry);
    }

    public void incrementRequestCount() {
        requestCounter.increment();
    }

    public void incrementErrorCount() {
        errorCounter.increment();
    }

    public void incrementRateLimitCount() {
        rateLimitCounter.increment();
    }

    public void recordRequestDuration(long duration, TimeUnit unit) {
        requestTimer.record(duration, unit);
    }

    public void recordRequestWithTags(String method, String path, int status) {
        Counter.builder("gateway.requests")
                .tag("method", method)
                .tag("path", path)
                .tag("status", String.valueOf(status))
                .register(meterRegistry)
                .increment();
    }
}
