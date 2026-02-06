package com.foodexpress.user.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * User Profile Entity - Aggregate Root.
 * 
 * Represents all types of users in the system:
 * - Customers who order food
 * - Drivers who deliver orders
 * - Restaurant owners who manage restaurants
 * - Admins who manage the platform
 */
@Entity
@Table(name = "users")
public class UserProfile {
    
    @Id
    private String id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(name = "password_hash")
    private String passwordHash;
    
    @Column(name = "phone_number", unique = true)
    private String phoneNumber;
    
    @Column(name = "first_name", nullable = false)
    private String firstName;
    
    @Column(name = "last_name", nullable = false)
    private String lastName;
    
    @Column(name = "display_name")
    private String displayName;
    
    @Column(name = "profile_picture_url")
    private String profilePictureUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;
    
    // Preferences stored as JSON
    @Column(columnDefinition = "jsonb")
    private String preferences;
    
    // Addresses
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("isDefault DESC, createdAt DESC")
    private List<Address> addresses = new ArrayList<>();
    
    // Favorites
    @ElementCollection
    @CollectionTable(name = "favorite_restaurants", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "restaurant_id")
    private Set<String> favoriteRestaurants = new HashSet<>();
    
    @ElementCollection
    @CollectionTable(name = "favorite_items", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "menu_item_id")
    private Set<String> favoriteMenuItems = new HashSet<>();
    
    // Driver-specific fields
    @Column(name = "vehicle_type")
    private String vehicleType;
    
    @Column(name = "vehicle_number")
    private String vehicleNumber;
    
    @Column(name = "license_number")
    private String licenseNumber;
    
    @Column(name = "is_available")
    private Boolean isAvailable;
    
    // Ratings
    @Column(name = "average_rating", precision = 3)
    private Double averageRating;
    
    @Column(name = "total_ratings")
    private Integer totalRatings;
    
    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;
    
    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @Version
    private Integer version;
    
    protected UserProfile() {}
    
    public UserProfile(String id, String email, String firstName, String lastName, UserRole role) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.status = UserStatus.PENDING_VERIFICATION;
        this.displayName = firstName + " " + lastName;
        this.averageRating = 0.0;
        this.totalRatings = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        if (role == UserRole.DRIVER) {
            this.isAvailable = false;
        }
    }
    
    // ========================================
    // BUSINESS METHODS
    // ========================================
    
    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void suspend(String reason) {
        this.status = UserStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void softDelete() {
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        // Anonymize for GDPR
        this.email = "deleted_" + this.id + "@deleted.local";
        this.phoneNumber = null;
        this.profilePictureUrl = null;
    }
    
    public void verifyEmail() {
        this.emailVerifiedAt = LocalDateTime.now();
        if (this.status == UserStatus.PENDING_VERIFICATION) {
            this.status = UserStatus.ACTIVE;
        }
        this.updatedAt = LocalDateTime.now();
    }
    
    public void verifyPhone() {
        this.phoneVerifiedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
    
    public void updateRating(int newRating) {
        double totalScore = (averageRating * totalRatings) + newRating;
        totalRatings++;
        averageRating = totalScore / totalRatings;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Address management
    public void addAddress(Address address) {
        address.setUser(this);
        if (addresses.isEmpty()) {
            address.setDefault(true);
        }
        addresses.add(address);
        this.updatedAt = LocalDateTime.now();
    }
    
    public void removeAddress(String addressId) {
        addresses.removeIf(a -> a.getId().equals(addressId));
        // If we removed the default, make the first one default
        if (addresses.stream().noneMatch(Address::isDefault) && !addresses.isEmpty()) {
            addresses.get(0).setDefault(true);
        }
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setDefaultAddress(String addressId) {
        addresses.forEach(a -> a.setDefault(a.getId().equals(addressId)));
        this.updatedAt = LocalDateTime.now();
    }
    
    public Optional<Address> getDefaultAddress() {
        return addresses.stream().filter(Address::isDefault).findFirst();
    }
    
    // Favorites
    public void addFavoriteRestaurant(String restaurantId) {
        favoriteRestaurants.add(restaurantId);
        this.updatedAt = LocalDateTime.now();
    }
    
    public void removeFavoriteRestaurant(String restaurantId) {
        favoriteRestaurants.remove(restaurantId);
        this.updatedAt = LocalDateTime.now();
    }
    
    public void addFavoriteMenuItem(String menuItemId) {
        favoriteMenuItems.add(menuItemId);
        this.updatedAt = LocalDateTime.now();
    }
    
    public void removeFavoriteMenuItem(String menuItemId) {
        favoriteMenuItems.remove(menuItemId);
        this.updatedAt = LocalDateTime.now();
    }
    
    // Driver methods
    public void setAvailability(boolean available) {
        if (role != UserRole.DRIVER) {
            throw new IllegalStateException("Only drivers can set availability");
        }
        this.isAvailable = available;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }
    
    public boolean isPhoneVerified() {
        return phoneVerifiedAt != null;
    }
    
    // ========================================
    // GETTERS AND SETTERS
    // ========================================
    
    public String getId() { return id; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { 
        this.email = email; 
        this.emailVerifiedAt = null; // Reset verification on email change
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { 
        this.passwordHash = passwordHash;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { 
        this.phoneNumber = phoneNumber;
        this.phoneVerifiedAt = null; // Reset verification on phone change
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { 
        this.firstName = firstName;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { 
        this.lastName = lastName;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { 
        this.displayName = displayName;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { 
        this.profilePictureUrl = profilePictureUrl;
        this.updatedAt = LocalDateTime.now();
    }
    
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }
    
    public String getPreferences() { return preferences; }
    public void setPreferences(String preferences) { 
        this.preferences = preferences;
        this.updatedAt = LocalDateTime.now();
    }
    
    public List<Address> getAddresses() { return Collections.unmodifiableList(addresses); }
    public Set<String> getFavoriteRestaurants() { return Collections.unmodifiableSet(favoriteRestaurants); }
    public Set<String> getFavoriteMenuItems() { return Collections.unmodifiableSet(favoriteMenuItems); }
    
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    
    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
    
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    
    public Boolean getIsAvailable() { return isAvailable; }
    public Double getAverageRating() { return averageRating; }
    public Integer getTotalRatings() { return totalRatings; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public LocalDateTime getEmailVerifiedAt() { return emailVerifiedAt; }
    public LocalDateTime getPhoneVerifiedAt() { return phoneVerifiedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public Integer getVersion() { return version; }
}
