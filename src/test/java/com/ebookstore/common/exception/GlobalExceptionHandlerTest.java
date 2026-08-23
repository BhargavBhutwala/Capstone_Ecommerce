package com.ebookstore.common.exception;

import com.ebookstore.common.exception.BusinessRuleViolationException;
import com.ebookstore.common.exception.DuplicatePaymentException;
import com.ebookstore.common.exception.GlobalExceptionHandler;
import com.ebookstore.common.exception.InsufficientStockException;
import com.ebookstore.common.exception.InvalidRequestException;
import com.ebookstore.common.exception.OrderCancellationNotAllowedException;
import com.ebookstore.common.exception.ResourceNotFoundException;
import com.ebookstore.common.exception.UnauthorizedResourceAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * <p>Uses {@code MockMvcBuilders.standaloneSetup} so that Spring Security and
 * the application context (datasource, Flyway, JPA) are never loaded. The only
 * beans active are the test controller and the exception handler under test.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestExceptionController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // =========================================================================
    // Minimal controller that throws on demand
    // =========================================================================

    @RestController
    @RequestMapping("/test-errors")
    @Validated
    static class TestExceptionController {

        @GetMapping("/not-found")
        String notFound() { throw new ResourceNotFoundException("Product not found with id: 42"); }

        @GetMapping("/insufficient-stock")
        String insufficientStock() { throw new InsufficientStockException("Insufficient stock for product id: 7"); }

        @GetMapping("/duplicate-payment")
        String duplicatePayment() { throw new DuplicatePaymentException("Payment already exists for order id: 5"); }

        @GetMapping("/cancellation-not-allowed")
        String cancellationNotAllowed() { throw new OrderCancellationNotAllowedException("Cancellation deadline has passed"); }

        @GetMapping("/business-rule")
        String businessRule() { throw new BusinessRuleViolationException("Cart is empty"); }

        @GetMapping("/invalid-request")
        String invalidRequest() { throw new InvalidRequestException("Address id must be provided"); }

        @GetMapping("/unauthorized-access")
        String unauthorizedAccess() { throw new UnauthorizedResourceAccessException("Access to this resource is not allowed"); }

        @GetMapping("/unexpected")
        String unexpected() { throw new IllegalStateException("Something went very wrong internally"); }

        @PostMapping("/validate")
        String validate(@Valid @RequestBody ValidatedRequest body) { return "ok"; }

        record ValidatedRequest(
                @NotBlank(message = "title must not be blank") String title
        ) {}
    }

    // =========================================================================
    // 404 — ResourceNotFoundException
    // =========================================================================

    @Test
    void resourceNotFound_returns404WithCorrectShape() throws Exception {
        mockMvc.perform(get("/test-errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Product not found with id: 42"))
                .andExpect(jsonPath("$.path").value("/test-errors/not-found"))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist())
                .andExpect(jsonPath("$.message", not(containsString("Exception"))))
                .andExpect(jsonPath("$.message", not(containsString("at com."))));
    }

    // =========================================================================
    // 409 — InsufficientStockException
    // =========================================================================

    @Test
    void insufficientStock_returns409() throws Exception {
        mockMvc.perform(get("/test-errors/insufficient-stock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message").value("Insufficient stock for product id: 7"))
                .andExpect(jsonPath("$.path").value("/test-errors/insufficient-stock"))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    // =========================================================================
    // 409 — DuplicatePaymentException
    // =========================================================================

    @Test
    void duplicatePayment_returns409() throws Exception {
        mockMvc.perform(get("/test-errors/duplicate-payment"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("DUPLICATE_PAYMENT"))
                .andExpect(jsonPath("$.message").value("Payment already exists for order id: 5"))
                .andExpect(jsonPath("$.path").value("/test-errors/duplicate-payment"))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    // =========================================================================
    // 409 — OrderCancellationNotAllowedException
    // =========================================================================

    @Test
    void orderCancellationNotAllowed_returns409() throws Exception {
        mockMvc.perform(get("/test-errors/cancellation-not-allowed"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("ORDER_CANCELLATION_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("Cancellation deadline has passed"))
                .andExpect(jsonPath("$.path").value("/test-errors/cancellation-not-allowed"))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    // =========================================================================
    // 409 — BusinessRuleViolationException
    // =========================================================================

    @Test
    void businessRuleViolation_returns409() throws Exception {
        mockMvc.perform(get("/test-errors/business-rule"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("Cart is empty"))
                .andExpect(jsonPath("$.path").value("/test-errors/business-rule"))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    // =========================================================================
    // 400 — InvalidRequestException
    // =========================================================================

    @Test
    void invalidRequest_returns400() throws Exception {
        mockMvc.perform(get("/test-errors/invalid-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Address id must be provided"))
                .andExpect(jsonPath("$.path").value("/test-errors/invalid-request"))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    // =========================================================================
    // 403 — UnauthorizedResourceAccessException
    // =========================================================================

    @Test
    void unauthorizedResourceAccess_returns403() throws Exception {
        mockMvc.perform(get("/test-errors/unauthorized-access"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_RESOURCE_ACCESS"))
                .andExpect(jsonPath("$.message").value("Access to this resource is not allowed"))
                .andExpect(jsonPath("$.path").value("/test-errors/unauthorized-access"))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    // =========================================================================
    // 400 — MethodArgumentNotValidException (Bean Validation with fieldErrors)
    // =========================================================================

    @Test
    void validationFailure_returns400WithFieldErrors() throws Exception {
        // Send a body with a blank title to trigger @NotBlank
        String json = """
                {"title": ""}
                """;

        mockMvc.perform(post("/test-errors/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/test-errors/validate"))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors", hasKey("title")))
                .andExpect(jsonPath("$.fieldErrors.title").value("title must not be blank"));
    }

    // =========================================================================
    // 500 — Unexpected Exception (safe generic message, no internals exposed)
    // =========================================================================

    @Test
    void unexpectedException_returns500WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test-errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."))
                .andExpect(jsonPath("$.path").value("/test-errors/unexpected"))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                // Must NOT contain the underlying exception message
                .andExpect(jsonPath("$.message", not(containsString("Something went very wrong internally"))))
                // Must NOT contain class names or stack trace fragments
                .andExpect(jsonPath("$.message", not(containsString("IllegalStateException"))))
                .andExpect(jsonPath("$.message", not(containsString("at com."))))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }
}
