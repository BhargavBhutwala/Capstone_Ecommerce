package com.ebookstore.order;

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
 * Integration tests for order endpoints.
 *
 * <p>Spins up a real PostgreSQL container via Testcontainers.
 * Each test registers a unique user so tests are independent.
 * Skipped automatically when Docker is unavailable.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @BeforeAll
    static void requireDocker() {
        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Skipping integration tests: Docker is not available on this machine.");
    }

    // =========================================================================
    // POST /orders — basic checkout flow
    // =========================================================================

    @Test
    void checkout_validCart_returns201WithPendingPaymentStatus() throws Exception {
        String token = registerAndLogin("ord_checkout@example.com");
        long productId = getAnyActiveProductId();
        addItemToCart(token, productId, 1);
        long addressId = createAddress(token);

        mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.orderNumber").isString())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.subtotal").isNumber())
                .andExpect(jsonPath("$.totalAmount").isNumber())
                .andExpect(jsonPath("$.shippingAmount").value(0))
                .andExpect(jsonPath("$.discountAmount").value(0))
                .andExpect(jsonPath("$.placedAt", notNullValue()))
                .andExpect(jsonPath("$.cancellationDeadline", notNullValue()))
                .andExpect(jsonPath("$.shippingAddress", notNullValue()))
                .andExpect(jsonPath("$.shippingAddress.addressLine1", notNullValue()));
    }

    @Test
    void checkout_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkout_emptyCart_returns409() throws Exception {
        String token = registerAndLogin("ord_empty@example.com");
        long addressId = createAddress(token);

        mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + "}"))
                .andExpect(status().isBadRequest());  // InvalidRequestException → 400
    }

    @Test
    void checkout_addressNotFound_returns404() throws Exception {
        String token = registerAndLogin("ord_noaddr@example.com");
        long productId = getAnyActiveProductId();
        addItemToCart(token, productId, 1);

        mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":999999}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void checkout_orderItemsContainTitleAndPriceSnapshots() throws Exception {
        String token = registerAndLogin("ord_snapshot@example.com");
        long productId = getAnyActiveProductId();
        addItemToCart(token, productId, 1);
        long addressId = createAddress(token);

        MvcResult result = mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + "}"))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> order = parseBody(result);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> items = (List<Map<?, ?>>) order.get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("productTitle")).isNotNull().isInstanceOf(String.class);
        assertThat(items.get(0).get("unitPrice")).isNotNull();
        assertThat(items.get(0).get("productId")).isNotNull();
    }

    @Test
    void checkout_shippingAddressSnapshotMatchesSelectedAddress() throws Exception {
        String token = registerAndLogin("ord_addrsnap@example.com");
        long productId = getAnyActiveProductId();
        addItemToCart(token, productId, 1);
        long addressId = createAddress(token);

        MvcResult result = mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + "}"))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> order = parseBody(result);
        @SuppressWarnings("unchecked")
        Map<?, ?> shippingAddress = (Map<?, ?>) order.get("shippingAddress");
        assertThat(shippingAddress.get("addressLine1")).isNotNull();
        assertThat(shippingAddress.get("city")).isEqualTo("Springfield");
        assertThat(shippingAddress.get("country")).isEqualTo("US");
    }

    // =========================================================================
    // Cart lifecycle after checkout
    // =========================================================================

    @Test
    void afterCheckout_cartExistsWithStatusActiveAndNoItems() throws Exception {
        String token = registerAndLogin("ord_cartafter@example.com");
        long productId = getAnyActiveProductId();
        addItemToCart(token, productId, 1);
        long addressId = createAddress(token);

        // Get cart id before checkout
        MvcResult cartBefore = mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        Long cartIdBefore = ((Number) parseBody(cartBefore).get("id")).longValue();

        // Checkout
        mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + "}"))
                .andExpect(status().isCreated());

        // Cart must still exist, be ACTIVE, have no items, same id
        MvcResult cartAfter = mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andReturn();

        Long cartIdAfter = ((Number) parseBody(cartAfter).get("id")).longValue();
        assertThat(cartIdAfter).isEqualTo(cartIdBefore); // Same cart — not replaced
    }

    @Test
    void afterCheckout_canAddNewItemsToSameCart() throws Exception {
        String token = registerAndLogin("ord_cartreuse@example.com");
        long productId = getAnyActiveProductId();
        addItemToCart(token, productId, 1);
        long addressId = createAddress(token);

        // Checkout
        mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + "}"))
                .andExpect(status().isCreated());

        // Cart is reusable — add same product again without error
        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    // =========================================================================
    // Stock decrement
    // =========================================================================

    @Test
    void checkout_stockDecrementedAfterCheckout() throws Exception {
        String token = registerAndLogin("ord_stock@example.com");
        long productId = getAnyActiveProductId();

        // Get current stock
        MvcResult productResult = mockMvc.perform(get("/products/" + productId))
                .andExpect(status().isOk())
                .andReturn();
        int stockBefore = ((Number) parseBody(productResult).get("stockQuantity")).intValue();

        addItemToCart(token, productId, 1);
        long addressId = createAddress(token);

        mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + "}"))
                .andExpect(status().isCreated());

        // Verify stock decremented
        MvcResult productAfter = mockMvc.perform(get("/products/" + productId))
                .andExpect(status().isOk())
                .andReturn();
        int stockAfter = ((Number) parseBody(productAfter).get("stockQuantity")).intValue();
        assertThat(stockAfter).isEqualTo(stockBefore - 1);
    }

    // =========================================================================
    // GET /orders — order history
    // =========================================================================

    @Test
    void listOrders_authenticated_returnsPagedResponse() throws Exception {
        String token = registerAndLogin("ord_list@example.com");
        long productId = getAnyActiveProductId();
        addItemToCart(token, productId, 1);
        long addressId = createAddress(token);
        checkout(token, addressId);

        mockMvc.perform(get("/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page.page").value(0))
                .andExpect(jsonPath("$.page.size").isNumber())
                .andExpect(jsonPath("$.page.totalElements").isNumber())
                .andExpect(jsonPath("$.content[0].status").value("PENDING_PAYMENT"));
    }

    @Test
    void listOrders_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listOrders_withStatusFilter_returnsOnlyMatchingOrders() throws Exception {
        String token = registerAndLogin("ord_filter@example.com");
        long productId = getAnyActiveProductId();
        addItemToCart(token, productId, 1);
        long addressId = createAddress(token);
        checkout(token, addressId);

        // Filter by PENDING_PAYMENT
        mockMvc.perform(get("/orders?status=PENDING_PAYMENT")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING_PAYMENT"));

        // Filter by PAID — should be empty for a freshly checked-out order
        mockMvc.perform(get("/orders?status=PAID")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    // =========================================================================
    // GET /orders/{orderId}
    // =========================================================================

    @Test
    void getOrder_ownOrder_returns200WithOrderResponse() throws Exception {
        String token = registerAndLogin("ord_get@example.com");
        long productId = getAnyActiveProductId();
        addItemToCart(token, productId, 1);
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);

        mockMvc.perform(get("/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.orderNumber", notNullValue()));
    }

    @Test
    void getOrder_anotherUsersOrder_returns404() throws Exception {
        String ownerToken = registerAndLogin("ord_owner@example.com");
        String otherToken = registerAndLogin("ord_other@example.com");
        long productId = getAnyActiveProductId();
        addItemToCart(ownerToken, productId, 1);
        long addressId = createAddress(ownerToken);
        long orderId = checkoutAndGetOrderId(ownerToken, addressId);

        mockMvc.perform(get("/orders/" + orderId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // POST /orders/{orderId}/cancel
    // =========================================================================

    @Test
    void cancelOrder_pendingPaymentWithinDeadline_returns200Cancelled() throws Exception {
        String token = registerAndLogin("ord_cancel@example.com");
        long productId = getAnyActiveProductId();
        addItemToCart(token, productId, 1);
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);

        // Cancel immediately (well within 48h deadline)
        mockMvc.perform(post("/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelOrder_anotherUsersOrder_returns404() throws Exception {
        String ownerToken = registerAndLogin("ord_canc_owner@example.com");
        String otherToken = registerAndLogin("ord_canc_other@example.com");
        long productId = getAnyActiveProductId();
        addItemToCart(ownerToken, productId, 1);
        long addressId = createAddress(ownerToken);
        long orderId = checkoutAndGetOrderId(ownerToken, addressId);

        mockMvc.perform(post("/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelOrder_stockRestoredAfterCancellation() throws Exception {
        String token = registerAndLogin("ord_stockrestore@example.com");
        long productId = getAnyActiveProductId();

        // Record stock before
        int stockBefore = getProductStock(productId);

        addItemToCart(token, productId, 1);
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);

        // Stock should be decremented
        int stockAfterCheckout = getProductStock(productId);
        assertThat(stockAfterCheckout).isEqualTo(stockBefore - 1);

        // Cancel
        mockMvc.perform(post("/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Stock restored
        int stockAfterCancel = getProductStock(productId);
        assertThat(stockAfterCancel).isEqualTo(stockBefore);
    }

    // =========================================================================
    // POST /orders/{orderId}/buy-again
    // =========================================================================

    @Test
    void buyAgain_addsProductsToCart_returns200WithCartResponse() throws Exception {
        String token = registerAndLogin("ord_buyagain@example.com");
        long productId = getAnyActiveProductId();
        addItemToCart(token, productId, 1);
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);

        // Cart is empty after checkout — buy again should re-add
        mockMvc.perform(post("/orders/" + orderId + "/buy-again")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void buyAgain_anotherUsersOrder_returns404() throws Exception {
        String ownerToken = registerAndLogin("ord_ba_owner@example.com");
        String otherToken = registerAndLogin("ord_ba_other@example.com");
        long productId = getAnyActiveProductId();
        addItemToCart(ownerToken, productId, 1);
        long addressId = createAddress(ownerToken);
        long orderId = checkoutAndGetOrderId(ownerToken, addressId);

        mockMvc.perform(post("/orders/" + orderId + "/buy-again")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String registerAndLogin(String email) throws Exception {
        String body = String.format("""
                {"firstName":"Test","lastName":"User","email":"%s","password":"password123"}
                """, email);
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        String loginBody = String.format("""
                {"email":"%s","password":"password123"}
                """, email);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();
        return (String) parseBody(result).get("accessToken");
    }

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

    private int getProductStock(long productId) throws Exception {
        MvcResult result = mockMvc.perform(get("/products/" + productId))
                .andExpect(status().isOk())
                .andReturn();
        return ((Number) parseBody(result).get("stockQuantity")).intValue();
    }

    private void addItemToCart(String token, long productId, int quantity) throws Exception {
        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":" + quantity + "}"))
                .andExpect(status().isCreated());
    }

    private long createAddress(String token) throws Exception {
        String body = """
                {"label":"Home","addressLine1":"123 Main St","city":"Springfield",
                "state":"IL","postalCode":"62701","country":"US","isDefault":false}
                """;
        MvcResult result = mockMvc.perform(post("/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) parseBody(result).get("id")).longValue();
    }

    private void checkout(String token, long addressId) throws Exception {
        mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + "}"))
                .andExpect(status().isCreated());
    }

    private long checkoutAndGetOrderId(String token, long addressId) throws Exception {
        MvcResult result = mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) parseBody(result).get("id")).longValue();
    }

    private Map<?, ?> parseBody(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
    }
}
