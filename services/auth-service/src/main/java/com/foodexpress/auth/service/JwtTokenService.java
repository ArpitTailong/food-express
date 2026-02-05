package com.foodexpress.auth.service;

import com.foodexpress.auth.domain.AuthDomain.*;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * JWT Token Service for generating and validating tokens.
 * Uses JJWT library with HMAC-SHA256 signing.
 */
@Service
public class JwtTokenService {
    
    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);
    
    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;
    private final String issuer;
    
    public JwtTokenService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms:900000}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs,
            @Value("${jwt.issuer:foodexpress}") String issuer) {
        
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.issuer = issuer;
    }
    
    /**
     * Generate a token pair (access + refresh)
     */
    public TokenPair generateTokenPair(UserCredentials user) {
        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);
        
        return TokenPair.of(
                accessToken,
                refreshToken,
                accessTokenExpirationMs / 1000,
                refreshTokenExpirationMs / 1000
        );
    }
    
    /**
     * Generate access token with user claims
     */
    public String generateAccessToken(UserCredentials user) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(accessTokenExpirationMs);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.email());
        claims.put("roles", user.roles().stream().map(Role::name).toList());
        claims.put("permissions", user.permissions().stream().map(Permission::name).toList());
        claims.put("token_type", TokenType.ACCESS.name());
        
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.userId())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claims(claims)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }
    
    /**
     * Generate refresh token (minimal claims)
     */
    public String generateRefreshToken(UserCredentials user) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(refreshTokenExpirationMs);
        
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.userId())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim("token_type", TokenType.REFRESH.name())
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }
    
    /**
     * Generate service-to-service token
     */
    public String generateServiceToken(String serviceId, String targetService) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(300); // 5 minutes
        
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(serviceId)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim("token_type", TokenType.SERVICE.name())
                .claim("target_service", targetService)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }
    
    /**
     * Validate and parse token
     */
    public Optional<TokenClaims> validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return Optional.of(extractClaims(claims));
            
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            return Optional.empty();
        } catch (JwtException e) {
            log.warn("Invalid token: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Validate token and return claims (throws exception if invalid)
     */
    public TokenClaims validateTokenOrThrow(String token) {
        return validateToken(token)
                .orElseThrow(() -> new JwtException("Invalid or expired token"));
    }
    
    /**
     * Extract user ID from token without full validation
     * (useful for checking blacklist before full validation)
     */
    public Optional<String> extractSubjectUnsafe(String token) {
        try {
            // Parse without signature verification
            String[] parts = token.split("\\.");
            if (parts.length != 3) return Optional.empty();
            
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            // Simple extraction - in production use proper JSON parsing
            int subStart = payload.indexOf("\"sub\":\"") + 7;
            int subEnd = payload.indexOf("\"", subStart);
            
            return Optional.of(payload.substring(subStart, subEnd));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Get token expiration without validation
     */
    public Optional<Instant> extractExpirationUnsafe(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return Optional.of(claims.getExpiration().toInstant());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    private TokenClaims extractClaims(Claims claims) {
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        
        @SuppressWarnings("unchecked")
        List<String> permissions = claims.get("permissions", List.class);
        
        String tokenTypeStr = claims.get("token_type", String.class);
        TokenType tokenType = tokenTypeStr != null 
                ? TokenType.valueOf(tokenTypeStr) 
                : TokenType.ACCESS;
        
        return new TokenClaims(
                claims.getSubject(),
                claims.get("email", String.class),
                claims.get("firstName", String.class),
                claims.get("lastName", String.class),
                roles != null ? Set.copyOf(roles) : Set.of(),
                permissions != null ? Set.copyOf(permissions) : Set.of(),
                claims.get("tenantId", String.class),
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant(),
                claims.getId(),
                tokenType
        );
    }
}
