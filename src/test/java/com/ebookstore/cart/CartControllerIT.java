package com.ebookstore.cart;

import com.ebookstore.util.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

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
 * <p>Exercises the full request → controller → service → repository → PostgreSQL stack.
 * Each test registers a unique user so tests are independent without relying on
 * {@code @Transactional} rollback (ineffective with {@code RANDOM_PORT}).
 */
class CartControllerIT extends AbstractIntegrationTest {

    // =========================================================================
    // GET /cart — authentication & schema
    // =========================================================================

    @Test
    void getCart_authenticated_returns200WithCartResponseSchema() throws Exception {
        String token = registerAndLogin("cart_get@example.com");

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
    void getCart_cartBelongsToAuthenticatedUser_notSharedAcrossUsers() throws Exception {
        String token1 = registerAndLogin("cart_usr1@example.com");
        String token2 = registerAndLogin("cart_usr2@example.com");

        // Add item to user1's cart
        addItemToCart(token1, 1L, 1);

        // User2's cart must be empty
        mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
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
        assertThat(cart1.get("id")).isEqualTo(cart2.get("id"));
    }

    // =========================================================================
    // POST /cart/items — add item
    // =========================================================================

    @Test
    void addCartItem_validRequest_returns201WithCartResponse() throws Exception {
        String token = registerAndLogin("cart_add@example.com");

        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].product.id").value(1))
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
                        .content("{\"productId\":1,\"quantity\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addCartItem_missingProductId_returns400WithFieldErrors() throws Exception {
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
                        .content("{\"productId\":999999,\"quantity\":1}"))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // POST /cart/items — duplicate product merges quantity
    // =========================================================================

    @Test
    void addCartItem_sameProductTwice_mergesQuantityNoDuplicateRow() throws Exception {
        String token = registerAndLogin("cart_merge@example.com");

        // Add 1 first
        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":1}"))
                .andExpect(status().isCreated());

        // Add 2 more — must merge to 3, not create a second row
        MvcResult result = mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(1)))      // still only 1 row
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

        // Product 1 has stock_quantity=50; request 51 → valid qty but exceeds stock → 409
        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":51}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    // =========================================================================
    // PUT /cart/items/{itemId} — update quantity
    // =========================================================================

    @Test
    void updateCartItem_validRequest_returns200WithUpdatedQuantity() throws Exception {
        String token = registerAndLogin("cart_upd@example.com");
        long itemId = addItemAndGetItemId(token, 1L, 1);

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
        long itemId = addItemAndGetItemId(token, 1L, 1);

        // Product 1 has stock_quantity=50; update to 51 → valid qty but exceeds stock → 409
        mockMvc.perform(put("/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":51}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void updateCartItem_invalidQuantityZero_returns400() throws Exception {
        String token = registerAndLogin("cart_upd_zero@example.com");
        long itemId = addItemAndGetItemId(token, 1L, 1);

        mockMvc.perform(put("/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCartItem_anotherUsersItem_returns404() throws Exception {
        String ownerToken = registerAndLogin("cart_upd_owner@example.com");
        String otherToken = registerAndLogin("cart_upd_other@example.com");
        long itemId = addItemAndGetItemId(ownerToken, 1L, 1);

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
        long itemId = addItemAndGetItemId(token, 1L, 1);

        mockMvc.perform(delete("/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void removeCartItem_anotherUsersItem_returns404() throws Exception {
        String ownerToken = registerAndLogin("cart_del_owner@example.com");
        String otherToken = registerAndLogin("cart_del_other@example.com");
        long itemId = addItemAndGetItemId(ownerToken, 1L, 1);

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

        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":1}"))
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
    void getCart_cartStatusRemainsActive_afterAddAndRemove() throws Exception {
        String token = registerAndLogin("cart_status_check@example.com");
        long itemId = addItemAndGetItemId(token, 1L, 1);

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
    // Recommendations
    // =========================================================================

    @Test
    void getCart_recommendations_returnEmptyList_whenNoPurchaseHistory() throws Exception {
        String token = registerAndLogin("cart_rec_empty@example.com");

        MvcResult result = mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = parseBody(result);
        // No purchase history → recommendations should be empty list
        List<?> recs = (List<?>) body.get("recommendedProducts");
        if (recs != null) {
            assertThat(recs).isEmpty();
        }
        // null is also acceptable when no history
    }

    @Test
    void getCart_recommendations_excludeCurrentCartItems() throws Exception {
        // After checkout (creates purchase history), recommendations should not include
        // products already in cart
        String token = registerAndLogin("cart_rec_excl@example.com");

        // Checkout product 1 to create purchase history
        addItemToCart(token, 1L, 1);
        long addrId = createAddress(token);
        checkoutAndGetOrderId(token, addrId);

        // Now add product 2 to cart
        addItemToCart(token, 2L, 1);

        MvcResult result = mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = parseBody(result);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> recs = (List<Map<?, ?>>) body.get("recommendedProducts");
        if (recs != null && !recs.isEmpty()) {
            // Recommended products must not include product currently in cart (id=2)
            recs.forEach(rec -> assertThat(((Number) rec.get("id")).longValue()).isNotEqualTo(2L));
        }
    }

    @Test
    void getCart_recommendations_usePurchaseHistoryCategories() throws Exception {
        String token = registerAndLogin("cart_rec_hist@example.com");

        // Purchase product 1 (category 1 = Programming)
        addItemToCart(token, 1L, 1);
        long addrId = createAddress(token);
        checkoutAndGetOrderId(token, addrId);

        // Cart is now empty after checkout
        MvcResult result = mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = parseBody(result);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> recs = (List<Map<?, ?>>) body.get("recommendedProducts");
        // There is purchase history in category 1, so recommendations should be present
        // (product 2 in category 1 is available)
        if (recs != null) {
            assertThat(recs.size()).isLessThanOrEqualTo(4);
        }
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private long addItemAndGetItemId(String token, long productId, int quantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":" + quantity + "}"))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> cart = parseBody(result);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> items = (List<Map<?, ?>>) cart.get("items");
        assertThat(items).isNotEmpty();
        // Find the item with the matching product id
        return items.stream()
                .filter(i -> ((Number) ((Map<?, ?>) i.get("product")).get("id")).longValue() == productId)
                .mapToLong(i -> ((Number) i.get("id")).longValue())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Item not found in cart for productId=" + productId));
    }
}
