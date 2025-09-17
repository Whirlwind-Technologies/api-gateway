package com.nnipa.gateway.service;

import com.nnipa.gateway.metrics.GatewayMetricsCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for collecting and managing metrics
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final GatewayMetricsCollector metricsCollector;

    public void recordRequest(String method, String path, int status, long duration) {
        metricsCollector.incrementRequestCount();
        metricsCollector.recordRequestWithTags(method, path, status);
        metricsCollector.recordRequestDuration(duration, TimeUnit.MILLISECONDS);

        if (status >= 400) {
            metricsCollector.incrementErrorCount();
        }

        log.debug("Recorded metrics - Method: {}, Path: {}, Status: {}, Duration: {}ms",
                method, path, status, duration);
    }

    public void recordRateLimitExceeded() {
        metricsCollector.incrementRateLimitCount();
    }

    public void recordError() {
        metricsCollector.incrementErrorCount();
    }
}