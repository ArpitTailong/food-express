package com.foodexpress.common.util;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Money representation using Java 21 Record.
 * Immutable value object for monetary operations.
 */
public record Money(
        BigDecimal amount,
        Currency currency
) {
    // Common currency constants
    public static final Currency USD = Currency.getInstance("USD");
    public static final Currency EUR = Currency.getInstance("EUR");
    public static final Currency GBP = Currency.getInstance("GBP");
    public static final Currency INR = Currency.getInstance("INR");
    
    /**
     * Compact constructor with validation
     */
    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        // Scale to currency's default fraction digits
        amount = amount.setScale(currency.getDefaultFractionDigits(), java.math.RoundingMode.HALF_UP);
    }
    
    // ========================================
    // FACTORY METHODS
    // ========================================
    
    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }
    
    public static Money of(double amount, Currency currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }
    
    public static Money usd(BigDecimal amount) {
        return new Money(amount, USD);
    }
    
    public static Money usd(double amount) {
        return new Money(BigDecimal.valueOf(amount), USD);
    }
    
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }
    
    // ========================================
    // ARITHMETIC OPERATIONS
    // ========================================
    
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }
    
    public Money multiply(int factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }
    
    public Money multiply(BigDecimal factor) {
        return new Money(this.amount.multiply(factor), this.currency);
    }
    
    public Money percentage(BigDecimal percent) {
        BigDecimal factor = percent.divide(BigDecimal.valueOf(100), 10, java.math.RoundingMode.HALF_UP);
        return multiply(factor);
    }
    
    public Money negate() {
        return new Money(amount.negate(), currency);
    }
    
    // ========================================
    // COMPARISON
    // ========================================
    
    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }
    
    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }
    
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }
    
    // ========================================
    // FORMATTING
    // ========================================
    
    public String formatted() {
        return "%s %s".formatted(currency.getSymbol(), amount.toPlainString());
    }
    
    public String formattedWithCode() {
        return "%s %s".formatted(amount.toPlainString(), currency.getCurrencyCode());
    }
    
    // ========================================
    // VALIDATION
    // ========================================
    
    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot perform operation on different currencies: %s and %s"
                            .formatted(this.currency, other.currency));
        }
    }
}
