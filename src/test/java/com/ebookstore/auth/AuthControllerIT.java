package com.ebookstore.auth;

import com.ebookstore.util.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for auth and user-profile endpoints.
 *
 * <p>Exercises the full request → controller → service → repository → PostgreSQL stack.
 * Uses a real Testcontainers PostgreSQL instance with Flyway-migrated schema.
 * Database is cleaned before each test by {@link AbstractIntegrationTest}.
 *
 * <p>Tests MUST NOT be skipped when Docker is unavailable — if the container
 * fails to start the run fails with the exact container error.
 */
class AuthControllerIT extends AbstractIntegrationTest {

    // =========================================================================
    // POST /auth/register
    // =========================================================================

    @Test
    void register_validRequest_returns201WithUserResponse() throws Exception {
        String body = """
                {"firstName":"Alice","lastName":"Smith",
                "email":"alice_reg@example.com","password":"password123"}
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email").value("alice_reg@example.com"))
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void register_duplicateEmail_returns409WithBusinessRuleCode() throws Exception {
        String body = """
                {"firstName":"Bob","lastName":"Jones",
                "email":"duplicate@example.com","password":"password123"}
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void register_invalidRequest_returns400WithFieldErrors() throws Exception {
        String body = """
                {"firstName":"","lastName":"Smith",
                "email":"not-an-email","password":"short"}
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", notNullValue()));
    }

    @Test
    void register_createsCartForNewUser() throws Exception {
        String email = "cartcheck@example.com";
        String token = registerAndLogin(email);
        assertThat(token).isNotBlank();

        // Cart exists and is ACTIVE after registration
        mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // =========================================================================
    // POST /auth/login
    // =========================================================================

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        registerAndLogin("carol_login@example.com"); // registers + gets token

        // Explicit login check
        String loginBody = """
                {"email":"carol_login@example.com","password":"password123"}
                """;
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn", notNullValue()))
                .andExpect(jsonPath("$.user.email").value("carol_login@example.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void login_invalidCredentials_returns401WithErrorResponse() throws Exception {
        String body = """
                {"email":"nobody@example.com","password":"wrongpassword"}
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    // =========================================================================
    // GET /users/me
    // =========================================================================

    @Test
    void getCurrentUser_withValidJwt_returns200() throws Exception {
        String email = "dave_me@example.com";
        String token = registerAndLogin(email);

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void getCurrentUser_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // =========================================================================
    // POST /auth/logout
    // =========================================================================

    @Test
    void logout_withValidJwt_returns204() throws Exception {
        String email = "eve_logout@example.com";
        String token = registerAndLogin(email);

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void logout_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized());
    }
}
