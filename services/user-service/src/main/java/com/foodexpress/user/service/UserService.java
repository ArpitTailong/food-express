package com.foodexpress.user.service;

import com.foodexpress.user.domain.*;
import com.foodexpress.user.dto.UserDTOs;
import com.foodexpress.user.dto.UserDTOs.*;
import com.foodexpress.user.repository.AddressRepository;
import com.foodexpress.user.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User Profile Service.
 * Handles all user-related business logic.
 */
@Service
public class UserService {
    
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final int MAX_ADDRESSES_PER_USER = 10;
    
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    
    private final Counter usersCreated;
    private final Counter usersDeleted;
    
    public UserService(
            UserRepository userRepository,
            AddressRepository addressRepository,
            MeterRegistry meterRegistry) {
        
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        
        this.usersCreated = Counter.builder("users.created").register(meterRegistry);
        this.usersDeleted = Counter.builder("users.deleted").register(meterRegistry);
    }
    
    // ========================================
    // USER CRUD
    // ========================================
    
    @Transactional
    public UserResponse createUser(String userId, CreateUserRequest request) {
        log.info("Creating user {} with email {}", userId, request.email());
        
        // Validate email uniqueness
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        
        // Validate phone uniqueness if provided
        if (request.phoneNumber() != null && userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new DuplicatePhoneException(request.phoneNumber());
        }
        
        // Create user
        UserProfile user = new UserProfile(
                userId,
                request.email(),
                request.firstName(),
                request.lastName(),
                request.role()
        );
        
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber());
        }
        
        // Add default address if provided
        if (request.defaultAddress() != null) {
            Address address = UserDTOs.toAddress(request.defaultAddress());
            address.setDefault(true);
            user.addAddress(address);
        }
        
        user = userRepository.save(user);
        usersCreated.increment();
        
        log.info("User {} created successfully", userId);
        
        return UserDTOs.toResponse(user);
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#userId")
    public Optional<UserResponse> getUser(String userId) {
        return userRepository.findById(userId)
                .filter(u -> u.getStatus() != UserStatus.DELETED)
                .map(UserDTOs::toResponse);
    }
    
    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .filter(u -> u.getStatus() != UserStatus.DELETED)
                .map(UserDTOs::toResponse);
    }
    
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public UserResponse updateUser(String userId, UpdateUserRequest request) {
        log.info("Updating user {}", userId);
        
        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        if (user.getStatus() == UserStatus.DELETED) {
            throw new UserNotFoundException(userId);
        }
        
        // Update fields if provided
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.phoneNumber() != null) {
            // Check uniqueness
            userRepository.findByPhoneNumber(request.phoneNumber())
                    .filter(u -> !u.getId().equals(userId))
                    .ifPresent(u -> { throw new DuplicatePhoneException(request.phoneNumber()); });
            user.setPhoneNumber(request.phoneNumber());
        }
        if (request.profilePictureUrl() != null) {
            user.setProfilePictureUrl(request.profilePictureUrl());
        }
        
        user = userRepository.save(user);
        
        return UserDTOs.toResponse(user);
    }
    
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void deleteUser(String userId) {
        log.info("Soft deleting user {}", userId);
        
        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        user.softDelete();
        userRepository.save(user);
        usersDeleted.increment();
        
        log.info("User {} soft deleted", userId);
    }
    
    // ========================================
    // USER STATUS
    // ========================================
    
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public UserResponse activateUser(String userId) {
        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        user.activate();
        user = userRepository.save(user);
        
        return UserDTOs.toResponse(user);
    }
    
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public UserResponse suspendUser(String userId, String reason) {
        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        user.suspend(reason);
        user = userRepository.save(user);
        
        return UserDTOs.toResponse(user);
    }
    
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void verifyEmail(String userId) {
        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        user.verifyEmail();
        userRepository.save(user);
    }
    
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void verifyPhone(String userId) {
        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        user.verifyPhone();
        userRepository.save(user);
    }
    
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void recordLogin(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.recordLogin();
            userRepository.save(user);
        });
    }
    
    // ========================================
    // ADDRESSES
    // ========================================
    
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public AddressResponse addAddress(String userId, AddressRequest request) {
        log.info("Adding address for user {}", userId);
        
        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        // Check limit
        if (user.getAddresses().size() >= MAX_ADDRESSES_PER_USER) {
            throw new AddressLimitExceededException(MAX_ADDRESSES_PER_USER);
        }
        
        Address address = UserDTOs.toAddress(request);
        
        // If this is set as default, unset existing default
        if (request.isDefault()) {
            user.getAddresses().forEach(a -> a.setDefault(false));
        }
        
        user.addAddress(address);
        userRepository.save(user);
        
        return UserDTOs.toAddressResponse(address);
    }
    
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public AddressResponse updateAddress(String userId, String addressId, AddressRequest request) {
        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        Address address = user.getAddresses().stream()
                .filter(a -> a.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new AddressNotFoundException(addressId));
        
        // Update fields
        address.setLabel(request.label());
        address.setAddressLine1(request.addressLine1());
        address.setAddressLine2(request.addressLine2());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country() != null ? request.country() : "India");
        address.setDeliveryInstructions(request.deliveryInstructions());
        address.setContactPhone(request.contactPhone());
        
        if (request.latitude() != null && request.longitude() != null) {
            address.setCoordinates(request.latitude(), request.longitude());
        }
        
        if (request.isDefault() && !address.isDefault()) {
            user.setDefaultAddress(addressId);
        }
        
        userRepository.save(user);
        
        return UserDTOs.toAddressResponse(address);
    }
    
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void deleteAddress(String userId, String addressId) {
        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        user.removeAddress(addressId);
        userRepository.save(user);
    }
    
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void setDefaultAddress(String userId, String addressId) {
        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        user.setDefaultAddress(addressId);
        userRepository.save(user);
    }
    
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(String userId) {
        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        return user.getAddresses().stream()
                .map(UserDTOs::toAddressResponse)
                .toList();
    }
    
    // ========================================
    // FAVORITES
    // ========================================
    
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void addFavoriteRestaurant(String userId, String restaurantId) {
        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        user.addFavoriteRestaurant(restaurantId);
        userRepository.save(user);
    }
    
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void removeFavoriteRestaurant(String userId, String restaurantId) {
        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        user.removeFavoriteRestaurant(restaurantId);
        userRepository.save(user);
    }
    
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void addFavoriteMenuItem(String userId, String menuItemId) {
        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        user.addFavoriteMenuItem(menuItemId);
        userRepository.save(user);
    }
    
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void removeFavoriteMenuItem(String userId, String menuItemId) {
        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        user.removeFavoriteMenuItem(menuItemId);
        userRepository.save(user);
    }
    
    @Transactional(readOnly = true)
    public FavoritesResponse getUserFavorites(String userId) {
        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        return UserDTOs.toFavoritesResponse(user);
    }
    
    // ========================================
    // DRIVER OPERATIONS
    // ========================================
    
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public DriverResponse updateDriverInfo(String userId, UpdateDriverInfoRequest request) {
        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        if (user.getRole() != UserRole.DRIVER) {
            throw new InvalidOperationException("User is not a driver");
        }
        
        user.setVehicleType(request.vehicleType());
        user.setVehicleNumber(request.vehicleNumber());
        user.setLicenseNumber(request.licenseNumber());
        
        user = userRepository.save(user);
        
        return UserDTOs.toDriverResponse(user);
    }
    
    @Transactional
    @CacheEvict(value = "users", key = "#driverId")
    public DriverResponse setDriverAvailability(String driverId, boolean available) {
        UserProfile user = userRepository.findById(driverId)
                .orElseThrow(() -> new UserNotFoundException(driverId));
        
        user.setAvailability(available);
        user = userRepository.save(user);
        
        return UserDTOs.toDriverResponse(user);
    }
    
    @Transactional(readOnly = true)
    public List<DriverResponse> getAvailableDrivers() {
        return userRepository.findAvailableDrivers().stream()
                .map(UserDTOs::toDriverResponse)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<DriverResponse> getAvailableDriversByRating() {
        return userRepository.findAvailableDriversByRating().stream()
                .map(UserDTOs::toDriverResponse)
                .toList();
    }
    
    // ========================================
    // RATINGS
    // ========================================
    
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void updateRating(String userId, int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        
        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        user.updateRating(rating);
        userRepository.save(user);
    }
    
    // ========================================
    // ADMIN OPERATIONS
    // ========================================
    
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAllActive(pageable)
                .map(UserDTOs::toResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersByRole(UserRole role, Pageable pageable) {
        return userRepository.findActiveByRole(role, pageable)
                .map(UserDTOs::toResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String query, Pageable pageable) {
        return userRepository.searchUsers(query, pageable)
                .map(UserDTOs::toResponse);
    }
    
    // ========================================
    // EXCEPTIONS
    // ========================================
    
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String userId) {
            super("User not found: " + userId);
        }
    }
    
    public static class DuplicateEmailException extends RuntimeException {
        public DuplicateEmailException(String email) {
            super("Email already registered: " + email);
        }
    }
    
    public static class DuplicatePhoneException extends RuntimeException {
        public DuplicatePhoneException(String phone) {
            super("Phone number already registered: " + phone);
        }
    }
    
    public static class AddressNotFoundException extends RuntimeException {
        public AddressNotFoundException(String addressId) {
            super("Address not found: " + addressId);
        }
    }
    
    public static class AddressLimitExceededException extends RuntimeException {
        public AddressLimitExceededException(int limit) {
            super("Maximum address limit reached: " + limit);
        }
    }
    
    public static class InvalidOperationException extends RuntimeException {
        public InvalidOperationException(String message) {
            super(message);
        }
    }
}
