package com.foodexpress.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Authentication DTOs using Java 21 Records.
 */
public final class AuthDTOs {
    
    private AuthDTOs() {}
    
    // ========================================
    // REQUEST DTOs
    // ========================================
    
    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,
            
            @NotBlank(message = "Password is required")
            @Size(min = 8, message = "Password must be at least 8 characters")
            String password
    ) {}
    
    public record TokenRefreshRequest(
            @NotBlank(message = "Refresh token is required")
            String refreshToken
    ) {}
    
    public record LogoutRequest(
            String sessionId
    ) {}
    
    public record PasswordChangeRequest(
            @NotBlank(message = "Current password is required")
            String currentPassword,
            
            @NotBlank(message = "New password is required")
            @Size(min = 8, message = "Password must be at least 8 characters")
            String newPassword,
            
            @NotBlank(message = "Password confirmation is required")
            String confirmPassword
    ) {
        public boolean passwordsMatch() {
            return newPassword.equals(confirmPassword);
        }
    }
    
    public record PasswordResetRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email
    ) {}
    
    public record PasswordResetConfirmRequest(
            @NotBlank(message = "Reset token is required")
            String resetToken,
            
            @NotBlank(message = "New password is required")
            @Size(min = 8, message = "Password must be at least 8 characters")
            String newPassword,
            
            @NotBlank(message = "Password confirmation is required")
            String confirmPassword
    ) {}
    
    // ========================================
    // RESPONSE DTOs
    // ========================================
    
    public record LoginResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UserInfo user,
            String sessionId
    ) {}
    
    public record TokenRefreshResponse(
            String accessToken,
            String tokenType,
            long expiresIn
    ) {}
    
    public record UserInfo(
            String id,
            String email,
            List<String> roles
    ) {}
    
    public record TokenValidationResponse(
            boolean valid,
            String userId,
            String email,
            List<String> roles,
            List<String> permissions,
            long expiresAt
    ) {
        public static TokenValidationResponse invalid() {
            return new TokenValidationResponse(false, null, null, null, null, 0);
        }
    }
    
    public record SessionInfo(
            String sessionId,
            String deviceInfo,
            String ipAddress,
            String createdAt,
            String lastAccessedAt,
            boolean isCurrent
    ) {}
    
    public record ActiveSessionsResponse(
            List<SessionInfo> sessions,
            int totalCount
    ) {}
    
    // ========================================
    // REGISTRATION DTOs
    // ========================================
    
    public record RegisterRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,
            
            @NotBlank(message = "Password is required")
            @Size(min = 8, message = "Password must be at least 8 characters")
            String password,
            
            @NotBlank(message = "First name is required")
            String firstName,
            
            @NotBlank(message = "Last name is required")
            String lastName
    ) {}
}
