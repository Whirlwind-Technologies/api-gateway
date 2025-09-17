package com.nnipa.gateway.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting service using Bucket4j
 */
@Slf4j
@Service
public class RateLimitService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, Instant> resetTimes = new ConcurrentHashMap<>();

    /**
     * Check if request is allowed based on rate limit
     */
    public boolean allowRequest(String key, int requestsPerSecond, int burstCapacity) {
        Bucket bucket = getBucket(key, requestsPerSecond, burstCapacity);
        boolean allowed = bucket.tryConsume(1);

        if (allowed) {
            log.debug("Rate limit allowed for key: {}", key);
        } else {
            log.debug("Rate limit denied for key: {}", key);
        }

        return allowed;
    }

    /**
     * Get or create bucket for the given key
     */
    private Bucket getBucket(String key, int requestsPerSecond, int burstCapacity) {
        return buckets.computeIfAbsent(key, k -> {
            Bandwidth limit = Bandwidth.classic(
                    burstCapacity,
                    Refill.intervally(requestsPerSecond, Duration.ofSeconds(1))
            );

            // Set reset time
            resetTimes.put(key, Instant.now().plusSeconds(1));

            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        });
    }

    /**
     * Get remaining tokens for a key
     */
    public long getRemainingTokens(String key) {
        Bucket bucket = buckets.get(key);
        return bucket != null ? bucket.getAvailableTokens() : 0;
    }

    /**
     * Get reset time for a key
     */
    public long getResetTime(String key) {
        Instant resetTime = resetTimes.get(key);
        return resetTime != null ? resetTime.toEpochMilli() :
                Instant.now().plusSeconds(1).toEpochMilli();
    }

    /**
     * Reset rate limit for a key
     */
    public void reset(String key) {
        buckets.remove(key);
        resetTimes.remove(key);
        log.info("Rate limit reset for key: {}", key);
    }

    /**
     * Clear all rate limits (for testing/admin purposes)
     */
    public void clearAll() {
        buckets.clear();
        resetTimes.clear();
        log.info("All rate limits cleared");
    }
}