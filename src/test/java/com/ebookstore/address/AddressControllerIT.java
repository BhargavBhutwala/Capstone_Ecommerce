package com.ebookstore.address;

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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for address CRUD endpoints.
 *
 * <p>Spins up a real PostgreSQL container via Testcontainers.
 * Skipped automatically when Docker is unavailable.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AddressControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @BeforeAll
    static void requireDocker() {
        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Skipping integration tests: Docker is not available on this machine.");
    }

    // =========================================================================
    // GET /addresses — list
    // =========================================================================

    @Test
    void listAddresses_withValidToken_returnsOwnAddresses() throws Exception {
        String email = "addr_list@example.com";
        String token = registerAndLogin(email);

        // No addresses yet
        mockMvc.perform(get("/addresses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Create one
        createAddress(token, "123 A St", "Home");

        mockMvc.perform(get("/addresses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].addressLine1").value("123 A St"));
    }

    @Test
    void listAddresses_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/addresses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // =========================================================================
    // POST /addresses — create
    // =========================================================================

    @Test
    void createAddress_validRequest_returns201() throws Exception {
        String token = registerAndLogin("addr_create@example.com");

        mockMvc.perform(post("/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildAddressJson("456 Oak Ave", "Office")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.addressLine1").value("456 Oak Ave"))
                .andExpect(jsonPath("$.label").value("Office"))
                .andExpect(jsonPath("$.city").value("Springfield"))
                .andExpect(jsonPath("$.state").value("IL"))
                .andExpect(jsonPath("$.postalCode").value("62701"))
                .andExpect(jsonPath("$.country").value("US"));
    }

    @Test
    void createAddress_missingRequiredField_returns400WithFieldErrors() throws Exception {
        String token = registerAndLogin("addr_invalid@example.com");

        String body = """
                {
                  "label": "Home",
                  "city": "Springfield",
                  "state": "IL",
                  "postalCode": "62701",
                  "country": "US"
                }
                """;

        mockMvc.perform(post("/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", notNullValue()));
    }

    // =========================================================================
    // PUT /addresses/{addressId} — update
    // =========================================================================

    @Test
    void updateAddress_ownAddress_returns200WithUpdatedData() throws Exception {
        String token = registerAndLogin("addr_update@example.com");
        Long addressId = createAddress(token, "789 Pine St", "Home");

        String updated = buildAddressJson("999 Updated Rd", "Updated");

        mockMvc.perform(put("/addresses/" + addressId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updated))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressLine1").value("999 Updated Rd"))
                .andExpect(jsonPath("$.label").value("Updated"));
    }

    @Test
    void updateAddress_anotherUsersAddress_returns404() throws Exception {
        String ownerToken = registerAndLogin("addr_owner@example.com");
        Long addressId = createAddress(ownerToken, "100 Owner St", "Home");

        String otherToken = registerAndLogin("addr_other@example.com");

        mockMvc.perform(put("/addresses/" + addressId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildAddressJson("100 Owner St", "Home")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // =========================================================================
    // DELETE /addresses/{addressId} — delete
    // =========================================================================

    @Test
    void deleteAddress_ownAddress_returns204() throws Exception {
        String token = registerAndLogin("addr_delete@example.com");
        Long addressId = createAddress(token, "555 Delete Me", "Temp");

        mockMvc.perform(delete("/addresses/" + addressId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verify it's gone
        mockMvc.perform(get("/addresses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deleteAddress_anotherUsersAddress_returns404() throws Exception {
        String ownerToken = registerAndLogin("addr_del_owner@example.com");
        Long addressId = createAddress(ownerToken, "200 Owner St", "Home");

        String otherToken = registerAndLogin("addr_del_other@example.com");

        mockMvc.perform(delete("/addresses/" + addressId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String registerAndLogin(String email) throws Exception {
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

    private Long createAddress(String token, String addressLine1, String label) throws Exception {
        MvcResult result = mockMvc.perform(post("/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildAddressJson(addressLine1, label)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), Map.class);
        return ((Number) response.get("id")).longValue();
    }

    private String buildAddressJson(String addressLine1, String label) {
        return String.format("""
                {
                  "label": "%s",
                  "addressLine1": "%s",
                  "city": "Springfield",
                  "state": "IL",
                  "postalCode": "62701",
                  "country": "US",
                  "isDefault": false
                }
                """, label, addressLine1);
    }
}
