package com.foodexpress.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for API Gateway.
 * Handles JWT validation and route-level authorization.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    
    // Paths that don't require authentication
    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/**",
            "/api/v1/public/**",
            "/actuator/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/webjars/**"
    };
    
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // Disable CSRF for API Gateway (stateless)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                
                // Configure CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // Authorization rules
                .authorizeExchange(exchange -> exchange
                        // Public endpoints
                        .pathMatchers(PUBLIC_PATHS).permitAll()
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        
                        // Admin endpoints
                        .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        
                        // User management
                        .pathMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                        .pathMatchers("/api/v1/users/**").hasAnyRole("ADMIN", "SUPPORT")
                        
                        // Order endpoints
                        .pathMatchers("/api/v1/orders/**").authenticated()
                        
                        // Payment endpoints (stricter)
                        .pathMatchers("/api/v1/payments/**").authenticated()
                        
                        // Restaurant endpoints
                        .pathMatchers(HttpMethod.GET, "/api/v1/restaurants/**").permitAll()
                        .pathMatchers("/api/v1/restaurants/**").hasAnyRole("RESTAURANT_OWNER", "ADMIN")
                        
                        // Everything else requires authentication
                        .anyExchange().authenticated()
                )
                
                // OAuth2 Resource Server with JWT
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {})
                )
                
                .build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:4200",
                "https://foodexpress.com"
        ));
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(Arrays.asList(
                "X-Correlation-ID",
                "X-Request-ID",
                "Authorization"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
