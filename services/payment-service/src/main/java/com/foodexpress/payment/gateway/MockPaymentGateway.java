package com.foodexpress.payment.gateway;

import com.foodexpress.payment.dto.PaymentDTOs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock Payment Gateway for development and testing.
 * Simulates real gateway behavior including:
 * - Random failures for testing error handling
 * - 3D Secure simulation
 * - Card brand detection
 * - Delay simulation for realistic timing
 * 
 * PRODUCTION: Replace with real Stripe/Razorpay implementation.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {
    
    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);
    
    // Simulated transaction store (for status checks and refunds)
    private final Map<String, StoredTransaction> transactions = new ConcurrentHashMap<>();
    
    private final Random random = new Random();
    
    // Special test tokens for predictable behavior
    private static final String TOKEN_ALWAYS_SUCCESS = "tok_success";
    private static final String TOKEN_ALWAYS_FAIL = "tok_fail";
    private static final String TOKEN_INSUFFICIENT_FUNDS = "tok_insufficient";
    private static final String TOKEN_CARD_DECLINED = "tok_declined";
    private static final String TOKEN_REQUIRES_3DS = "tok_3ds_required";
    private static final String TOKEN_NETWORK_ERROR = "tok_network_error";
    
    private static final Set<String> SUPPORTED_METHODS = Set.of("CARD", "WALLET", "UPI");
    
    @Override
    public String getProviderName() {
        return "MOCK_GATEWAY";
    }
    
    @Override
    public boolean supportsPaymentMethod(String paymentMethod) {
        return SUPPORTED_METHODS.contains(paymentMethod.toUpperCase());
    }
    
    @Override
    public GatewayResponse charge(GatewayRequest request) {
        log.info("Mock Gateway: Processing charge for payment {} amount {} {}",
                request.paymentId(), request.amount(), request.currency());
        
        // Simulate network latency (100-500ms)
        simulateLatency();
        
        String token = request.gatewayToken();
        
        // Handle special test tokens
        if (token != null) {
            GatewayResponse testResponse = handleTestToken(token, request);
            if (testResponse != null) {
                return testResponse;
            }
        }
        
        // Random failure (5% chance in mock mode)
        if (random.nextInt(100) < 5) {
            log.warn("Mock Gateway: Simulating random failure for payment {}", request.paymentId());
            return createFailureResponse("RANDOM_FAILURE", "Simulated random gateway failure");
        }
        
        // Success case
        String transactionId = "txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        CardInfo cardInfo = extractCardInfo(token);
        
        // Store transaction for status checks
        transactions.put(transactionId, new StoredTransaction(
                transactionId,
                request.paymentId(),
                request.amount(),
                request.currency(),
                "SUCCESS",
                cardInfo
        ));
        
        log.info("Mock Gateway: Charge successful. Transaction ID: {}", transactionId);
        
        return new GatewayResponse(
                true,
                transactionId,
                "APPROVED",
                null,
                null,
                cardInfo.lastFour(),
                cardInfo.brand(),
                false,
                null
        );
    }
    
    @Override
    public GatewayResponse refund(String transactionId, BigDecimal amount, String reason) {
        log.info("Mock Gateway: Processing refund for transaction {} amount {}", transactionId, amount);
        
        simulateLatency();
        
        StoredTransaction original = transactions.get(transactionId);
        if (original == null) {
            return createFailureResponse("TRANSACTION_NOT_FOUND", "Original transaction not found");
        }
        
        if ("REFUNDED".equals(original.status())) {
            return createFailureResponse("ALREADY_REFUNDED", "Transaction already refunded");
        }
        
        BigDecimal refundAmount = amount != null ? amount : original.amount();
        if (refundAmount.compareTo(original.amount()) > 0) {
            return createFailureResponse("INVALID_AMOUNT", "Refund amount exceeds original");
        }
        
        String refundId = "ref_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        
        // Update stored transaction
        transactions.put(transactionId, new StoredTransaction(
                transactionId,
                original.paymentId(),
                original.amount(),
                original.currency(),
                "REFUNDED",
                original.cardInfo()
        ));
        
        log.info("Mock Gateway: Refund successful. Refund ID: {}", refundId);
        
        return new GatewayResponse(
                true,
                refundId,
                "REFUND_PROCESSED",
                null,
                null,
                original.cardInfo().lastFour(),
                original.cardInfo().brand(),
                false,
                null
        );
    }
    
    @Override
    public GatewayResponse getStatus(String transactionId) {
        log.debug("Mock Gateway: Checking status for transaction {}", transactionId);
        
        StoredTransaction txn = transactions.get(transactionId);
        if (txn == null) {
            return createFailureResponse("NOT_FOUND", "Transaction not found");
        }
        
        return new GatewayResponse(
                true,
                transactionId,
                txn.status(),
                null,
                null,
                txn.cardInfo().lastFour(),
                txn.cardInfo().brand(),
                false,
                null
        );
    }
    
    // ========================================
    // HELPER METHODS
    // ========================================
    
    private GatewayResponse handleTestToken(String token, GatewayRequest request) {
        return switch (token) {
            case TOKEN_ALWAYS_SUCCESS -> {
                String txnId = "txn_success_" + System.currentTimeMillis();
                CardInfo card = new CardInfo("4242", "VISA");
                transactions.put(txnId, new StoredTransaction(
                        txnId, request.paymentId(), request.amount(), 
                        request.currency(), "SUCCESS", card
                ));
                yield new GatewayResponse(true, txnId, "APPROVED", 
                        null, null, "4242", "VISA", false, null);
            }
            
            case TOKEN_ALWAYS_FAIL -> 
                createFailureResponse("TEST_FAILURE", "Test token for failure");
            
            case TOKEN_INSUFFICIENT_FUNDS -> 
                createFailureResponse("INSUFFICIENT_FUNDS", "Card has insufficient funds");
            
            case TOKEN_CARD_DECLINED -> 
                createFailureResponse("CARD_DECLINED", "Card was declined by issuer");
            
            case TOKEN_REQUIRES_3DS -> new GatewayResponse(
                    false, null, "3DS_REQUIRED", null, null, "4000", "VISA",
                    true,
                    new GatewayResponse.NextActionDetails(
                            "REDIRECT_TO_URL",
                            "https://mock-3ds.example.com/auth/" + request.paymentId(),
                            "cs_" + UUID.randomUUID()
                    )
            );
            
            case TOKEN_NETWORK_ERROR -> 
                throw new RuntimeException("Simulated network error");
            
            default -> null; // Not a test token, proceed normally
        };
    }
    
    private GatewayResponse createFailureResponse(String errorCode, String errorMessage) {
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
    
    private CardInfo extractCardInfo(String token) {
        // Mock: Extract card info from token prefix
        if (token == null || token.isEmpty()) {
            return new CardInfo("0000", "UNKNOWN");
        }
        
        // Simulate different card brands based on token
        if (token.contains("visa") || token.startsWith("4")) {
            return new CardInfo("4242", "VISA");
        } else if (token.contains("master") || token.startsWith("5")) {
            return new CardInfo("5555", "MASTERCARD");
        } else if (token.contains("amex") || token.startsWith("3")) {
            return new CardInfo("0005", "AMEX");
        } else if (token.contains("rupay")) {
            return new CardInfo("6521", "RUPAY");
        }
        
        return new CardInfo("1234", "VISA");
    }
    
    private void simulateLatency() {
        try {
            Thread.sleep(100 + random.nextInt(400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // ========================================
    // INTERNAL RECORDS
    // ========================================
    
    private record CardInfo(String lastFour, String brand) {}
    
    private record StoredTransaction(
            String transactionId,
            String paymentId,
            BigDecimal amount,
            String currency,
            String status,
            CardInfo cardInfo
    ) {}
}
