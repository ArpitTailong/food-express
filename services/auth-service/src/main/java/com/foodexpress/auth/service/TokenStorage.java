package com.foodexpress.auth.service;

import com.foodexpress.auth.domain.AuthDomain.Session;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Interface for token storage operations.
 * Provides session management, token blacklisting, and login attempt tracking.
 */
public interface TokenStorage {
    
    // Session Management
    Session createSession(String userId, String refreshToken, String deviceInfo, String ipAddress);
    Optional<Session> getSession(String sessionId);
    Optional<Session> getSessionByRefreshToken(String refreshToken);
    void touchSession(String sessionId);
    void invalidateSession(String sessionId);
    void invalidateAllUserSessions(String userId);
    Set<String> getUserActiveSessions(String userId);
    
    // Token Blacklist
    void blacklistToken(String tokenId, Instant expiresAt);
    boolean isTokenBlacklisted(String tokenId);
    
    // Login Attempts
    void recordLoginAttempt(String email, boolean success);
    boolean isAccountLocked(String email);
    int getRemainingAttempts(String email);
}
