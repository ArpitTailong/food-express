package com.foodexpress.user.domain;

/**
 * User account status.
 */
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    PENDING_VERIFICATION,
    DELETED;
    
    public boolean canLogin() {
        return this == ACTIVE;
    }
    
    public boolean isAccessible() {
        return this == ACTIVE || this == PENDING_VERIFICATION;
    }
}
