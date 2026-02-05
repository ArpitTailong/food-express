package com.foodexpress.auth.service;

import com.foodexpress.auth.domain.AuthDomain.*;
import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based session and token management.
 * Handles:
 * - Session storage
 * - Token blacklisting
 * - Refresh token tracking
 * - Rate limiting for login attempts
 * 
 * Active only on non-local profiles (requires Redis).
 */
@Service
@Profile("!local")
public class TokenStorageService implements TokenStorage {
    
    private static final Logger log = LoggerFactory.getLogger(TokenStorageService.class);
    
    private static final String SESSION_PREFIX = "auth:session:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String USER_SESSIONS_PREFIX = "auth:user-sessions:";
    private static final String LOGIN_ATTEMPTS_PREFIX = "auth:login-attempts:";
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";
    
    private static final long SESSION_TTL_DAYS = 7;
    private static final long BLACKLIST_TTL_HOURS = 24;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOGIN_LOCKOUT_MINUTES = 30;
    
    private final RedissonClient redisson;
    
    public TokenStorageService(RedissonClient redisson) {
        this.redisson = redisson;
    }
    
    // ========================================
    // SESSION MANAGEMENT
    // ========================================
    
    /**
     * Create a new session
     */
    public Session createSession(String userId, String refreshToken, 
                                  String deviceInfo, String ipAddress) {
        String sessionId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofDays(SESSION_TTL_DAYS));
        
        Session session = new Session(
                sessionId,
                userId,
                hashToken(refreshToken),
                deviceInfo,
                ipAddress,
                now,
                now,
                expiresAt
        );
        
        // Store session
        RBucket<Session> bucket = redisson.getBucket(SESSION_PREFIX + sessionId);
        bucket.set(session, SESSION_TTL_DAYS, TimeUnit.DAYS);
        
        // Add to user's session set
        RSet<String> userSessions = redisson.getSet(USER_SESSIONS_PREFIX + userId);
        userSessions.add(sessionId);
        userSessions.expire(Duration.ofDays(SESSION_TTL_DAYS));
        
        // Store refresh token mapping
        RBucket<String> refreshBucket = redisson.getBucket(REFRESH_TOKEN_PREFIX + hashToken(refreshToken));
        refreshBucket.set(sessionId, SESSION_TTL_DAYS, TimeUnit.DAYS);
        
        log.info("Session created for user: {} sessionId: {}", userId, sessionId);
        
        return session;
    }
    
    /**
     * Get session by ID
     */
    public Optional<Session> getSession(String sessionId) {
        RBucket<Session> bucket = redisson.getBucket(SESSION_PREFIX + sessionId);
        Session session = bucket.get();
        return Optional.ofNullable(session);
    }
    
    /**
     * Get session by refresh token
     */
    public Optional<Session> getSessionByRefreshToken(String refreshToken) {
        RBucket<String> refreshBucket = redisson.getBucket(REFRESH_TOKEN_PREFIX + hashToken(refreshToken));
        String sessionId = refreshBucket.get();
        
        if (sessionId == null) {
            return Optional.empty();
        }
        
        return getSession(sessionId);
    }
    
    /**
     * Update session last accessed time
     */
    public void touchSession(String sessionId) {
        getSession(sessionId).ifPresent(session -> {
            Session updated = new Session(
                    session.sessionId(),
                    session.userId(),
                    session.refreshTokenHash(),
                    session.deviceInfo(),
                    session.ipAddress(),
                    session.createdAt(),
                    Instant.now(),
                    session.expiresAt()
            );
            
            RBucket<Session> bucket = redisson.getBucket(SESSION_PREFIX + sessionId);
            bucket.set(updated, SESSION_TTL_DAYS, TimeUnit.DAYS);
        });
    }
    
    /**
     * Invalidate a specific session
     */
    public void invalidateSession(String sessionId) {
        getSession(sessionId).ifPresent(session -> {
            // Remove session
            redisson.getBucket(SESSION_PREFIX + sessionId).delete();
            
            // Remove from user's session set
            RSet<String> userSessions = redisson.getSet(USER_SESSIONS_PREFIX + session.userId());
            userSessions.remove(sessionId);
            
            // Remove refresh token mapping
            redisson.getBucket(REFRESH_TOKEN_PREFIX + session.refreshTokenHash()).delete();
            
            log.info("Session invalidated: {}", sessionId);
        });
    }
    
    /**
     * Invalidate all sessions for a user (logout from all devices)
     */
    public void invalidateAllUserSessions(String userId) {
        RSet<String> userSessions = redisson.getSet(USER_SESSIONS_PREFIX + userId);
        Set<String> sessionIds = userSessions.readAll();
        
        for (String sessionId : sessionIds) {
            invalidateSession(sessionId);
        }
        
        userSessions.delete();
        log.info("All sessions invalidated for user: {}", userId);
    }
    
    /**
     * Get all active sessions for a user
     */
    public Set<String> getUserActiveSessions(String userId) {
        RSet<String> userSessions = redisson.getSet(USER_SESSIONS_PREFIX + userId);
        return userSessions.readAll();
    }
    
    // ========================================
    // TOKEN BLACKLISTING
    // ========================================
    
    /**
     * Blacklist a token (e.g., on logout or password change)
     */
    public void blacklistToken(String tokenId, Instant expiresAt) {
        long ttlSeconds = Duration.between(Instant.now(), expiresAt).getSeconds();
        if (ttlSeconds <= 0) {
            return; // Token already expired
        }
        
        RBucket<Boolean> bucket = redisson.getBucket(BLACKLIST_PREFIX + tokenId);
        bucket.set(true, ttlSeconds, TimeUnit.SECONDS);
        
        log.debug("Token blacklisted: {}", tokenId);
    }
    
    /**
     * Check if a token is blacklisted
     */
    public boolean isTokenBlacklisted(String tokenId) {
        RBucket<Boolean> bucket = redisson.getBucket(BLACKLIST_PREFIX + tokenId);
        return bucket.isExists();
    }
    
    // ========================================
    // LOGIN ATTEMPT TRACKING
    // ========================================
    
    /**
     * Record a login attempt
     */
    public void recordLoginAttempt(String identifier, boolean successful) {
        String key = LOGIN_ATTEMPTS_PREFIX + identifier;
        
        if (successful) {
            // Clear attempts on successful login
            redisson.getBucket(key).delete();
        } else {
            // Increment failed attempts
            RBucket<Integer> bucket = redisson.getBucket(key);
            Integer attempts = bucket.get();
            attempts = (attempts == null) ? 1 : attempts + 1;
            bucket.set(attempts, LOGIN_LOCKOUT_MINUTES, TimeUnit.MINUTES);
        }
    }
    
    /**
     * Check if account is locked due to too many failed attempts
     */
    public boolean isAccountLocked(String identifier) {
        String key = LOGIN_ATTEMPTS_PREFIX + identifier;
        RBucket<Integer> bucket = redisson.getBucket(key);
        Integer attempts = bucket.get();
        return attempts != null && attempts >= MAX_LOGIN_ATTEMPTS;
    }
    
    /**
     * Get remaining login attempts
     */
    public int getRemainingAttempts(String identifier) {
        String key = LOGIN_ATTEMPTS_PREFIX + identifier;
        RBucket<Integer> bucket = redisson.getBucket(key);
        Integer attempts = bucket.get();
        return MAX_LOGIN_ATTEMPTS - (attempts != null ? attempts : 0);
    }
    
    // ========================================
    // UTILITIES
    // ========================================
    
    private String hashToken(String token) {
        // In production, use proper hashing (SHA-256)
        return Integer.toHexString(token.hashCode());
    }
}