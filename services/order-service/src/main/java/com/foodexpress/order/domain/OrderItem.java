package com.foodexpress.order.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Order item entity - represents a single item in an order.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @Column(name = "menu_item_id", nullable = false, length = 36)
    private String menuItemId;
    
    @Column(name = "menu_item_name", nullable = false, length = 200)
    private String menuItemName;
    
    @Column(name = "menu_item_description", length = 500)
    private String menuItemDescription;
    
    @Column(name = "menu_item_image_url", length = 500)
    private String menuItemImageUrl;
    
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;
    
    @Column(name = "special_instructions", length = 500)
    private String specialInstructions;
    
    // Optional: customizations/add-ons stored as JSON
    @Column(name = "customizations", columnDefinition = "TEXT")
    private String customizations;
    
    // ========================================
    // CONSTRUCTORS
    // ========================================
    
    protected OrderItem() {} // JPA
    
    public OrderItem(String menuItemId, String menuItemName, BigDecimal unitPrice, Integer quantity) {
        this.id = UUID.randomUUID().toString();
        this.menuItemId = menuItemId;
        this.menuItemName = menuItemName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    // ========================================
    // BUSINESS METHODS
    // ========================================
    
    public void updateQuantity(int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.quantity = newQuantity;
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(newQuantity));
    }
    
    // ========================================
    // GETTERS & SETTERS
    // ========================================
    
    public String getId() { return id; }
    public Order getOrder() { return order; }
    public String getMenuItemId() { return menuItemId; }
    public String getMenuItemName() { return menuItemName; }
    public String getMenuItemDescription() { return menuItemDescription; }
    public String getMenuItemImageUrl() { return menuItemImageUrl; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public String getSpecialInstructions() { return specialInstructions; }
    public String getCustomizations() { return customizations; }
    
    void setOrder(Order order) { this.order = order; }
    public void setMenuItemDescription(String menuItemDescription) { this.menuItemDescription = menuItemDescription; }
    public void setMenuItemImageUrl(String menuItemImageUrl) { this.menuItemImageUrl = menuItemImageUrl; }
    public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }
    public void setCustomizations(String customizations) { this.customizations = customizations; }
}
