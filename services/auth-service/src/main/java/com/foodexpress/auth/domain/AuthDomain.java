package com.foodexpress.auth.domain;

import java.time.Instant;
import java.util.Set;

/**
 * Domain records for authentication.
 * Uses Java 21 Records for immutability.
 */
public final class AuthDomain {
    
    private AuthDomain() {}
    
    /**
     * User credentials for authentication
     */
    public record UserCredentials(
            String userId,
            String email,
            String passwordHash,
            Set<Role> roles,
            Set<Permission> permissions,
            boolean enabled,
            boolean accountNonLocked,
            Instant passwordChangedAt,
            int failedLoginAttempts
    ) {}
    
    /**
     * Role enumeration
     */
    public enum Role {
        CUSTOMER("ROLE_CUSTOMER"),
        RESTAURANT_OWNER("ROLE_RESTAURANT_OWNER"),
        DELIVERY_PARTNER("ROLE_DELIVERY_PARTNER"),
        SUPPORT("ROLE_SUPPORT"),
        ADMIN("ROLE_ADMIN");
        
        private final String authority;
        
        Role(String authority) {
            this.authority = authority;
        }
        
        public String getAuthority() {
            return authority;
        }
    }
    
    /**
     * Fine-grained permissions
     */
    public enum Permission {
        // Order permissions
        ORDER_CREATE,
        ORDER_READ,
        ORDER_UPDATE,
        ORDER_CANCEL,
        ORDER_READ_ALL,
        
        // Payment permissions
        PAYMENT_INITIATE,
        PAYMENT_REFUND,
        PAYMENT_READ,
        PAYMENT_READ_ALL,
        
        // User permissions
        USER_READ,
        USER_UPDATE,
        USER_DELETE,
        USER_MANAGE_ALL,
        
        // Restaurant permissions
        RESTAURANT_CREATE,
        RESTAURANT_UPDATE,
        RESTAURANT_DELETE,
        RESTAURANT_MANAGE_MENU,
        
        // Analytics permissions
        ANALYTICS_READ,
        ANALYTICS_EXPORT,
        
        // Admin permissions
        ADMIN_ACCESS,
        SYSTEM_CONFIG
    }
    
    /**
     * Token pair (access + refresh)
     */
    public record TokenPair(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresIn,
            long refreshTokenExpiresIn,
            String tokenType
    ) {
        public static TokenPair of(String accessToken, String refreshToken, 
                                    long accessExpiresIn, long refreshExpiresIn) {
            return new TokenPair(accessToken, refreshToken, accessExpiresIn, refreshExpiresIn, "Bearer");
        }
    }
    
    /**
     * Token claims extracted from JWT
     */
    public record TokenClaims(
            String subject,
            String email,
            String firstName,
            String lastName,
            Set<String> roles,
            Set<String> permissions,
            String tenantId,
            Instant issuedAt,
            Instant expiresAt,
            String tokenId,
            TokenType tokenType
    ) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
    
    /**
     * Token type
     */
    public enum TokenType {
        ACCESS,
        REFRESH,
        SERVICE
    }
    
    /**
     * Session information stored in Redis
     */
    public record Session(
            String sessionId,
            String userId,
            String refreshTokenHash,
            String deviceInfo,
            String ipAddress,
            Instant createdAt,
            Instant lastAccessedAt,
            Instant expiresAt
    ) {}
    
    /**
     * Login attempt record for security
     */
    public record LoginAttempt(
            String userId,
            String ipAddress,
            String userAgent,
            Instant attemptedAt,
            boolean successful,
            String failureReason
    ) {}
}
