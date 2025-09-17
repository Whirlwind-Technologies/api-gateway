package com.nnipa.gateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtils {

    private final String jwtSecret;
    private final String jwtIssuer;

    public JwtUtils(@Value("${JWT_SECRET:dGhpcyBpcyBhIDUxMi1iaXQgc2VjcmV0IGtleSBmb3IgSFM1MTIgand0IHNpZ25pbmcgYWxnb3JpdGhtIGVuY29kZWQgaW4gYmFzZTY0IGZvcm1hdCB0aGF0IHlvdSBjYW4gdXNlIGZvciBzZWN1cmUgdG9rZW4=}") String jwtSecret,
                    @Value("${JWT_ISSUER:https://nnipa.cloud}") String jwtIssuer) {
        this.jwtSecret = jwtSecret;
        this.jwtIssuer = jwtIssuer;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims validateAndGetClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
            throw new RuntimeException("Token expired");
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
            throw new RuntimeException("Token unsupported");
        } catch (MalformedJwtException e) {
            log.error("JWT token is malformed: {}", e.getMessage());
            throw new RuntimeException("Token malformed");
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            throw new RuntimeException("Token invalid");
        } catch (Exception e) {
            log.error("Error validating JWT token: {}", e.getMessage());
            throw new RuntimeException("Token validation failed");
        }
    }

    public boolean isValidToken(String token) {
        try {
            validateAndGetClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        Claims claims = validateAndGetClaims(token);
        String userId = claims.get("userId", String.class);
        return userId != null ? UUID.fromString(userId) : null;
    }

    public UUID extractTenantId(String token) {
        Claims claims = validateAndGetClaims(token);
        String tenantId = claims.get("tenantId", String.class);
        return tenantId != null ? UUID.fromString(tenantId) : null;
    }

    public String extractCorrelationId(String token) {
        Claims claims = validateAndGetClaims(token);
        return claims.get("correlationId", String.class);
    }
}