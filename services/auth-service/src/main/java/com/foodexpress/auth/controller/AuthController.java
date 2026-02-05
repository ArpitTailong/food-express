package com.foodexpress.auth.controller;

import com.foodexpress.auth.domain.AuthDomain.TokenClaims;
import com.foodexpress.auth.dto.AuthDTOs.*;
import com.foodexpress.auth.service.AuthenticationService;
import com.foodexpress.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Authentication REST Controller.
 * Handles login, logout, token refresh, and token validation.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {
    
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthenticationService authenticationService;
    
    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }
    
    // ========================================
    // PUBLIC ENDPOINTS
    // ========================================
    
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate with email and password")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        
        LoginResponse response = authenticationService.login(request, ipAddress, userAgent);
        
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Get new access token using refresh token")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        
        TokenRefreshResponse response = authenticationService.refreshToken(request);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout", description = "Invalidate current session")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @RequestBody(required = false) LogoutRequest request) {
        
        String token = extractToken(authHeader);
        String sessionId = request != null ? request.sessionId() : null;
        
        authenticationService.logout(token, sessionId);
        
        return ResponseEntity.ok(ApiResponse.<Void>success("Logged out successfully", null));
    }
    
    @PostMapping("/logout-all")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout all devices", description = "Invalidate all sessions for current user")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        
        String token = extractToken(authHeader);
        Optional<TokenClaims> claims = authenticationService.validateToken(token);
        
        if (claims.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid token"));
        }
        
        authenticationService.logoutAll(claims.get().subject(), token);
        
        return ResponseEntity.ok(ApiResponse.<Void>success("Logged out from all devices", null));
    }
    
    // ========================================
    // TOKEN VALIDATION (for other services)
    // ========================================
    
    @PostMapping("/validate")
    @Operation(summary = "Validate token", description = "Validate access token and return claims")
    public ResponseEntity<ApiResponse<TokenValidationResponse>> validateToken(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        
        String token = extractToken(authHeader);
        Optional<TokenClaims> claims = authenticationService.validateToken(token);
        
        if (claims.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(TokenValidationResponse.invalid()));
        }
        
        TokenClaims c = claims.get();
        TokenValidationResponse response = new TokenValidationResponse(
                true,
                c.subject(),
                c.email(),
                c.roles().stream().toList(),
                c.permissions().stream().toList(),
                c.expiresAt().toEpochMilli()
        );
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current user", description = "Get current authenticated user info")
    public ResponseEntity<ApiResponse<UserInfo>> getCurrentUser(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        
        String token = extractToken(authHeader);
        Optional<TokenClaims> claims = authenticationService.validateToken(token);
        
        if (claims.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid token"));
        }
        
        TokenClaims c = claims.get();
        UserInfo userInfo = new UserInfo(
                c.subject(),
                c.email(),
                c.roles().stream().toList()
        );
        
        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }
    
    // ========================================
    // HELPER METHODS
    // ========================================
    
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
