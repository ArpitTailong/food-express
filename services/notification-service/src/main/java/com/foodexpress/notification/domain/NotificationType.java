package com.foodexpress.notification.domain;

/**
 * Notification types for templating.
 */
public enum NotificationType {
    // Order notifications
    ORDER_PLACED("Order Placed", "Your order has been placed successfully"),
    ORDER_CONFIRMED("Order Confirmed", "Restaurant has confirmed your order"),
    ORDER_PREPARING("Order Preparing", "Your order is being prepared"),
    ORDER_READY("Ready for Pickup", "Your order is ready for pickup"),
    ORDER_OUT_FOR_DELIVERY("Out for Delivery", "Your order is on the way"),
    ORDER_DELIVERED("Order Delivered", "Your order has been delivered"),
    ORDER_CANCELLED("Order Cancelled", "Your order has been cancelled"),
    
    // Payment notifications
    PAYMENT_SUCCESS("Payment Successful", "Your payment was processed successfully"),
    PAYMENT_FAILED("Payment Failed", "Your payment could not be processed"),
    REFUND_INITIATED("Refund Initiated", "Your refund has been initiated"),
    REFUND_COMPLETED("Refund Completed", "Your refund has been processed"),
    
    // User notifications
    WELCOME("Welcome", "Welcome to Food Express!"),
    EMAIL_VERIFICATION("Verify Email", "Please verify your email address"),
    PASSWORD_RESET("Password Reset", "Reset your password"),
    ACCOUNT_SUSPENDED("Account Suspended", "Your account has been suspended"),
    
    // Driver notifications
    NEW_DELIVERY_AVAILABLE("New Delivery", "A new delivery is available nearby"),
    DELIVERY_ASSIGNED("Delivery Assigned", "You have been assigned a delivery"),
    
    // Promotional
    PROMOTIONAL("Special Offer", "Check out our latest offers!");
    
    private final String title;
    private final String defaultMessage;
    
    NotificationType(String title, String defaultMessage) {
        this.title = title;
        this.defaultMessage = defaultMessage;
    }
    
    public String getTitle() { return title; }
    public String getDefaultMessage() { return defaultMessage; }
    
    public String getTemplateKey() {
        return name().toLowerCase().replace("_", "-");
    }
}
