package com.foodexpress.user.exception;

import com.foodexpress.user.service.UserService.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
@org.springframework.stereotype.Component("userGlobalExceptionHandler")
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("User Not Found");
        problem.setType(URI.create("https://api.foodexpress.com/errors/user-not-found"));
        problem.setProperty("timestamp", Instant.now());
        
        return problem;
    }
    
    @ExceptionHandler(DuplicateEmailException.class)
    public ProblemDetail handleDuplicateEmail(DuplicateEmailException ex) {
        log.warn("Duplicate email: {}", ex.getMessage());
        
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Email Already Registered");
        problem.setType(URI.create("https://api.foodexpress.com/errors/duplicate-email"));
        problem.setProperty("timestamp", Instant.now());
        
        return problem;
    }
    
    @ExceptionHandler(DuplicatePhoneException.class)
    public ProblemDetail handleDuplicatePhone(DuplicatePhoneException ex) {
        log.warn("Duplicate phone: {}", ex.getMessage());
        
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Phone Number Already Registered");
        problem.setType(URI.create("https://api.foodexpress.com/errors/duplicate-phone"));
        problem.setProperty("timestamp", Instant.now());
        
        return problem;
    }
    
    @ExceptionHandler(AddressNotFoundException.class)
    public ProblemDetail handleAddressNotFound(AddressNotFoundException ex) {
        log.warn("Address not found: {}", ex.getMessage());
        
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Address Not Found");
        problem.setType(URI.create("https://api.foodexpress.com/errors/address-not-found"));
        problem.setProperty("timestamp", Instant.now());
        
        return problem;
    }
    
    @ExceptionHandler(AddressLimitExceededException.class)
    public ProblemDetail handleAddressLimitExceeded(AddressLimitExceededException ex) {
        log.warn("Address limit exceeded: {}", ex.getMessage());
        
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Address Limit Exceeded");
        problem.setType(URI.create("https://api.foodexpress.com/errors/address-limit-exceeded"));
        problem.setProperty("timestamp", Instant.now());
        
        return problem;
    }
    
    @ExceptionHandler(InvalidOperationException.class)
    public ProblemDetail handleInvalidOperation(InvalidOperationException ex) {
        log.warn("Invalid operation: {}", ex.getMessage());
        
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Operation");
        problem.setType(URI.create("https://api.foodexpress.com/errors/invalid-operation"));
        problem.setProperty("timestamp", Instant.now());
        
        return problem;
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationError(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        log.warn("Validation error: {}", errors);
        
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, errors);
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://api.foodexpress.com/errors/validation-error"));
        problem.setProperty("timestamp", Instant.now());
        
        return problem;
    }
    
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericError(Exception ex) {
        log.error("Unexpected error", ex);
        
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://api.foodexpress.com/errors/internal-error"));
        problem.setProperty("timestamp", Instant.now());
        
        return problem;
    }
}
