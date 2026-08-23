package com.ebookstore.common.exception;

import com.ebookstore.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized exception handler for all REST controllers.
 *
 * <p>Every handler builds an {@link ErrorResponse} with:
 * <ul>
 *   <li>{@code timestamp} — current UTC offset timestamp</li>
 *   <li>{@code status} — HTTP status code integer</li>
 *   <li>{@code code} — short machine-readable string</li>
 *   <li>{@code message} — safe, human-readable description (never a stack trace)</li>
 *   <li>{@code path} — request URI</li>
 *   <li>{@code fieldErrors} — field-level constraint violations (validation only)</li>
 * </ul>
 *
 * <p>Security-specific 401/403 responses (unauthenticated requests, Spring Security
 * access denied) are handled by {@code AuthEntryPoint} and
 * {@code AccessDeniedHandlerImpl} in Task 6 — they also use {@link ErrorResponse}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // -------------------------------------------------------------------------
    // 404 Not Found
    // -------------------------------------------------------------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request);
    }

    // -------------------------------------------------------------------------
    // 409 Conflict
    // -------------------------------------------------------------------------

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(
            InsufficientStockException ex, HttpServletRequest request) {

        log.warn("Insufficient stock: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatePayment(
            DuplicatePaymentException ex, HttpServletRequest request) {

        log.warn("Duplicate payment: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "DUPLICATE_PAYMENT", ex.getMessage(), request);
    }

    @ExceptionHandler(OrderCancellationNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleOrderCancellationNotAllowed(
            OrderCancellationNotAllowedException ex, HttpServletRequest request) {

        log.warn("Order cancellation not allowed: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "ORDER_CANCELLATION_NOT_ALLOWED", ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolation(
            BusinessRuleViolationException ex, HttpServletRequest request) {

        log.warn("Business rule violation: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "BUSINESS_RULE_VIOLATION", ex.getMessage(), request);
    }

    // -------------------------------------------------------------------------
    // 400 Bad Request — malformed/unreadable request body (e.g. invalid enum value)
    // -------------------------------------------------------------------------

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Malformed request body on {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Request body is malformed or contains an invalid value.", request);
    }

    // -------------------------------------------------------------------------
    // 401 Unauthorized — invalid credentials
    // -------------------------------------------------------------------------

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {

        log.warn("Invalid credentials attempt on {}", request.getRequestURI());
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                "Invalid email or password.", request);
    }

    // -------------------------------------------------------------------------
    // 400 Bad Request
    // -------------------------------------------------------------------------

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(
            InvalidRequestException ex, HttpServletRequest request) {

        log.warn("Invalid request: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            // Keep only the first violation per field for a clean response
            fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }

        log.warn("Validation failed for {}: {}", request.getRequestURI(), fieldErrors);

        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Request validation failed",
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // -------------------------------------------------------------------------
    // 403 Forbidden
    // -------------------------------------------------------------------------

    @ExceptionHandler(UnauthorizedResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedResourceAccess(
            UnauthorizedResourceAccessException ex, HttpServletRequest request) {

        log.warn("Unauthorized resource access: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "UNAUTHORIZED_RESOURCE_ACCESS", ex.getMessage(), request);
    }

    // -------------------------------------------------------------------------
    // 500 Internal Server Error — catch-all, never expose internals
    // -------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {

        // Log full details server-side only; never send to the client
        log.error("Unexpected error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.",
                request
        );
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String code, String message, HttpServletRequest request) {

        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                code,
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}
