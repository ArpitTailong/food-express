package com.foodexpress.user.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Delivery Address Entity.
 * 
 * Supports multiple addresses per user with:
 * - Default address selection
 * - Address labels (Home, Work, etc.)
 * - Geocoding support (lat/lng)
 * - Delivery instructions
 */
@Entity
@Table(name = "addresses")
public class Address {
    
    @Id
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile user;
    
    @Column(nullable = false)
    private String label; // Home, Work, etc.
    
    @Column(name = "address_line1", nullable = false)
    private String addressLine1;
    
    @Column(name = "address_line2")
    private String addressLine2;
    
    @Column(nullable = false)
    private String city;
    
    @Column(nullable = false)
    private String state;
    
    @Column(name = "postal_code", nullable = false)
    private String postalCode;
    
    @Column(nullable = false)
    private String country;
    
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;
    
    @Column(name = "delivery_instructions")
    private String deliveryInstructions;
    
    @Column(name = "contact_phone")
    private String contactPhone;
    
    @Column(name = "is_default")
    private boolean isDefault;
    
    @Column(name = "is_verified")
    private boolean isVerified;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    protected Address() {}
    
    public Address(String label, String addressLine1, String city, String state, String postalCode, String country) {
        this.id = UUID.randomUUID().toString();
        this.label = label;
        this.addressLine1 = addressLine1;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.isDefault = false;
        this.isVerified = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setCoordinates(BigDecimal latitude, BigDecimal longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.isVerified = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(addressLine1);
        if (addressLine2 != null && !addressLine2.isEmpty()) {
            sb.append(", ").append(addressLine2);
        }
        sb.append(", ").append(city);
        sb.append(", ").append(state);
        sb.append(" ").append(postalCode);
        sb.append(", ").append(country);
        return sb.toString();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    
    public UserProfile getUser() { return user; }
    void setUser(UserProfile user) { this.user = user; }
    
    public String getLabel() { return label; }
    public void setLabel(String label) { 
        this.label = label;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { 
        this.addressLine1 = addressLine1;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { 
        this.addressLine2 = addressLine2;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getCity() { return city; }
    public void setCity(String city) { 
        this.city = city;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getState() { return state; }
    public void setState(String state) { 
        this.state = state;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { 
        this.postalCode = postalCode;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { 
        this.country = country;
        this.updatedAt = LocalDateTime.now();
    }
    
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    
    public String getDeliveryInstructions() { return deliveryInstructions; }
    public void setDeliveryInstructions(String deliveryInstructions) { 
        this.deliveryInstructions = deliveryInstructions;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { 
        this.contactPhone = contactPhone;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { 
        this.isDefault = isDefault;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isVerified() { return isVerified; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
