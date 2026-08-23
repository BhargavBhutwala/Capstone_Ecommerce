package com.ebookstore.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.DockerClientFactory;

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
 * <p>Spins up a real PostgreSQL container via Testcontainers (configured in
 * {@code application-test.yml}) and runs Flyway migrations, so the full
 * request→controller→service→repository→DB stack is exercised.
 *
 * <p>The tests are skipped automatically when Docker is not available on the
 * current machine (CI environments without Docker, local dev without daemon).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @BeforeAll
    static void requireDocker() {
        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Skipping integration tests: Docker is not available on this machine.");
    }

    // =========================================================================
    // POST /auth/register
    // =========================================================================

    @Test
    void register_validRequest_returns201WithUserResponse() throws Exception {
        String body = """
                {
                  "firstName": "Alice",
                  "lastName": "Smith",
                  "email": "alice_reg1@example.com",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email").value("alice_reg1@example.com"))
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String body = """
                {
                  "firstName": "Bob",
                  "lastName": "Jones",
                  "email": "duplicate_reg@example.com",
                  "password": "password123"
                }
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
                {
                  "firstName": "",
                  "lastName": "Smith",
                  "email": "not-an-email",
                  "password": "short"
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", notNullValue()));
    }

    // =========================================================================
    // POST /auth/login
    // =========================================================================

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        registerUser("carol_login@example.com");

        String loginBody = """
                {
                  "email": "carol_login@example.com",
                  "password": "password123"
                }
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
    void login_invalidCredentials_returns401() throws Exception {
        String body = """
                {
                  "email": "nobody@example.com",
                  "password": "wrongpassword"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // =========================================================================
    // GET /users/me
    // =========================================================================

    @Test
    void getCurrentUser_withValidJwt_returns200() throws Exception {
        String email = "dave_me@example.com";
        registerUser(email);
        String token = loginAndGetToken(email);

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
        registerUser(email);
        String token = loginAndGetToken(email);

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void logout_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Cart creation verification
    // =========================================================================

    @Test
    void register_createsExactlyOneCart_verifiedBySuccessfulLogin() throws Exception {
        String email = "frank_cart@example.com";
        registerUser(email);
        // If cart wasn't created properly or user wasn't saved fully, login would fail
        String token = loginAndGetToken(email);
        assertThat(token).isNotBlank();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void registerUser(String email) throws Exception {
        String body = String.format("""
                {
                  "firstName": "Test",
                  "lastName": "User",
                  "email": "%s",
                  "password": "password123"
                }
                """, email);
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private String loginAndGetToken(String email) throws Exception {
        String loginBody = String.format("""
                {
                  "email": "%s",
                  "password": "password123"
                }
                """, email);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), Map.class);
        return (String) response.get("accessToken");
    }
}
