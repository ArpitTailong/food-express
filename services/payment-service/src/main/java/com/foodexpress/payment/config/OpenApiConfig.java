package com.foodexpress.payment.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger documentation configuration
 */
@Configuration
public class OpenApiConfig {
    
    @Value("${server.port:8083}")
    private String serverPort;
    
    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Service API")
                        .description("""
                                Payment processing microservice for Food Express application.
                                
                                Features:
                                - Idempotent payment creation
                                - Payment state machine (CREATED → PROCESSING → SUCCESS/FAILED)
                                - Refund processing
                                - Retry failed payments
                                - Circuit breaker for gateway calls
                                - Rate limiting per customer
                                - Kafka events for saga coordination
                                
                                **IMPORTANT**: All POST requests require X-Idempotency-Key header.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Food Express Team")
                                .email("support@foodexpress.com")
                                .url("https://foodexpress.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT"))
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server"),
                        new Server()
                                .url("http://localhost:8080/payment-service")
                                .description("API Gateway")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token from Auth Service")
                        )
                );
    }
}
