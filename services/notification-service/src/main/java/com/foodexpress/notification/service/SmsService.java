package com.foodexpress.notification.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * SMS Service.
 * Placeholder for SMS delivery (Twilio, AWS SNS, etc.)
 */
@Service
public class SmsService {
    
    private static final Logger log = LoggerFactory.getLogger(SmsService.class);
    
    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;
    
    @RateLimiter(name = "smsSending")
    @CircuitBreaker(name = "smsService")
    public String send(String phoneNumber, String message) {
        log.info("Sending SMS to {}", maskPhone(phoneNumber));
        
        if (!smsEnabled) {
            log.info("SMS sending disabled, simulating send");
            return "sim-" + UUID.randomUUID().toString();
        }
        
        // TODO: Integrate with Twilio or AWS SNS
        // Example Twilio integration:
        // Message twilioMessage = Message.creator(
        //     new PhoneNumber(phoneNumber),
        //     new PhoneNumber(twilioFromNumber),
        //     message
        // ).create();
        // return twilioMessage.getSid();
        
        String messageId = UUID.randomUUID().toString();
        log.info("SMS sent successfully to {}, messageId: {}", maskPhone(phoneNumber), messageId);
        
        return messageId;
    }
    
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return "***" + phone.substring(phone.length() - 4);
    }
}
