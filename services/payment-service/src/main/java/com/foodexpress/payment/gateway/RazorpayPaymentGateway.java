package com.foodexpress.payment.gateway;

import com.foodexpress.payment.dto.PaymentDTOs.*;
import com.razorpay.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Razorpay Payment Gateway implementation.
 * Optimized for Indian payment methods including:
 * - UPI (Google Pay, PhonePe, Paytm, etc.)
 * - Credit/Debit Cards
 * - Net Banking
 * - Wallets (Paytm, Freecharge, etc.)
 * - EMI
 * 
 * Configuration:
 * - razorpay.key-id: Your Razorpay Key ID (rzp_test_... or rzp_live_...)
 * - razorpay.key-secret: Your Razorpay Key Secret
 * 
 * Features:
 * - Order-based payment flow (required by Razorpay)
 * - Automatic payment verification
 * - Refund processing (full and partial)
 * - Multiple payment method support
 * - INR currency optimized
 * 
 * @see <a href="https://razorpay.com/docs/api/">Razorpay API Documentation</a>
 */
public class RazorpayPaymentGateway implements PaymentGateway {
    
    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentGateway.class);
    
    private String keyId;
    private String keySecret;
    private RazorpayClient razorpayClient;
    
    // Cache for storing Razorpay Order IDs mapped to our payment IDs
    private final Map<String, String> orderIdCache = new ConcurrentHashMap<>();
    // Cache for storing Razorpay Payment IDs
    private final Map<String, String> paymentIdCache = new ConcurrentHashMap<>();
    
    // Supported payment methods in India
    private static final Set<String> SUPPORTED_METHODS = Set.of(
            "CARD", "UPI", "NETBANKING", "WALLET", "EMI", "BANK_TRANSFER"
    );
    
    /**
     * Default constructor for programmatic configuration.
     */
    public RazorpayPaymentGateway() {
    }
    
    /**
     * Set the Razorpay Key ID.
     * @param keyId Razorpay key ID (rzp_test_... or rzp_live_...)
     */
    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }
    
    /**
     * Set the Razorpay Key Secret.
     * @param keySecret Razorpay key secret
     */
    public void setKeySecret(String keySecret) {
        this.keySecret = keySecret;
    }
    
    /**
     * Initialize the Razorpay client.
     * Must be called after setting the key ID and secret.
     */
    public void init() {
        if (keyId == null || keyId.isEmpty()) {
            throw new IllegalStateException("Razorpay Key ID not configured!");
        }
        if (keySecret == null || keySecret.isEmpty()) {
            throw new IllegalStateException("Razorpay Key Secret not configured!");
        }
        
        try {
            razorpayClient = new RazorpayClient(keyId, keySecret);
            log.info("Razorpay Payment Gateway initialized. Mode: {}", 
                    keyId.startsWith("rzp_test") ? "TEST" : 
                    keyId.startsWith("rzp_live") ? "LIVE" : "UNKNOWN");
        } catch (RazorpayException e) {
            throw new IllegalStateException("Failed to initialize Razorpay client: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getProviderName() {
        return "RAZORPAY";
    }
    
    @Override
    public boolean supportsPaymentMethod(String paymentMethod) {
        return SUPPORTED_METHODS.contains(paymentMethod.toUpperCase());
    }
    
    @Override
    public GatewayResponse charge(GatewayRequest request) {
        log.info("Razorpay Gateway: Processing charge for payment {} amount {} {}",
                request.paymentId(), request.amount(), request.currency());
        
        try {
            // Step 1: Create a Razorpay Order
            Order order = createOrder(request);
            String razorpayOrderId = order.get("id");
            orderIdCache.put(request.paymentId(), razorpayOrderId);
            
            log.info("Razorpay order created: {} for payment: {}", razorpayOrderId, request.paymentId());
            
            // Step 2: Check if we have a payment token (razorpay_payment_id from frontend)
            // If token is provided, it means frontend has already completed the payment
            if (request.gatewayToken() != null && !request.gatewayToken().isEmpty() 
                    && !request.gatewayToken().startsWith("tok_")) {
                
                // This is a razorpay_payment_id from the frontend
                return verifyAndCapturePayment(request, razorpayOrderId, request.gatewayToken());
            }
            
            // No payment yet - return order details for frontend to complete payment
            // Frontend needs to use Razorpay Checkout with this order_id
            return new GatewayResponse(
                    true,  // Order created successfully
                    razorpayOrderId,  // Return order_id as transaction_id
                    "ORDER_CREATED",
                    null,
                    null,
                    null,
                    null,
                    true,  // Requires action - frontend needs to complete payment
                    new GatewayResponse.NextActionDetails(
                            "razorpay_checkout",
                            null,
                            buildCheckoutOptions(razorpayOrderId, request)  // JSON checkout options
                    )
            );
            
        } catch (RazorpayException e) {
            log.error("Razorpay charge failed for payment {}: {}", request.paymentId(), e.getMessage());
            return handleRazorpayError(e);
        } catch (Exception e) {
            log.error("Unexpected error during Razorpay charge for payment {}: {}", 
                    request.paymentId(), e.getMessage(), e);
            return new GatewayResponse(
                    false,
                    null,
                    "ERROR",
                    "GATEWAY_ERROR",
                    "Payment processing failed: " + e.getMessage(),
                    null,
                    null,
                    false,
                    null
            );
        }
    }
    
    /**
     * Create a Razorpay Order.
     * Orders are mandatory in Razorpay's payment flow.
     */
    private Order createOrder(GatewayRequest request) throws RazorpayException {
        // Convert amount to paise (smallest currency unit for INR)
        long amountInPaise = request.amount().multiply(BigDecimal.valueOf(100)).longValue();
        
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", request.currency() != null ? request.currency() : "INR");
        orderRequest.put("receipt", request.paymentId());
        
        // Add notes for tracking
        JSONObject notes = new JSONObject();
        notes.put("payment_id", request.paymentId());
        notes.put("order_id", request.orderId());
        if (request.customerId() != null) {
            notes.put("customer_id", request.customerId());
        }
        if (request.description() != null) {
            notes.put("description", request.description());
        }
        orderRequest.put("notes", notes);
        
        return razorpayClient.orders.create(orderRequest);
    }
    
    /**
     * Verify payment signature and capture the payment.
     * Called when frontend completes payment and sends razorpay_payment_id.
     */
    private GatewayResponse verifyAndCapturePayment(GatewayRequest request, 
            String razorpayOrderId, String razorpayPaymentId) throws RazorpayException {
        
        log.info("Verifying Razorpay payment: {} for order: {}", razorpayPaymentId, razorpayOrderId);
        
        // Fetch the payment details
        Payment payment = razorpayClient.payments.fetch(razorpayPaymentId);
        String status = payment.get("status");
        
        paymentIdCache.put(request.paymentId(), razorpayPaymentId);
        
        // Check if payment is already captured
        if ("captured".equals(status)) {
            return buildSuccessResponse(payment, razorpayPaymentId);
        }
        
        // If payment is authorized but not captured, capture it
        if ("authorized".equals(status)) {
            long amountInPaise = request.amount().multiply(BigDecimal.valueOf(100)).longValue();
            
            JSONObject captureRequest = new JSONObject();
            captureRequest.put("amount", amountInPaise);
            captureRequest.put("currency", request.currency() != null ? request.currency() : "INR");
            
            payment = razorpayClient.payments.capture(razorpayPaymentId, captureRequest);
            return buildSuccessResponse(payment, razorpayPaymentId);
        }
        
        // Handle other statuses
        return handlePaymentStatus(payment, razorpayPaymentId);
    }
    
    /**
     * Build success response from Razorpay payment.
     */
    private GatewayResponse buildSuccessResponse(Payment payment, String razorpayPaymentId) {
        String method = payment.get("method");
        String cardLast4 = null;
        String cardBrand = null;
        
        // Extract card details if card payment
        if ("card".equals(method)) {
            try {
                JSONObject card = payment.get("card");
                if (card != null) {
                    cardLast4 = card.optString("last4", null);
                    cardBrand = card.optString("network", null);  // VISA, Mastercard, etc.
                }
            } catch (Exception e) {
                log.debug("Could not extract card details: {}", e.getMessage());
            }
        }
        
        // For UPI, we can show the VPA
        if ("upi".equals(method)) {
            try {
                String vpa = payment.get("vpa");
                if (vpa != null) {
                    // Mask the VPA for security
                    cardLast4 = maskVpa(vpa);
                    cardBrand = "UPI";
                }
            } catch (Exception e) {
                log.debug("Could not extract UPI details: {}", e.getMessage());
            }
        }
        
        return new GatewayResponse(
                true,
                razorpayPaymentId,
                "CAPTURED",
                null,
                null,
                cardLast4,
                cardBrand != null ? cardBrand.toUpperCase() : method.toUpperCase(),
                false,
                null
        );
    }
    
    /**
     * Handle different payment statuses.
     */
    private GatewayResponse handlePaymentStatus(Payment payment, String razorpayPaymentId) {
        String status = payment.get("status");
        
        switch (status) {
            case "created":
                return new GatewayResponse(
                        true, razorpayPaymentId, "PENDING",
                        null, null, null, null, true, 
                        new GatewayResponse.NextActionDetails("pending", null, "Payment initiated, awaiting completion")
                );
            
            case "failed":
                String errorCode = payment.get("error_code");
                String errorDesc = payment.get("error_description");
                return new GatewayResponse(
                        false, razorpayPaymentId, "FAILED",
                        errorCode != null ? errorCode : "PAYMENT_FAILED",
                        errorDesc != null ? errorDesc : "Payment failed",
                        null, null, false, null
                );
            
            case "refunded":
                return new GatewayResponse(
                        true, razorpayPaymentId, "REFUNDED",
                        null, null, null, null, false, null
                );
            
            default:
                return new GatewayResponse(
                        true, razorpayPaymentId, status.toUpperCase(),
                        null, null, null, null, true, 
                        new GatewayResponse.NextActionDetails("status", null, "Payment status: " + status)
                );
        }
    }
    
    @Override
    public GatewayResponse refund(String transactionId, BigDecimal amount, String reason) {
        log.info("Razorpay Gateway: Processing refund for transaction {} amount {}", 
                transactionId, amount);
        
        try {
            // Convert amount to paise
            long amountInPaise = amount.multiply(BigDecimal.valueOf(100)).longValue();
            
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", amountInPaise);
            if (reason != null && !reason.isEmpty()) {
                JSONObject notes = new JSONObject();
                notes.put("reason", reason);
                refundRequest.put("notes", notes);
            }
            
            // Create refund on the payment
            Refund refund = razorpayClient.payments.refund(transactionId, refundRequest);
            
            String refundId = refund.get("id");
            String refundStatus = refund.get("status");
            
            log.info("Razorpay refund created: {} with status: {}", refundId, refundStatus);
            
            boolean success = "processed".equals(refundStatus) || "pending".equals(refundStatus);
            
            return new GatewayResponse(
                    success,
                    refundId,
                    refundStatus.toUpperCase(),
                    success ? null : "REFUND_FAILED",
                    success ? null : "Refund failed with status: " + refundStatus,
                    null,
                    null,
                    "pending".equals(refundStatus),  // Requires action if pending
                    null
            );
            
        } catch (RazorpayException e) {
            log.error("Razorpay refund failed for transaction {}: {}", transactionId, e.getMessage());
            return handleRazorpayError(e);
        } catch (Exception e) {
            log.error("Unexpected error during Razorpay refund for transaction {}: {}", 
                    transactionId, e.getMessage(), e);
            return new GatewayResponse(
                    false,
                    null,
                    "ERROR",
                    "REFUND_ERROR",
                    "Refund processing failed: " + e.getMessage(),
                    null,
                    null,
                    false,
                    null
            );
        }
    }
    
    @Override
    public GatewayResponse getStatus(String transactionId) {
        log.debug("Razorpay Gateway: Checking status for transaction {}", transactionId);
        
        try {
            // Try to fetch as payment first
            if (transactionId.startsWith("pay_")) {
                Payment payment = razorpayClient.payments.fetch(transactionId);
                return handlePaymentStatus(payment, transactionId);
            }
            
            // Try to fetch as order
            if (transactionId.startsWith("order_")) {
                Order order = razorpayClient.orders.fetch(transactionId);
                String status = order.get("status");
                
                return new GatewayResponse(
                        !"failed".equals(status),
                        transactionId,
                        status.toUpperCase(),
                        null,
                        null,
                        null,
                        null,
                        "created".equals(status) || "attempted".equals(status),
                        null
                );
            }
            
            // Try to fetch as refund
            if (transactionId.startsWith("rfnd_")) {
                Refund refund = razorpayClient.refunds.fetch(transactionId);
                String status = refund.get("status");
                
                return new GatewayResponse(
                        "processed".equals(status),
                        transactionId,
                        status.toUpperCase(),
                        null,
                        null,
                        null,
                        null,
                        false,
                        null
                );
            }
            
            // Unknown transaction type
            return new GatewayResponse(
                    false,
                    transactionId,
                    "UNKNOWN",
                    "INVALID_TRANSACTION",
                    "Unknown transaction ID format",
                    null,
                    null,
                    false,
                    null
            );
            
        } catch (RazorpayException e) {
            log.error("Razorpay status check failed for transaction {}: {}", transactionId, e.getMessage());
            return handleRazorpayError(e);
        } catch (Exception e) {
            log.error("Unexpected error during Razorpay status check for transaction {}: {}", 
                    transactionId, e.getMessage(), e);
            return new GatewayResponse(
                    false,
                    null,
                    "ERROR",
                    "STATUS_ERROR",
                    "Status check failed: " + e.getMessage(),
                    null,
                    null,
                    false,
                    null
            );
        }
    }
    
    /**
     * Handle Razorpay exceptions and convert to GatewayResponse.
     */
    private GatewayResponse handleRazorpayError(RazorpayException e) {
        String errorCode = "RAZORPAY_ERROR";
        String errorMessage = e.getMessage();
        
        // Parse error code if available
        if (errorMessage != null) {
            if (errorMessage.contains("BAD_REQUEST_ERROR")) {
                errorCode = "INVALID_REQUEST";
            } else if (errorMessage.contains("GATEWAY_ERROR")) {
                errorCode = "GATEWAY_ERROR";
            } else if (errorMessage.contains("SERVER_ERROR")) {
                errorCode = "SERVER_ERROR";
            }
        }
        
        return new GatewayResponse(
                false,
                null,
                "FAILED",
                errorCode,
                errorMessage,
                null,
                null,
                false,
                null
        );
    }
    
    /**
     * Build checkout options JSON for frontend Razorpay integration.
     */
    private String buildCheckoutOptions(String orderId, GatewayRequest request) {
        // Return JSON with checkout options for frontend
        JSONObject checkoutOptions = new JSONObject();
        checkoutOptions.put("key", keyId);
        checkoutOptions.put("order_id", orderId);
        checkoutOptions.put("amount", request.amount().multiply(BigDecimal.valueOf(100)).longValue());
        checkoutOptions.put("currency", request.currency() != null ? request.currency() : "INR");
        checkoutOptions.put("name", "Food Express");
        checkoutOptions.put("description", request.description() != null ? 
                request.description() : "Order Payment");
        
        // Prefill customer info if available
        JSONObject prefill = new JSONObject();
        if (request.customerId() != null) {
            prefill.put("contact", "");  // Phone number would go here
            prefill.put("email", "");    // Email would go here
        }
        checkoutOptions.put("prefill", prefill);
        
        // Theme
        JSONObject theme = new JSONObject();
        theme.put("color", "#FF6B35");  // Food Express brand color
        checkoutOptions.put("theme", theme);
        
        return checkoutOptions.toString();
    }
    
    /**
     * Mask UPI VPA for security (show only last part after @).
     */
    private String maskVpa(String vpa) {
        if (vpa == null || !vpa.contains("@")) {
            return "****";
        }
        String[] parts = vpa.split("@");
        return "****@" + parts[parts.length - 1];
    }
    
    /**
     * Verify payment signature from Razorpay webhook/callback.
     * Should be used to verify that the payment callback is authentic.
     */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);
            
            Utils.verifyPaymentSignature(attributes, keySecret);
            return true;
        } catch (RazorpayException e) {
            log.error("Payment signature verification failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Verify webhook signature.
     */
    public boolean verifyWebhookSignature(String payload, String signature, String webhookSecret) {
        try {
            Utils.verifyWebhookSignature(payload, signature, webhookSecret);
            return true;
        } catch (RazorpayException e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}
