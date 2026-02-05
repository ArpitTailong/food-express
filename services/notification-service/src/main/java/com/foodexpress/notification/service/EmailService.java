package com.foodexpress.notification.service;

import com.foodexpress.notification.domain.NotificationType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.UUID;

/**
 * Email Service.
 * Handles email delivery with HTML templates.
 */
@Service
public class EmailService {
    
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }
    
    @RateLimiter(name = "emailSending")
    @CircuitBreaker(name = "emailService")
    public String send(String to, String subject, String content, NotificationType type) {
        log.info("Sending email to {} with subject: {}", maskEmail(to), subject);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom("noreply@foodexpress.com", "Food Express");
            helper.setTo(to);
            helper.setSubject(subject);
            
            // Try to use template, fallback to plain content
            String htmlContent = renderTemplate(type, subject, content);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
            String messageId = UUID.randomUUID().toString();
            log.info("Email sent successfully to {}, messageId: {}", maskEmail(to), messageId);
            
            return messageId;
            
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", maskEmail(to), e.getMessage());
            throw new EmailDeliveryException("Failed to send email", e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", maskEmail(to), e.getMessage());
            throw new EmailDeliveryException("Unexpected error sending email", e);
        }
    }
    
    private String renderTemplate(NotificationType type, String subject, String content) {
        try {
            Context context = new Context();
            context.setVariable("title", subject);
            context.setVariable("content", content);
            context.setVariable("year", java.time.Year.now().getValue());
            
            String templateName = "emails/" + type.getTemplateKey();
            
            // Try specific template first
            try {
                return templateEngine.process(templateName, context);
            } catch (Exception e) {
                // Fall back to generic template
                return templateEngine.process("emails/generic", context);
            }
        } catch (Exception e) {
            // If no template engine, return simple HTML
            return String.format("""
                    <!DOCTYPE html>
                    <html>
                    <head><meta charset="UTF-8"></head>
                    <body style="font-family: Arial, sans-serif; padding: 20px;">
                        <h2 style="color: #FF6B6B;">%s</h2>
                        <p>%s</p>
                        <hr style="border: none; border-top: 1px solid #eee;">
                        <p style="color: #888; font-size: 12px;">Food Express Team</p>
                    </body>
                    </html>
                    """, subject, content);
        }
    }
    
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 2) return "**@" + parts[1];
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + "@" + parts[1];
    }
    
    public static class EmailDeliveryException extends RuntimeException {
        public EmailDeliveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
