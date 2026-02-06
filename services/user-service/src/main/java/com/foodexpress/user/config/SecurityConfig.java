package com.foodexpress.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Actuator and docs
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                
                // Public endpoints - registration and credential verification
                .requestMatchers(HttpMethod.POST, "/api/v1/users/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/users/credentials").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/users/*/credentials").permitAll()
                
                // User profile - authenticated
                .requestMatchers("/api/v1/users/me/**").authenticated()
                
                // Available drivers - any authenticated service
                .requestMatchers(HttpMethod.GET, "/api/v1/users/drivers/available").authenticated()
                
                // Get user by ID - any authenticated user
                .requestMatchers(HttpMethod.GET, "/api/v1/users/{userId}").authenticated()
                
                // Admin endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/users").hasRole("ADMIN")
                .requestMatchers("/api/v1/users/search").hasRole("ADMIN")
                .requestMatchers("/api/v1/users/*/activate").hasRole("ADMIN")
                .requestMatchers("/api/v1/users/*/suspend").hasRole("ADMIN")
                
                // Create user - any authenticated
                .requestMatchers(HttpMethod.POST, "/api/v1/users").authenticated()
                
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            // Allow H2 console frames
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        
        return http.build();
    }
    
    /**
     * JWT Decoder for local profile - uses shared secret with auth-service
     */
    @Bean
    @Profile("local")
    public JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {
        SecretKeySpec secretKey = new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8), 
            "HmacSHA256"
        );
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
    
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        
        return jwtAuthenticationConverter;
    }
}
