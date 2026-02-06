package com.foodexpress.auth.client;

import com.foodexpress.auth.domain.AuthDomain.*;
import com.foodexpress.auth.dto.AuthDTOs.RegisterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Client for communicating with User Service.
 * Used to fetch user credentials for authentication.
 */
@Component
public class UserServiceClient {
    
    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);
    
    private final RestTemplate restTemplate;
    private final String userServiceUrl;
    
    public UserServiceClient(
            RestTemplate restTemplate,
            @Value("${user-service.url:http://localhost:8084}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }
    
    /**
     * Fetch user credentials from User Service by email.
     */
    public Optional<UserCredentials> getUserCredentialsByEmail(String email) {
        try {
            String url = userServiceUrl + "/api/v1/users/credentials";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, String> requestBody = Map.of("email", email);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<UserCredentialsDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    UserCredentialsDTO.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                UserCredentialsDTO dto = response.getBody();
                
                // Map role string to Role enum
                Role role = mapRole(dto.role());
                Set<Permission> permissions = getDefaultPermissions(role);
                
                return Optional.of(new UserCredentials(
                        dto.userId(),
                        dto.email(),
                        dto.passwordHash(),
                        Set.of(role),
                        permissions,
                        dto.enabled(),
                        dto.accountNonLocked(),
                        Instant.now(),
                        0
                ));
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Failed to fetch user credentials from user-service: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Register a new user through User Service.
     */
    public Optional<UserRegistrationResponse> registerUser(RegisterRequest request) {
        try {
            String url = userServiceUrl + "/api/v1/users/register";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, String> requestBody = Map.of(
                    "email", request.email(),
                    "password", request.password(),
                    "firstName", request.firstName(),
                    "lastName", request.lastName()
            );
            
            HttpEntity<Map<String, String>> httpRequest = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<UserRegistrationResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpRequest,
                    UserRegistrationResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Failed to register user through user-service: {}", e.getMessage());
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fetch user credentials from User Service by user ID.
     */
    public Optional<UserCredentials> getUserCredentialsById(String userId) {
        try {
            String url = userServiceUrl + "/api/v1/users/" + userId + "/credentials";
            
            ResponseEntity<UserCredentialsDTO> response = restTemplate.getForEntity(
                    url,
                    UserCredentialsDTO.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                UserCredentialsDTO dto = response.getBody();
                
                // Map role string to Role enum
                Role role = mapRole(dto.role());
                Set<Permission> permissions = getDefaultPermissions(role);
                
                return Optional.of(new UserCredentials(
                        dto.userId(),
                        dto.email(),
                        dto.passwordHash(),
                        Set.of(role),
                        permissions,
                        dto.enabled(),
                        dto.accountNonLocked(),
                        Instant.now(),
                        0
                ));
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Failed to fetch user credentials by ID from user-service: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    private Role mapRole(String roleStr) {
        return switch (roleStr.toUpperCase()) {
            case "ADMIN" -> Role.ADMIN;
            case "DELIVERY_PARTNER", "DRIVER" -> Role.DELIVERY_PARTNER;
            case "RESTAURANT_OWNER" -> Role.RESTAURANT_OWNER;
            case "SUPPORT" -> Role.SUPPORT;
            default -> Role.CUSTOMER;
        };
    }
    
    private Set<Permission> getDefaultPermissions(Role role) {
        return switch (role) {
            case ADMIN -> Set.of(Permission.ADMIN_ACCESS, Permission.ORDER_READ_ALL, Permission.USER_MANAGE_ALL);
            case RESTAURANT_OWNER -> Set.of(Permission.RESTAURANT_CREATE, Permission.RESTAURANT_UPDATE, Permission.ORDER_READ_ALL);
            case DELIVERY_PARTNER -> Set.of(Permission.ORDER_READ, Permission.ORDER_UPDATE);
            case SUPPORT -> Set.of(Permission.ORDER_READ_ALL, Permission.USER_READ);
            case CUSTOMER -> Set.of(Permission.ORDER_CREATE, Permission.ORDER_READ, Permission.PAYMENT_INITIATE);
        };
    }
    
    // DTOs for User Service responses
    public record UserCredentialsDTO(
            String userId,
            String email,
            String passwordHash,
            String role,
            String status,
            boolean enabled,
            boolean accountNonLocked
    ) {}
    
    public record UserRegistrationResponse(
            String id,
            String email,
            String firstName,
            String lastName,
            String role,
            String status
    ) {}
}
