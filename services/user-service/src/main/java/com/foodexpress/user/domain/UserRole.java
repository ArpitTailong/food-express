package com.foodexpress.user.domain;

/**
 * User role enumeration.
 * Uses sealed pattern for exhaustive handling.
 */
public enum UserRole {
    CUSTOMER,
    DRIVER,
    RESTAURANT_OWNER,
    ADMIN;
    
    public boolean canOrder() {
        return this == CUSTOMER;
    }
    
    public boolean canDeliver() {
        return this == DRIVER;
    }
    
    public boolean canManageRestaurant() {
        return this == RESTAURANT_OWNER || this == ADMIN;
    }
    
    public boolean isAdmin() {
        return this == ADMIN;
    }
}
