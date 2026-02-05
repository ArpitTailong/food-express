package com.foodexpress.auth.service;

import com.foodexpress.auth.domain.AuthDomain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of token storage for local development.
 * Replaces Redis-based TokenStorageService when running with 'local' profile.
 */
@Service
@Profile("local")
@Primary
public class InMemoryTokenStorageService implements TokenStorage {
    
    private static final Logger log = LoggerFactory.getLogger(InMemoryTokenStorageService.class);
    
    private static final long SESSION_TTL_DAYS = 7;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOGIN_LOCKOUT_MINUTES = 30;
    
    // In-memory storage
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    private final Map<String, String> refreshTokenMap = new ConcurrentHashMap<>();
    private final Map<String, LoginAttempts> loginAttempts = new ConcurrentHashMap<>();
    
    record LoginAttempts(int count, Instant lastAttempt, Instant lockoutUntil) {}
    
    // ========================================
    // SESSION MANAGEMENT
    // ========================================
    
    public Session createSession(String userId, String refreshToken, 
                                  String deviceInfo, String ipAddress) {
        String sessionId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofDays(SESSION_TTL_DAYS));
        
        String hashedToken = hashToken(refreshToken);
        
        Session session = new Session(
                sessionId,
                userId,
                hashedToken,
                deviceInfo,
                ipAddress,
                now,
                now,
                expiresAt
        );
        
        // Store session
        sessions.put(sessionId, session);
        
        // Add to user's session set
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        
        // Map refresh token to session
        refreshTokenMap.put(hashedToken, sessionId);
        
        log.debug("Created session {} for user {}", sessionId, userId);
        return session;
    }
    
    public Optional<Session> getSession(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session != null && session.expiresAt().isAfter(Instant.now())) {
            return Optional.of(session);
        }
        return Optional.empty();
    }
    
    public Optional<Session> getSessionByRefreshToken(String refreshToken) {
        String hashedToken = hashToken(refreshToken);
        String sessionId = refreshTokenMap.get(hashedToken);
        if (sessionId != null) {
            return getSession(sessionId);
        }
        return Optional.empty();
    }
    
    public void touchSession(String sessionId) {
        Session existing = sessions.get(sessionId);
        if (existing != null) {
            Session updated = new Session(
                    existing.sessionId(),
                    existing.userId(),
                    existing.refreshTokenHash(),
                    existing.deviceInfo(),
                    existing.ipAddress(),
                    existing.createdAt(),
                    Instant.now(),
                    existing.expiresAt()
            );
            sessions.put(sessionId, updated);
        }
    }
    
    public void invalidateSession(String sessionId) {
        Session session = sessions.remove(sessionId);
        if (session != null) {
            refreshTokenMap.remove(session.refreshTokenHash());
            Set<String> userSessionSet = userSessions.get(session.userId());
            if (userSessionSet != null) {
                userSessionSet.remove(sessionId);
            }
            log.debug("Invalidated session {}", sessionId);
        }
    }
    
    public void invalidateAllUserSessions(String userId) {
        Set<String> sessionIds = userSessions.remove(userId);
        if (sessionIds != null) {
            for (String sessionId : sessionIds) {
                Session session = sessions.remove(sessionId);
                if (session != null) {
                    refreshTokenMap.remove(session.refreshTokenHash());
                }
            }
            log.debug("Invalidated all sessions for user {}", userId);
        }
    }
    
    public Set<String> getUserActiveSessions(String userId) {
        return userSessions.getOrDefault(userId, Collections.emptySet());
    }
    
    // ========================================
    // TOKEN BLACKLIST
    // ========================================
    
    public void blacklistToken(String tokenId, Instant expiresAt) {
        blacklistedTokens.add(tokenId);
        log.debug("Blacklisted token {}", tokenId);
    }
    
    public boolean isTokenBlacklisted(String tokenId) {
        return blacklistedTokens.contains(tokenId);
    }
    
    // ========================================
    // LOGIN ATTEMPTS
    // ========================================
    
    public void recordLoginAttempt(String email, boolean success) {
        if (success) {
            loginAttempts.remove(email);
            return;
        }
        
        LoginAttempts current = loginAttempts.get(email);
        Instant now = Instant.now();
        
        if (current == null || current.lockoutUntil() != null && current.lockoutUntil().isBefore(now)) {
            loginAttempts.put(email, new LoginAttempts(1, now, null));
        } else {
            int newCount = current.count() + 1;
            Instant lockout = newCount >= MAX_LOGIN_ATTEMPTS 
                    ? now.plus(Duration.ofMinutes(LOGIN_LOCKOUT_MINUTES)) 
                    : null;
            loginAttempts.put(email, new LoginAttempts(newCount, now, lockout));
        }
    }
    
    public boolean isAccountLocked(String email) {
        LoginAttempts attempts = loginAttempts.get(email);
        if (attempts == null) return false;
        if (attempts.lockoutUntil() == null) return false;
        if (attempts.lockoutUntil().isBefore(Instant.now())) {
            loginAttempts.remove(email);
            return false;
        }
        return true;
    }
    
    public int getRemainingAttempts(String email) {
        LoginAttempts attempts = loginAttempts.get(email);
        if (attempts == null) return MAX_LOGIN_ATTEMPTS;
        return Math.max(0, MAX_LOGIN_ATTEMPTS - attempts.count());
    }
    
    // ========================================
    // UTILITIES
    // ========================================
    
    private String hashToken(String token) {
        // Simple hash for local development - not secure for production
        return String.valueOf(token.hashCode());
    }
}
