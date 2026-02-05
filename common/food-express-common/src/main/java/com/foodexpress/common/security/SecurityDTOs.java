package com.foodexpress.common.security;

import java.util.List;
import java.util.Set;

/**
 * Security-related DTOs using Java 21 Records.
 * Immutable by design for thread-safety.
 */
public final class SecurityDTOs {
    
    private SecurityDTOs() {} // Utility class
    
    /**
     * Represents the authenticated user context.
     * Available throughout request processing.
     */
    public record UserPrincipal(
            String userId,
            String email,
            String firstName,
            String lastName,
            Set<String> roles,
            Set<String> permissions,
            String tenantId
    ) {
        public boolean hasRole(String role) {
            return roles != null && roles.contains(role);
        }
        
        public boolean hasPermission(String permission) {
            return permissions != null && permissions.contains(permission);
        }
        
        public boolean hasAnyRole(String... checkRoles) {
            if (roles == null) return false;
            for (String role : checkRoles) {
                if (roles.contains(role)) return true;
            }
            return false;
        }
        
        public String fullName() {
            return "%s %s".formatted(firstName, lastName);
        }
    }
    
    /**
     * JWT Token payload structure
     */
    public record JwtPayload(
            String sub,        // Subject (user ID)
            String email,
            String firstName,
            String lastName,
            List<String> roles,
            List<String> permissions,
            String tenantId,
            long iat,          // Issued at
            long exp,          // Expiration
            String jti,        // JWT ID
            String iss         // Issuer
    ) {
        public boolean isExpired() {
            return System.currentTimeMillis() / 1000 > exp;
        }
        
        public UserPrincipal toUserPrincipal() {
            return new UserPrincipal(
                    sub,
                    email,
                    firstName,
                    lastName,
                    roles != null ? Set.copyOf(roles) : Set.of(),
                    permissions != null ? Set.copyOf(permissions) : Set.of(),
                    tenantId
            );
        }
    }
    
    /**
     * Authentication request DTO
     */
    public record AuthRequest(
            String email,
            String password
    ) {}
    
    /**
     * Authentication response with tokens
     */
    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UserInfo user
    ) {
        public record UserInfo(
                String id,
                String email,
                String firstName,
                String lastName,
                List<String> roles
        ) {}
        
        public static AuthResponse of(String accessToken, String refreshToken, 
                                       long expiresIn, UserInfo user) {
            return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
        }
    }
    
    /**
     * Token refresh request
     */
    public record RefreshTokenRequest(String refreshToken) {}
    
    /**
     * OAuth2 callback data
     */
    public record OAuth2CallbackData(
            String code,
            String state,
            String redirectUri
    ) {}
    
    /**
     * Service-to-service authentication context
     */
    public record ServiceContext(
            String serviceId,
            String serviceName,
            String correlationId,
            long timestamp
    ) {
        public static ServiceContext create(String serviceId, String serviceName, String correlationId) {
            return new ServiceContext(serviceId, serviceName, correlationId, System.currentTimeMillis());
        }
    }
}
