package com.foodexpress.user.controller;

import com.foodexpress.user.dto.UserDTOs.*;
import com.foodexpress.user.service.UserService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User Profile Management REST API.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User profile management APIs")
public class UserController {
    
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    // ========================================
    // PUBLIC ENDPOINTS (Registration & Credentials)
    // ========================================
    
    @PostMapping("/register")
    @Timed(value = "user.register", description = "Time taken to register user")
    @Operation(summary = "Register new user", description = "Public endpoint for user registration")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "409", description = "Email or phone already exists")
    })
    public ResponseEntity<UserResponse> registerUser(
            @Valid @RequestBody RegisterUserRequest request) {
        
        log.info("Registering new user with email {}", request.email());
        
        UserResponse response = userService.registerUser(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/credentials")
    @Timed(value = "user.credentials", description = "Time taken to fetch credentials")
    @Operation(summary = "Get user credentials by email", description = "Internal endpoint for auth-service to verify user credentials")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Credentials found"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserCredentialsResponse> getUserCredentials(
            @Valid @RequestBody VerifyCredentialsRequest request) {
        
        log.debug("Fetching credentials for email {}", request.email());
        
        return userService.getUserCredentialsByEmail(request.email())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{userId}/credentials")
    @Timed(value = "user.credentials.byId", description = "Time taken to fetch credentials by ID")
    @Operation(summary = "Get user credentials by ID", description = "Internal endpoint for auth-service to verify user credentials")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Credentials found"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserCredentialsResponse> getUserCredentialsById(
            @PathVariable String userId) {
        
        log.debug("Fetching credentials for userId {}", userId);
        
        return userService.getUserCredentialsById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // ========================================
    // PROFILE ENDPOINTS
    // ========================================
    
    @PostMapping
    @Timed(value = "user.create", description = "Time taken to create user")
    @Operation(summary = "Create user profile", description = "Creates a new user profile after authentication")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "409", description = "Email or phone already exists")
    })
    public ResponseEntity<UserResponse> createUser(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateUserRequest request) {
        
        String userId = jwt.getSubject();
        log.info("Creating profile for user {}", userId);
        
        UserResponse response = userService.createUser(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/me")
    @Timed(value = "user.get.me", description = "Time taken to get current user")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        
        return userService.getUser(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{userId}")
    @Timed(value = "user.get", description = "Time taken to get user")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserResponse> getUser(@PathVariable String userId) {
        return userService.getUser(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/me")
    @Timed(value = "user.update", description = "Time taken to update user")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserRequest request) {
        
        String userId = jwt.getSubject();
        
        UserResponse response = userService.updateUser(userId, request);
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/me")
    @Operation(summary = "Delete current user account", description = "Soft deletes the user with GDPR compliance")
    public ResponseEntity<Void> deleteCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        
        userService.deleteUser(userId);
        
        return ResponseEntity.noContent().build();
    }
    
    // ========================================
    // ADDRESS ENDPOINTS
    // ========================================
    
    @GetMapping("/me/addresses")
    @Operation(summary = "Get user addresses")
    public ResponseEntity<List<AddressResponse>> getMyAddresses(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        
        List<AddressResponse> addresses = userService.getUserAddresses(userId);
        
        return ResponseEntity.ok(addresses);
    }
    
    @PostMapping("/me/addresses")
    @Operation(summary = "Add new address")
    public ResponseEntity<AddressResponse> addAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddressRequest request) {
        
        String userId = jwt.getSubject();
        
        AddressResponse response = userService.addAddress(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/me/addresses/{addressId}")
    @Operation(summary = "Update address")
    public ResponseEntity<AddressResponse> updateAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String addressId,
            @Valid @RequestBody AddressRequest request) {
        
        String userId = jwt.getSubject();
        
        AddressResponse response = userService.updateAddress(userId, addressId, request);
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/me/addresses/{addressId}")
    @Operation(summary = "Delete address")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String addressId) {
        
        String userId = jwt.getSubject();
        
        userService.deleteAddress(userId, addressId);
        
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/me/addresses/{addressId}/default")
    @Operation(summary = "Set default address")
    public ResponseEntity<Void> setDefaultAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String addressId) {
        
        String userId = jwt.getSubject();
        
        userService.setDefaultAddress(userId, addressId);
        
        return ResponseEntity.ok().build();
    }
    
    // ========================================
    // FAVORITES ENDPOINTS
    // ========================================
    
    @GetMapping("/me/favorites")
    @Operation(summary = "Get user favorites")
    public ResponseEntity<FavoritesResponse> getMyFavorites(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        
        FavoritesResponse response = userService.getUserFavorites(userId);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/me/favorites/restaurants/{restaurantId}")
    @Operation(summary = "Add favorite restaurant")
    public ResponseEntity<Void> addFavoriteRestaurant(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String restaurantId) {
        
        String userId = jwt.getSubject();
        
        userService.addFavoriteRestaurant(userId, restaurantId);
        
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/me/favorites/restaurants/{restaurantId}")
    @Operation(summary = "Remove favorite restaurant")
    public ResponseEntity<Void> removeFavoriteRestaurant(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String restaurantId) {
        
        String userId = jwt.getSubject();
        
        userService.removeFavoriteRestaurant(userId, restaurantId);
        
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/me/favorites/items/{itemId}")
    @Operation(summary = "Add favorite menu item")
    public ResponseEntity<Void> addFavoriteMenuItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String itemId) {
        
        String userId = jwt.getSubject();
        
        userService.addFavoriteMenuItem(userId, itemId);
        
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/me/favorites/items/{itemId}")
    @Operation(summary = "Remove favorite menu item")
    public ResponseEntity<Void> removeFavoriteMenuItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String itemId) {
        
        String userId = jwt.getSubject();
        
        userService.removeFavoriteMenuItem(userId, itemId);
        
        return ResponseEntity.noContent().build();
    }
    
    // ========================================
    // DRIVER ENDPOINTS
    // ========================================
    
    @GetMapping("/drivers/available")
    @Operation(summary = "Get available drivers")
    public ResponseEntity<List<DriverResponse>> getAvailableDrivers() {
        List<DriverResponse> drivers = userService.getAvailableDriversByRating();
        
        return ResponseEntity.ok(drivers);
    }
    
    @PutMapping("/me/driver-info")
    @Operation(summary = "Update driver information")
    public ResponseEntity<DriverResponse> updateDriverInfo(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateDriverInfoRequest request) {
        
        String userId = jwt.getSubject();
        
        DriverResponse response = userService.updateDriverInfo(userId, request);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/me/availability")
    @Operation(summary = "Set driver availability")
    public ResponseEntity<DriverResponse> setAvailability(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam boolean available) {
        
        String userId = jwt.getSubject();
        
        DriverResponse response = userService.setDriverAvailability(userId, available);
        
        return ResponseEntity.ok(response);
    }
    
    // ========================================
    // ADMIN ENDPOINTS
    // ========================================
    
    @GetMapping
    @Operation(summary = "List all users (admin)")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<UserResponse> users = userService.getAllUsers(pageable);
        
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search users (admin)")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<UserResponse> users = userService.searchUsers(q, pageable);
        
        return ResponseEntity.ok(users);
    }
    
    @PostMapping("/{userId}/activate")
    @Operation(summary = "Activate user (admin)")
    public ResponseEntity<UserResponse> activateUser(@PathVariable String userId) {
        UserResponse response = userService.activateUser(userId);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{userId}/suspend")
    @Operation(summary = "Suspend user (admin)")
    public ResponseEntity<UserResponse> suspendUser(
            @PathVariable String userId,
            @RequestParam(required = false) String reason) {
        
        UserResponse response = userService.suspendUser(userId, reason);
        
        return ResponseEntity.ok(response);
    }
}
