package com.nnipa.gateway.util;

import java.util.UUID;

public class CorrelationIdUtils {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    public static String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    public static boolean isValid(String correlationId) {
        if (correlationId == null || correlationId.trim().isEmpty()) {
            return false;
        }
        try {
            UUID.fromString(correlationId);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String getHeaderName() {
        return CORRELATION_ID_HEADER;
    }
}