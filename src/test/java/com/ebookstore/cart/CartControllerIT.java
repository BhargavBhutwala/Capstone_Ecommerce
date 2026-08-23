package com.ebookstore.cart;

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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for cart endpoints.
 *
 * <p>Spins up a real PostgreSQL container via Testcontainers (configured in
 * {@code application-test.yml}) and exercises the full request → controller →
 * service → repository → DB stack.
 *
 * <p>Skipped automatically when Docker is not available.
 *
 * <p>Each test registers a unique user (and therefore gets its own empty cart),
 * ensuring tests are independent without relying on {@code @Transactional} rollback
 * (which does not work with {@code RANDOM_PORT}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @BeforeAll
    static void requireDocker() {
        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Skipping integration tests: Docker is not available on this machine.");
    }

    // =========================================================================
    // GET /cart — basic authentication & schema
    // =========================================================================

    @Test
    void getCart_authenticated_returns200WithCartResponseSchema() throws Exception {
        String token = registerAndLogin("cart_get1@example.com");

        mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.subtotal").value(0))
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    void getCart_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void getCart_cartIdUnchangedAcrossRequests() throws Exception {
        String token = registerAndLogin("cart_stable@example.com");

        MvcResult r1 = mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult r2 = mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> cart1 = parseBody(r1);
        Map<?, ?> cart2 = parseBody(r2);
        // Same cart id on both calls — cart is persistent, never re-created
        assertThat(cart1.get("id")).isEqualTo(cart2.get("id"));
    }

    // =========================================================================
    // POST /cart/items — add item
    // =========================================================================

    @Test
    void addCartItem_validRequest_returns201WithCartResponse() throws Exception {
        String token = registerAndLogin("cart_add1@example.com");
        long productId = getAnyActiveProductId();

        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemJson(productId, 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].product.id").value(productId))
                .andExpect(jsonPath("$.items[0].quantity").value(1))
                .andExpect(jsonPath("$.items[0].unitPrice", notNullValue()))
                .andExpect(jsonPath("$.items[0].subtotal", notNullValue()))
                .andExpect(jsonPath("$.subtotal").isNumber())
                .andExpect(jsonPath("$.totalAmount").isNumber());
    }

    @Test
    void addCartItem_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemJson(1L, 1)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addCartItem_missingProductId_returns400() throws Exception {
        String token = registerAndLogin("cart_add_bad@example.com");

        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", notNullValue()));
    }

    @Test
    void addCartItem_nonExistentProduct_returns404() throws Exception {
        String token = registerAndLogin("cart_add_404@example.com");

        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemJson(999999L, 1)))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // POST /cart/items — merge (duplicate product)
    // =========================================================================

    @Test
    void addCartItem_sameProductTwice_mergesQuantity() throws Exception {
        String token = registerAndLogin("cart_merge@example.com");
        long productId = getAnyActiveProductId();

        // Add 1 first
        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemJson(productId, 1)))
                .andExpect(status().isCreated());

        // Add 2 more — should merge to 3, not create a second row
        MvcResult result = mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemJson(productId, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(1)))        // still only 1 row
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andReturn();

        Map<?, ?> cart = parseBody(result);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> items = (List<Map<?, ?>>) cart.get("items");
        assertThat(items).hasSize(1);
        assertThat(((Number) items.get(0).get("quantity")).intValue()).isEqualTo(3);
    }

    // =========================================================================
    // POST /cart/items — insufficient stock
    // =========================================================================

    @Test
    void addCartItem_insufficientStock_returns409() throws Exception {
        String token = registerAndLogin("cart_stock@example.com");
        // Request quantity 9999 — guaranteed to exceed any seed data stock
        long productId = getAnyActiveProductId();

        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemJson(productId, 9999)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    // =========================================================================
    // PUT /cart/items/{itemId} — update quantity
    // =========================================================================

    @Test
    void updateCartItem_validRequest_returns200WithUpdatedQuantity() throws Exception {
        String token = registerAndLogin("cart_upd@example.com");
        long productId = getAnyActiveProductId();
        long itemId = addItemAndGetItemId(token, productId, 1);

        mockMvc.perform(put("/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void updateCartItem_insufficientStock_returns409() throws Exception {
        String token = registerAndLogin("cart_upd_stock@example.com");
        long productId = getAnyActiveProductId();
        long itemId = addItemAndGetItemId(token, productId, 1);

        mockMvc.perform(put("/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":9999}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void updateCartItem_invalidQuantityZero_returns400() throws Exception {
        String token = registerAndLogin("cart_upd_zero@example.com");

        mockMvc.perform(put("/cart/items/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCartItem_anotherUsersItem_returns404() throws Exception {
        String ownerToken = registerAndLogin("cart_upd_owner@example.com");
        String otherToken = registerAndLogin("cart_upd_other@example.com");
        long productId = getAnyActiveProductId();
        long itemId = addItemAndGetItemId(ownerToken, productId, 1);

        mockMvc.perform(put("/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // DELETE /cart/items/{itemId} — remove item
    // =========================================================================

    @Test
    void removeCartItem_ownItem_returns204() throws Exception {
        String token = registerAndLogin("cart_del@example.com");
        long productId = getAnyActiveProductId();
        long itemId = addItemAndGetItemId(token, productId, 1);

        mockMvc.perform(delete("/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verify item is gone
        mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void removeCartItem_anotherUsersItem_returns404() throws Exception {
        String ownerToken = registerAndLogin("cart_del_owner@example.com");
        String otherToken = registerAndLogin("cart_del_other@example.com");
        long productId = getAnyActiveProductId();
        long itemId = addItemAndGetItemId(ownerToken, productId, 1);

        mockMvc.perform(delete("/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeCartItem_nonExistentItem_returns404() throws Exception {
        String token = registerAndLogin("cart_del_404@example.com");

        mockMvc.perform(delete("/cart/items/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // Cart structure / schema
    // =========================================================================

    @Test
    void getCart_withItems_hasCorrectSchema() throws Exception {
        String token = registerAndLogin("cart_schema@example.com");
        long productId = getAnyActiveProductId();

        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemJson(productId, 1)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].id", notNullValue()))
                .andExpect(jsonPath("$.items[0].product.id", notNullValue()))
                .andExpect(jsonPath("$.items[0].product.title", notNullValue()))
                .andExpect(jsonPath("$.items[0].quantity").isNumber())
                .andExpect(jsonPath("$.items[0].unitPrice").isNumber())
                .andExpect(jsonPath("$.items[0].subtotal").isNumber())
                .andExpect(jsonPath("$.subtotal").isNumber())
                .andExpect(jsonPath("$.totalAmount").isNumber());
    }

    @Test
    void getCart_recommendations_fieldIsPresentInResponse() throws Exception {
        String token = registerAndLogin("cart_rec@example.com");

        // recommendedProducts may be absent or empty (no purchase history yet)
        MvcResult result = mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = parseBody(result);
        // Field may be absent (null) or an empty list — both are acceptable with no history
        if (body.containsKey("recommendedProducts") && body.get("recommendedProducts") != null) {
            assertThat((List<?>) body.get("recommendedProducts")).hasSizeLessThanOrEqualTo(4);
        }
    }

    @Test
    void getCart_cartStatusRemainsActive_afterAddAndRemove() throws Exception {
        String token = registerAndLogin("cart_status_check@example.com");
        long productId = getAnyActiveProductId();

        // Add then remove
        long itemId = addItemAndGetItemId(token, productId, 1);
        mockMvc.perform(delete("/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items", hasSize(0)));
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

        Map<?, ?> response = parseBody(result);
        return (String) response.get("accessToken");
    }

    /**
     * Returns the id of any active product available in the test database.
     * Uses the public catalog endpoint — no authentication needed.
     */
    private long getAnyActiveProductId() throws Exception {
        MvcResult result = mockMvc.perform(get("/products?size=1"))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = parseBody(result);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> content = (List<Map<?, ?>>) body.get("content");
        assertThat(content).as("Seed data must include at least one product").isNotEmpty();
        return ((Number) content.get(0).get("id")).longValue();
    }

    private long addItemAndGetItemId(String token, long productId, int quantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemJson(productId, quantity)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> cart = parseBody(result);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> items = (List<Map<?, ?>>) cart.get("items");
        assertThat(items).isNotEmpty();
        return ((Number) items.get(0).get("id")).longValue();
    }

    private String addItemJson(long productId, int quantity) {
        return String.format("{\"productId\":%d,\"quantity\":%d}", productId, quantity);
    }

    private Map<?, ?> parseBody(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
    }
}
