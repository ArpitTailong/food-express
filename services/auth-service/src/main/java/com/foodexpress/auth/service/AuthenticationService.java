package com.foodexpress.auth.service;

import com.foodexpress.auth.domain.AuthDomain.*;
import com.foodexpress.auth.dto.AuthDTOs.*;
import com.foodexpress.common.exception.FoodExpressException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Main authentication service orchestrating login, logout, and token operations.
 */
@Service
public class AuthenticationService {
    
    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    
    private final JwtTokenService jwtTokenService;
    private final TokenStorage tokenStorage;
    private final PasswordEncoder passwordEncoder;
    
    public AuthenticationService(JwtTokenService jwtTokenService,
                                  TokenStorage tokenStorage,
                                  PasswordEncoder passwordEncoder) {
        this.jwtTokenService = jwtTokenService;
        this.tokenStorage = tokenStorage;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * Authenticate user with email and password
     */
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        log.info("Login attempt for: {}", request.email());
        
        // Check if account is locked
        if (tokenStorage.isAccountLocked(request.email())) {
            throw new FoodExpressException.UnauthorizedException(
                    "Account is temporarily locked due to too many failed attempts");
        }
        
        // Find user and validate password
        // In real implementation, this would call User Service
        Optional<UserCredentials> userOpt = findUserByEmail(request.email());
        
        if (userOpt.isEmpty() || !passwordEncoder.matches(request.password(), userOpt.get().passwordHash())) {
            tokenStorage.recordLoginAttempt(request.email(), false);
            int remaining = tokenStorage.getRemainingAttempts(request.email());
            throw new FoodExpressException.UnauthorizedException(
                    "Invalid credentials. %d attempts remaining.".formatted(remaining));
        }
        
        UserCredentials user = userOpt.get();
        
        // Check if user is enabled
        if (!user.enabled()) {
            throw new FoodExpressException.UnauthorizedException("Account is disabled");
        }
        
        if (!user.accountNonLocked()) {
            throw new FoodExpressException.UnauthorizedException("Account is locked");
        }
        
        // Generate tokens
        TokenPair tokens = jwtTokenService.generateTokenPair(user);
        
        // Create session
        Session session = tokenStorage.createSession(
                user.userId(),
                tokens.refreshToken(),
                userAgent,
                ipAddress
        );
        
        // Record successful login
        tokenStorage.recordLoginAttempt(request.email(), true);
        
        log.info("Login successful for user: {}", user.userId());
        
        return new LoginResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.tokenType(),
                tokens.accessTokenExpiresIn(),
                new UserInfo(
                        user.userId(),
                        user.email(),
                        user.roles().stream().map(Role::name).toList()
                ),
                session.sessionId()
        );
    }
    
    /**
     * Refresh access token using refresh token
     */
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        log.debug("Token refresh requested");
        
        // Validate refresh token
        TokenClaims claims = jwtTokenService.validateTokenOrThrow(request.refreshToken());
        
        if (claims.tokenType() != TokenType.REFRESH) {
            throw new FoodExpressException.UnauthorizedException("Invalid token type");
        }
        
        // Check if token is blacklisted
        if (tokenStorage.isTokenBlacklisted(claims.tokenId())) {
            throw new FoodExpressException.UnauthorizedException("Token has been revoked");
        }
        
        // Find session
        Optional<Session> sessionOpt = tokenStorage.getSessionByRefreshToken(request.refreshToken());
        if (sessionOpt.isEmpty()) {
            throw new FoodExpressException.UnauthorizedException("Session not found");
        }
        
        // Get user credentials
        Optional<UserCredentials> userOpt = findUserById(claims.subject());
        if (userOpt.isEmpty()) {
            throw new FoodExpressException.UnauthorizedException("User not found");
        }
        
        UserCredentials user = userOpt.get();
        
        // Generate new access token
        String newAccessToken = jwtTokenService.generateAccessToken(user);
        
        // Touch session
        tokenStorage.touchSession(sessionOpt.get().sessionId());
        
        log.debug("Token refreshed for user: {}", user.userId());
        
        return new TokenRefreshResponse(
                newAccessToken,
                "Bearer",
                900 // 15 minutes
        );
    }
    
    /**
     * Logout - invalidate session and blacklist token
     */
    public void logout(String accessToken, String sessionId) {
        log.info("Logout requested for session: {}", sessionId);
        
        // Validate and get token claims
        jwtTokenService.validateToken(accessToken).ifPresent(claims -> {
            // Blacklist the access token
            tokenStorage.blacklistToken(claims.tokenId(), claims.expiresAt());
        });
        
        // Invalidate session
        if (sessionId != null) {
            tokenStorage.invalidateSession(sessionId);
        }
    }
    
    /**
     * Logout from all devices
     */
    public void logoutAll(String userId, String currentAccessToken) {
        log.info("Logout all devices for user: {}", userId);
        
        // Blacklist current token
        jwtTokenService.validateToken(currentAccessToken).ifPresent(claims -> {
            tokenStorage.blacklistToken(claims.tokenId(), claims.expiresAt());
        });
        
        // Invalidate all sessions
        tokenStorage.invalidateAllUserSessions(userId);
    }
    
    /**
     * Validate token and return claims
     */
    public Optional<TokenClaims> validateToken(String token) {
        Optional<TokenClaims> claimsOpt = jwtTokenService.validateToken(token);
        
        if (claimsOpt.isEmpty()) {
            return Optional.empty();
        }
        
        TokenClaims claims = claimsOpt.get();
        
        // Check blacklist
        if (tokenStorage.isTokenBlacklisted(claims.tokenId())) {
            log.debug("Token is blacklisted: {}", claims.tokenId());
            return Optional.empty();
        }
        
        return Optional.of(claims);
    }
    
    // ========================================
    // MOCK USER LOOKUP (Replace with Feign Client)
    // ========================================
    
    private Optional<UserCredentials> findUserByEmail(String email) {
        // TODO: Replace with actual User Service call via Feign Client
        // This is a mock for demonstration
        if ("admin@foodexpress.com".equals(email)) {
            return Optional.of(new UserCredentials(
                    "user-001",
                    "admin@foodexpress.com",
                    passwordEncoder.encode("password123"),
                    Set.of(Role.ADMIN, Role.CUSTOMER),
                    Set.of(Permission.ADMIN_ACCESS, Permission.ORDER_READ_ALL),
                    true,
                    true,
                    Instant.now().minus(Duration.ofDays(30)),
                    0
            ));
        }
        
        if ("customer@foodexpress.com".equals(email)) {
            return Optional.of(new UserCredentials(
                    "user-002",
                    "customer@foodexpress.com",
                    passwordEncoder.encode("password123"),
                    Set.of(Role.CUSTOMER),
                    Set.of(Permission.ORDER_CREATE, Permission.ORDER_READ, Permission.PAYMENT_INITIATE),
                    true,
                    true,
                    Instant.now().minus(Duration.ofDays(30)),
                    0
            ));
        }
        
        return Optional.empty();
    }
    
    private Optional<UserCredentials> findUserById(String userId) {
        // TODO: Replace with actual User Service call
        return switch (userId) {
            case "user-001" -> findUserByEmail("admin@foodexpress.com");
            case "user-002" -> findUserByEmail("customer@foodexpress.com");
            default -> Optional.empty();
        };
    }
}
