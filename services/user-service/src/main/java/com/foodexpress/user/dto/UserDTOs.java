package com.foodexpress.user.dto;

import com.foodexpress.user.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * User Service DTOs - Using Java 21 Records.
 */
public final class UserDTOs {
    
    private UserDTOs() {}
    
    // ========================================
    // REQUEST DTOs
    // ========================================
    
    public record CreateUserRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,
            
            @NotBlank(message = "First name is required")
            @Size(min = 1, max = 100)
            String firstName,
            
            @NotBlank(message = "Last name is required")
            @Size(min = 1, max = 100)
            String lastName,
            
            @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Invalid phone number")
            String phoneNumber,
            
            @NotNull(message = "Role is required")
            UserRole role,
            
            @Valid
            AddressRequest defaultAddress
    ) {}
    
    public record UpdateUserRequest(
            @Size(min = 1, max = 100)
            String firstName,
            
            @Size(min = 1, max = 100)
            String lastName,
            
            @Size(max = 200)
            String displayName,
            
            @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Invalid phone number")
            String phoneNumber,
            
            String profilePictureUrl
    ) {}
    
    public record UpdateDriverInfoRequest(
            @NotBlank
            String vehicleType,
            
            @NotBlank
            String vehicleNumber,
            
            @NotBlank
            String licenseNumber
    ) {}
    
    public record AddressRequest(
            @NotBlank(message = "Label is required")
            @Size(max = 50)
            String label,
            
            @NotBlank(message = "Address line 1 is required")
            @Size(max = 255)
            String addressLine1,
            
            @Size(max = 255)
            String addressLine2,
            
            @NotBlank(message = "City is required")
            @Size(max = 100)
            String city,
            
            @NotBlank(message = "State is required")
            @Size(max = 100)
            String state,
            
            @NotBlank(message = "Postal code is required")
            @Size(max = 20)
            String postalCode,
            
            @Size(max = 100)
            String country,
            
            BigDecimal latitude,
            BigDecimal longitude,
            
            @Size(max = 500)
            String deliveryInstructions,
            
            @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$")
            String contactPhone,
            
            boolean isDefault
    ) {}
    
    public record UpdatePreferencesRequest(
            List<String> dietaryRestrictions,
            boolean emailNotifications,
            boolean smsNotifications,
            boolean pushNotifications,
            boolean promotionalEmails,
            String preferredLanguage,
            String preferredCurrency
    ) {}
    
    public record FavoriteRequest(
            @NotBlank
            String itemId
    ) {}
    
    // ========================================
    // RESPONSE DTOs
    // ========================================
    
    public record UserResponse(
            String id,
            String email,
            String phoneNumber,
            String firstName,
            String lastName,
            String displayName,
            String profilePictureUrl,
            String role,
            String status,
            boolean emailVerified,
            boolean phoneVerified,
            Double averageRating,
            Integer totalRatings,
            List<AddressResponse> addresses,
            LocalDateTime createdAt,
            LocalDateTime lastLoginAt
    ) {}
    
    public record UserSummaryResponse(
            String id,
            String displayName,
            String profilePictureUrl,
            String role,
            Double averageRating
    ) {}
    
    public record DriverResponse(
            String id,
            String displayName,
            String profilePictureUrl,
            String phoneNumber,
            String vehicleType,
            String vehicleNumber,
            Boolean isAvailable,
            Double averageRating,
            Integer totalRatings
    ) {}
    
    public record AddressResponse(
            String id,
            String label,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country,
            BigDecimal latitude,
            BigDecimal longitude,
            String deliveryInstructions,
            String contactPhone,
            boolean isDefault,
            boolean isVerified,
            String formattedAddress
    ) {}
    
    public record FavoritesResponse(
            Set<String> restaurants,
            Set<String> menuItems
    ) {}
    
    public record PreferencesResponse(
            List<String> dietaryRestrictions,
            boolean emailNotifications,
            boolean smsNotifications,
            boolean pushNotifications,
            boolean promotionalEmails,
            String preferredLanguage,
            String preferredCurrency
    ) {}
    
    // ========================================
    // MAPPERS
    // ========================================
    
    public static UserResponse toResponse(UserProfile user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getFirstName(),
                user.getLastName(),
                user.getDisplayName(),
                user.getProfilePictureUrl(),
                user.getRole().name(),
                user.getStatus().name(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                user.getAverageRating(),
                user.getTotalRatings(),
                user.getAddresses().stream().map(UserDTOs::toAddressResponse).toList(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
    
    public static UserSummaryResponse toSummary(UserProfile user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getDisplayName(),
                user.getProfilePictureUrl(),
                user.getRole().name(),
                user.getAverageRating()
        );
    }
    
    public static DriverResponse toDriverResponse(UserProfile user) {
        return new DriverResponse(
                user.getId(),
                user.getDisplayName(),
                user.getProfilePictureUrl(),
                user.getPhoneNumber(),
                user.getVehicleType(),
                user.getVehicleNumber(),
                user.getIsAvailable(),
                user.getAverageRating(),
                user.getTotalRatings()
        );
    }
    
    public static AddressResponse toAddressResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getLabel(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.getLatitude(),
                address.getLongitude(),
                address.getDeliveryInstructions(),
                address.getContactPhone(),
                address.isDefault(),
                address.isVerified(),
                address.getFormattedAddress()
        );
    }
    
    public static Address toAddress(AddressRequest request) {
        Address address = new Address(
                request.label(),
                request.addressLine1(),
                request.city(),
                request.state(),
                request.postalCode(),
                request.country() != null ? request.country() : "India"
        );
        address.setAddressLine2(request.addressLine2());
        address.setDeliveryInstructions(request.deliveryInstructions());
        address.setContactPhone(request.contactPhone());
        address.setDefault(request.isDefault());
        
        if (request.latitude() != null && request.longitude() != null) {
            address.setCoordinates(request.latitude(), request.longitude());
        }
        
        return address;
    }
    
    public static FavoritesResponse toFavoritesResponse(UserProfile user) {
        return new FavoritesResponse(
                user.getFavoriteRestaurants(),
                user.getFavoriteMenuItems()
        );
    }
}
