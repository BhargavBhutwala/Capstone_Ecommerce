package com.ebookstore.security;

import com.ebookstore.util.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for security behaviour:
 * - public endpoints work without JWT
 * - authenticated endpoints return 401 without JWT
 * - authenticated endpoints work with valid JWT
 * - invalid/tampered JWT → 401
 * - ErrorResponse shape is preserved for 401 responses
 *
 * <p>Uses the shared Testcontainers PostgreSQL from {@link AbstractIntegrationTest}.
 */
class SecurityIT extends AbstractIntegrationTest {

    // =========================================================================
    // Public endpoints work without JWT
    // =========================================================================

    @Test
    void catalogEndpoints_accessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/categories")).andExpect(status().isOk());
        mockMvc.perform(get("/brands")).andExpect(status().isOk());
        mockMvc.perform(get("/products")).andExpect(status().isOk());
        mockMvc.perform(get("/products/1")).andExpect(status().isOk());
    }

    @Test
    void authRegisterAndLogin_accessibleWithoutToken() throws Exception {
        // Registration — public
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"X\",\"lastName\":\"Y\","
                                + "\"email\":\"sec_pub@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());

        // Login — public
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"sec_pub@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // Authenticated endpoints require JWT
    // =========================================================================

    @Test
    void cartEndpoint_withoutJwt_returns401WithErrorResponseShape() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.path").isString());
    }

    @Test
    void addressEndpoint_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/addresses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void ordersEndpoint_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void paymentsEndpoint_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":1,\"paymentMethod\":\"CREDIT_CARD\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // =========================================================================
    // Authenticated endpoints work with valid JWT
    // =========================================================================

    @Test
    void cartEndpoint_withValidJwt_returns200() throws Exception {
        String token = registerAndLogin("sec_valid@example.com");

        mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void usersMe_withValidJwt_returns200() throws Exception {
        String token = registerAndLogin("sec_me@example.com");

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("sec_me@example.com"));
    }

    // =========================================================================
    // Invalid / tampered JWT → 401
    // =========================================================================

    @Test
    void cartEndpoint_withTamperedJwt_returns401() throws Exception {
        String token = registerAndLogin("sec_tamper@example.com");

        // Tamper the signature by flipping the last character
        String tampered = token.substring(0, token.length() - 3) + "XXX";

        mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void cartEndpoint_withMalformedBearerToken_returns401() throws Exception {
        mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer not.a.valid.jwt.at.all"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void cartEndpoint_withRandomString_returns401() throws Exception {
        mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer randomgarbage12345"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // =========================================================================
    // ErrorResponse shape preservation
    // =========================================================================

    @Test
    void unauthorizedResponse_hasCorrectErrorShape() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.path").isString());
    }

    @Test
    void notFoundResponse_hasCorrectErrorShape() throws Exception {
        mockMvc.perform(get("/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.path").isString());
    }
}
