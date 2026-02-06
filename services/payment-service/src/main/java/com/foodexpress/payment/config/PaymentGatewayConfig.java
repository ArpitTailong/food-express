package com.foodexpress.payment.config;

import com.foodexpress.payment.gateway.MockPaymentGateway;
import com.foodexpress.payment.gateway.PaymentGateway;
import com.foodexpress.payment.gateway.RazorpayPaymentGateway;
import com.foodexpress.payment.gateway.StripePaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Payment Gateway Configuration.
 * Configures the appropriate payment gateway based on the app.gateway.provider property.
 * 
 * Supported providers:
 * - MOCK: Mock gateway for testing (default)
 * - STRIPE: Stripe payment gateway (international)
 * - RAZORPAY: Razorpay payment gateway (optimized for India)
 * 
 * Usage in application.yml:
 *   app:
 *     gateway:
 *       provider: RAZORPAY  # or MOCK, STRIPE
 *   
 *   # For Razorpay (recommended for India):
 *   razorpay:
 *     key-id: rzp_test_your_key_here
 *     key-secret: your_secret_here
 *   
 *   # For Stripe (international):
 *   stripe:
 *     api-key: sk_test_your_key_here
 *     webhook-secret: whsec_your_secret_here
 */
@Configuration
public class PaymentGatewayConfig {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayConfig.class);
    
    /**
     * Mock Payment Gateway bean for development/testing.
     * Activated when app.gateway.provider=MOCK (or missing, as it's the default)
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.gateway.provider", havingValue = "MOCK", matchIfMissing = true)
    public PaymentGateway mockPaymentGateway() {
        log.info("==> Configuring Mock Payment Gateway for development/testing");
        return new MockPaymentGateway();
    }
    
    /**
     * Razorpay Payment Gateway bean for Indian payments.
     * Activated when app.gateway.provider=RAZORPAY
     * Supports UPI, Cards, Net Banking, Wallets, EMI
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.gateway.provider", havingValue = "RAZORPAY")
    public PaymentGateway razorpayPaymentGateway(
            @Value("${razorpay.key-id}") String keyId,
            @Value("${razorpay.key-secret}") String keySecret
    ) {
        log.info("==> Configuring Razorpay Payment Gateway (India)");
        if (keyId.startsWith("rzp_test")) {
            log.info("    Mode: TEST (using test API key)");
        } else if (keyId.startsWith("rzp_live")) {
            log.info("    Mode: LIVE (using live API key)");
        }
        log.info("    Supported methods: UPI, Cards, Net Banking, Wallets, EMI");
        
        RazorpayPaymentGateway gateway = new RazorpayPaymentGateway();
        gateway.setKeyId(keyId);
        gateway.setKeySecret(keySecret);
        gateway.init(); // Initialize Razorpay client
        return gateway;
    }
    
    /**
     * Stripe Payment Gateway bean for international payments.
     * Activated when app.gateway.provider=STRIPE
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.gateway.provider", havingValue = "STRIPE")
    public PaymentGateway stripePaymentGateway(
            @Value("${stripe.api-key}") String apiKey,
            @Value("${stripe.webhook-secret:}") String webhookSecret
    ) {
        log.info("==> Configuring Stripe Payment Gateway (International)");
        if (apiKey.startsWith("sk_test")) {
            log.info("    Mode: TEST (using test API key)");
        } else if (apiKey.startsWith("sk_live")) {
            log.info("    Mode: LIVE (using live API key)");
        }
        
        StripePaymentGateway gateway = new StripePaymentGateway();
        gateway.setApiKey(apiKey);
        gateway.setWebhookSecret(webhookSecret);
        gateway.init(); // Initialize Stripe API
        return gateway;
    }
}
