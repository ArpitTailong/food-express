package com.foodexpress.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Embeddable delivery address value object.
 */
@Embeddable
public class DeliveryAddress {
    
    @Column(name = "address_line1", length = 200)
    private String addressLine1;
    
    @Column(name = "address_line2", length = 200)
    private String addressLine2;
    
    @Column(name = "city", length = 100)
    private String city;
    
    @Column(name = "state", length = 100)
    private String state;
    
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    
    @Column(name = "country", length = 100)
    private String country;
    
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude")
    private Double longitude;
    
    @Column(name = "contact_name", length = 100)
    private String contactName;
    
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;
    
    @Column(name = "address_type", length = 20)
    private String addressType; // HOME, WORK, OTHER
    
    // ========================================
    // CONSTRUCTORS
    // ========================================
    
    protected DeliveryAddress() {} // JPA
    
    public DeliveryAddress(String addressLine1, String city, String state, 
                           String postalCode, String country,
                           String contactName, String contactPhone) {
        this.addressLine1 = addressLine1;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
    }
    
    // ========================================
    // BUSINESS METHODS
    // ========================================
    
    public String getFullAddress() {
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
    
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
    
    // ========================================
    // GETTERS & SETTERS
    // ========================================
    
    public String getAddressLine1() { return addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPostalCode() { return postalCode; }
    public String getCountry() { return country; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getContactName() { return contactName; }
    public String getContactPhone() { return contactPhone; }
    public String getAddressType() { return addressType; }
    
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public void setCountry(String country) { this.country = country; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public void setAddressType(String addressType) { this.addressType = addressType; }
}
