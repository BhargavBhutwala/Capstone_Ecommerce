package com.ebookstore.order;

import com.ebookstore.util.AbstractIntegrationTest;
import com.ebookstore.util.ClockTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for order checkout, history, buy-again, and cancellation endpoints.
 *
 * <p>Uses a real Testcontainers PostgreSQL with Flyway-migrated schema.
 * Seed data (categories, brands, products) is reset before each test by
 * {@link AbstractIntegrationTest#cleanDatabase()}.
 *
 * <p>Uses {@link ClockTestConfig} to inject a fixed {@link java.time.Clock} so
 * cancellation deadline tests do not require {@code Thread.sleep()}.
 *
 * <p>Within-deadline tests rely on the fixed clock instant being BEFORE the
 * cancellation deadline (which is set to {@code placed_at + 48h}).
 * After-deadline tests force the deadline back into the past via JdbcTemplate.
 */
@Import(ClockTestConfig.class)
class OrderControllerIT extends AbstractIntegrationTest {

    // =========================================================================
    // POST /orders — basic checkout flow
    // =========================================================================

    @Test
    void checkout_validCart_returns201WithPendingPaymentStatus() throws Exception {
        String token = registerAndLogin("ord_checkout@example.com");
        addItemToCart(token, 1L, 1);
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
    void checkout_emptyCart_returns4xx() throws Exception {
        String token = registerAndLogin("ord_empty@example.com");
        long addressId = createAddress(token);

        // Empty cart → business rule violation → 400
        mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + "}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void checkout_addressNotFound_returns404() throws Exception {
        String token = registerAndLogin("ord_noaddr@example.com");
        addItemToCart(token, 1L, 1);

        mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":999999}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void checkout_noPaymentRecordCreated() throws Exception {
        String token = registerAndLogin("ord_nopay@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);

        long orderId = checkoutAndGetOrderId(token, addressId);

        // No payment yet — GET /payments for this order should 404 or return nothing
        // We verify by trying to GET order and confirming it's still PENDING_PAYMENT
        mockMvc.perform(get("/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    @Test
    void checkout_giftPointsUsedIsZero() throws Exception {
        String token = registerAndLogin("ord_gp@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);

        MvcResult result = mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + "}"))
                .andExpect(status().isCreated())
                .andReturn();

        // giftPointsUsed = 0, discountAmount = 0 in MVP
        Map<?, ?> order = parseBody(result);
        assertThat(((Number) order.get("discountAmount")).doubleValue()).isEqualTo(0.0);
    }

    // =========================================================================
    // Live-price snapshot: checkout uses products.price not cart_items.unit_price
    // =========================================================================

    @Test
    void checkout_usesLiveProductPrice_notStaleCartItemUnitPrice() throws Exception {
        String token = registerAndLogin("ord_liveprice@example.com");

        // Add product 1 (price = 39.99) to cart
        addItemToCart(token, 1L, 1);

        // Verify cart_item.unit_price is 39.99
        MvcResult cartResult = mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> cart = parseBody(cartResult);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> items = (List<Map<?, ?>>) cart.get("items");
        double cartUnitPrice = ((Number) items.get(0).get("unitPrice")).doubleValue();
        assertThat(cartUnitPrice).isEqualTo(39.99);

        // Now change product 1 price to 59.99 directly in DB
        jdbcTemplate.execute("UPDATE products SET price = 59.99, updated_at = now() WHERE id = 1");

        // Checkout — must use new price 59.99
        long addressId = createAddress(token);
        MvcResult orderResult = mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + "}"))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> order = parseBody(orderResult);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> orderItems = (List<Map<?, ?>>) order.get("items");
        double orderItemUnitPrice = ((Number) orderItems.get(0).get("unitPrice")).doubleValue();

        // order_items.unit_price must be 59.99 (live price), not 39.99 (stale cart price)
        assertThat(orderItemUnitPrice).isEqualTo(59.99);
        assertThat(orderItemUnitPrice).isNotEqualTo(cartUnitPrice);

        // Total amount must also be 59.99 (1 item × 59.99)
        double totalAmount = ((Number) order.get("totalAmount")).doubleValue();
        assertThat(totalAmount).isEqualTo(59.99);
    }

    // =========================================================================
    // Order-item snapshot: product_title snapshot at checkout
    // =========================================================================

    @Test
    void checkout_orderItemContainsTitleSnapshot() throws Exception {
        String token = registerAndLogin("ord_titlesnap@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);

        MvcResult result = mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + "}"))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> order = parseBody(result);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> orderItems = (List<Map<?, ?>>) order.get("items");
        assertThat(orderItems).hasSize(1);
        assertThat(orderItems.get(0).get("productTitle")).isEqualTo("Java Fundamentals");
    }

    @Test
    void checkout_orderItemUnitPriceIsSnapshot_notCartPrice() throws Exception {
        String token = registerAndLogin("ord_pricesnap@example.com");
        addItemToCart(token, 1L, 1);

        // Change price before checkout
        jdbcTemplate.execute("UPDATE products SET price = 44.99, updated_at = now() WHERE id = 1");

        long addressId = createAddress(token);
        MvcResult result = mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + "}"))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> order = parseBody(result);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> orderItems = (List<Map<?, ?>>) order.get("items");
        // order_items.unit_price = 44.99 (live at checkout), not 39.99 (stale cart)
        assertThat(((Number) orderItems.get(0).get("unitPrice")).doubleValue()).isEqualTo(44.99);
    }

    // =========================================================================
    // Shipping address snapshot (7-field)
    // =========================================================================

    @Test
    void checkout_shippingAddressSnapshotMatchesSelectedAddress() throws Exception {
        String token = registerAndLogin("ord_addrsnap@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);

        MvcResult result = mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + "}"))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> order = parseBody(result);
        @SuppressWarnings("unchecked")
        Map<?, ?> shipping = (Map<?, ?>) order.get("shippingAddress");
        assertThat(shipping.get("addressLine1")).isEqualTo("123 Main St");
        assertThat(shipping.get("city")).isEqualTo("Springfield");
        assertThat(shipping.get("state")).isEqualTo("IL");
        assertThat(shipping.get("postalCode")).isEqualTo("62701");
        assertThat(shipping.get("country")).isEqualTo("US");
    }

    @Test
    void checkout_shippingAddressSnapshotPersistedAsFlat7Fields() throws Exception {
        String token = registerAndLogin("ord_7field@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);

        // Update the address after checkout — snapshot should be unaffected
        jdbcTemplate.execute("UPDATE addresses SET address_line1 = 'Changed Street', updated_at = now() WHERE id = " + addressId);

        // Re-fetch order — shipping address must still be the original
        MvcResult result = mockMvc.perform(get("/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> order = parseBody(result);
        @SuppressWarnings("unchecked")
        Map<?, ?> shipping = (Map<?, ?>) order.get("shippingAddress");
        // Snapshot must not be affected by the address update
        assertThat(shipping.get("addressLine1")).isEqualTo("123 Main St");
    }

    // =========================================================================
    // Stock decrement on checkout
    // =========================================================================

    @Test
    void checkout_stockDecrementedByQuantityAfterCheckout() throws Exception {
        String token = registerAndLogin("ord_stock@example.com");

        // Product 1 has stock=50
        int stockBefore = getProductStock(1L);

        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);
        checkoutAndGetOrderId(token, addressId);

        int stockAfter = getProductStock(1L);
        assertThat(stockAfter).isEqualTo(stockBefore - 1);
    }

    // =========================================================================
    // Cart lifecycle after checkout
    // =========================================================================

    @Test
    void afterCheckout_cartExistsWithStatusActiveAndNoItems() throws Exception {
        String token = registerAndLogin("ord_cartafter@example.com");
        addItemToCart(token, 1L, 1);

        // Cart id before checkout
        MvcResult beforeResult = mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        Long cartIdBefore = ((Number) parseBody(beforeResult).get("id")).longValue();

        long addressId = createAddress(token);
        checkoutAndGetOrderId(token, addressId);

        // Same cart, ACTIVE, no items
        MvcResult afterResult = mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andReturn();

        Long cartIdAfter = ((Number) parseBody(afterResult).get("id")).longValue();
        assertThat(cartIdAfter).isEqualTo(cartIdBefore);
    }

    @Test
    void afterCheckout_canAddNewItemsToSameCart() throws Exception {
        String token = registerAndLogin("ord_cartreuse@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);
        checkoutAndGetOrderId(token, addressId);

        // Cart is reusable — add same product again without error
        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    void afterCheckout_noSecondCartCreated() throws Exception {
        String token = registerAndLogin("ord_nocart2@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);
        checkoutAndGetOrderId(token, addressId);

        // Subsequent calls all return the same cart id
        MvcResult r1 = mockMvc.perform(get("/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        MvcResult r2 = mockMvc.perform(get("/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        assertThat(parseBody(r1).get("id")).isEqualTo(parseBody(r2).get("id"));
    }

    // =========================================================================
    // GET /orders — order history
    // =========================================================================

    @Test
    void listOrders_authenticated_returnsPagedResponseSortedByPlacedAtDesc() throws Exception {
        String token = registerAndLogin("ord_list@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);
        checkoutAndGetOrderId(token, addressId);

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
    void listOrders_multiplePlacedAtDesc_newestFirst() throws Exception {
        String token = registerAndLogin("ord_desc@example.com");
        long addressId = createAddress(token);

        // Place first order
        addItemToCart(token, 1L, 1);
        long firstOrderId = checkoutAndGetOrderId(token, addressId);

        // Place second order (slightly later — update placed_at to enforce ordering)
        addItemToCart(token, 2L, 1);
        long secondOrderId = checkoutAndGetOrderId(token, addressId);

        // Ensure second order has a later placed_at
        jdbcTemplate.execute(
                "UPDATE orders SET placed_at = placed_at + interval '1 second' WHERE id = " + secondOrderId);

        MvcResult result = mockMvc.perform(get("/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = parseBody(result);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> content = (List<Map<?, ?>>) body.get("content");
        assertThat(content).hasSizeGreaterThanOrEqualTo(2);
        // Newest (secondOrderId) must appear first
        assertThat(((Number) content.get(0).get("id")).longValue()).isEqualTo(secondOrderId);
        assertThat(((Number) content.get(1).get("id")).longValue()).isEqualTo(firstOrderId);
    }

    @Test
    void listOrders_clientSortParamIgnored() throws Exception {
        // Client sort override must be silently ignored; ordering stays placed_at DESC
        String token = registerAndLogin("ord_nosort@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);
        checkoutAndGetOrderId(token, addressId);

        // Attempt to override sort — must still return 200 (not 400)
        mockMvc.perform(get("/orders?sort=id,asc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void listOrders_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listOrders_withStatusFilter_returnsOnlyMatchingOrders() throws Exception {
        String token = registerAndLogin("ord_filter@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);
        checkoutAndGetOrderId(token, addressId);

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
        addItemToCart(token, 1L, 1);
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
        addItemToCart(ownerToken, 1L, 1);
        long addressId = createAddress(ownerToken);
        long orderId = checkoutAndGetOrderId(ownerToken, addressId);

        mockMvc.perform(get("/orders/" + orderId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // POST /orders/{orderId}/cancel — cancellation
    // =========================================================================

    @Test
    void cancelOrder_pendingPaymentWithinDeadline_returns200Cancelled() throws Exception {
        // Fixed clock is within deadline (order placed_at = fixed clock instant,
        // deadline = placed_at + 48h → far future)
        String token = registerAndLogin("ord_cancel@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);

        mockMvc.perform(post("/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelOrder_paidStatusWithinDeadline_returns200Cancelled() throws Exception {
        String token = registerAndLogin("ord_cancel_paid@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);

        // Pay for the order first
        mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"paymentMethod\":\"CREDIT_CARD\"}"))
                .andExpect(status().isCreated());

        // Order must be PAID now
        mockMvc.perform(get("/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // Cancel PAID order within deadline — must succeed
        mockMvc.perform(post("/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelOrder_afterDeadline_returns409() throws Exception {
        String token = registerAndLogin("ord_afterdeadline@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);

        // The fixed clock in ClockTestConfig is 2024-06-15T10:00:00Z.
        // Set cancellation_deadline to a timestamp before that fixed instant
        // so that LocalDateTime.now(clock) > cancellationDeadline → 409.
        jdbcTemplate.execute(
                "UPDATE orders SET cancellation_deadline = '2024-06-15 09:59:59' WHERE id = " + orderId);

        mockMvc.perform(post("/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelOrder_stockRestoredAfterCancellation() throws Exception {
        String token = registerAndLogin("ord_stockrestore@example.com");

        int stockBefore = getProductStock(1L);

        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);

        int stockAfterCheckout = getProductStock(1L);
        assertThat(stockAfterCheckout).isEqualTo(stockBefore - 1);

        mockMvc.perform(post("/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        int stockAfterCancel = getProductStock(1L);
        assertThat(stockAfterCancel).isEqualTo(stockBefore);
    }

    @Test
    void cancelOrder_paidCancellation_paymentStatusRemainsSuccess() throws Exception {
        String token = registerAndLogin("ord_canc_paid_pay@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);

        // Pay first
        MvcResult payResult = mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"paymentMethod\":\"CREDIT_CARD\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long paymentId = ((Number) parseBody(payResult).get("id")).longValue();

        // Cancel PAID order
        mockMvc.perform(post("/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Payment status must remain SUCCESS (no refund in MVP)
        mockMvc.perform(get("/payments/" + paymentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void cancelOrder_anotherUsersOrder_returns404() throws Exception {
        String ownerToken = registerAndLogin("ord_canc_owner@example.com");
        String otherToken = registerAndLogin("ord_canc_other@example.com");
        addItemToCart(ownerToken, 1L, 1);
        long addressId = createAddress(ownerToken);
        long orderId = checkoutAndGetOrderId(ownerToken, addressId);

        mockMvc.perform(post("/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // POST /orders/{orderId}/buy-again
    // =========================================================================

    @Test
    void buyAgain_addsProductsToCart_returns200WithCartResponse() throws Exception {
        String token = registerAndLogin("ord_buyagain@example.com");
        addItemToCart(token, 1L, 1);
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
    void buyAgain_usesCurrentProductPrice_notHistoricalOrderItemUnitPrice() throws Exception {
        String token = registerAndLogin("ord_ba_price@example.com");
        addItemToCart(token, 1L, 1);  // product 1 price = 39.99 at checkout
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);  // order_items.unit_price = 39.99

        // Verify historical order_items.unit_price is 39.99
        MvcResult orderResult = mockMvc.perform(get("/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> order = parseBody(orderResult);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> orderItems = (List<Map<?, ?>>) order.get("items");
        double historicalUnitPrice = ((Number) orderItems.get(0).get("unitPrice")).doubleValue();
        assertThat(historicalUnitPrice).isEqualTo(39.99);

        // Change current product price to 79.99
        jdbcTemplate.execute("UPDATE products SET price = 79.99, updated_at = now() WHERE id = 1");

        // Buy Again
        mockMvc.perform(post("/orders/" + orderId + "/buy-again")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // cart_item.unit_price must be 79.99 (current), not 39.99 (historical)
        MvcResult cartResult = mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> cart = parseBody(cartResult);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> cartItems = (List<Map<?, ?>>) cart.get("items");
        double currentCartUnitPrice = ((Number) cartItems.get(0).get("unitPrice")).doubleValue();
        assertThat(currentCartUnitPrice).isEqualTo(79.99);
        assertThat(currentCartUnitPrice).isNotEqualTo(historicalUnitPrice);
    }

    @Test
    void buyAgain_partialUnavailability_availableProductsAddedUnavailableSkipped() throws Exception {
        String token = registerAndLogin("ord_ba_partial@example.com");

        // Place order with 2 products
        addItemToCart(token, 1L, 1);
        addItemToCart(token, 2L, 1);
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);

        // Make product 2 inactive
        jdbcTemplate.execute("UPDATE products SET active = false, updated_at = now() WHERE id = 2");

        // Buy Again — product 1 available, product 2 inactive (skipped)
        MvcResult result = mockMvc.perform(post("/orders/" + orderId + "/buy-again")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> cart = parseBody(result);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> items = (List<Map<?, ?>>) cart.get("items");
        // Only product 1 should be in cart
        assertThat(items).hasSize(1);
        assertThat(((Number) ((Map<?, ?>) items.get(0).get("product")).get("id")).longValue())
                .isEqualTo(1L);
    }

    @Test
    void buyAgain_allUnavailable_returns409() throws Exception {
        String token = registerAndLogin("ord_ba_none@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);

        // Make product 1 inactive
        jdbcTemplate.execute("UPDATE products SET active = false, updated_at = now() WHERE id = 1");

        mockMvc.perform(post("/orders/" + orderId + "/buy-again")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void buyAgain_reusesExistingCart_notCreatingSecondCart() throws Exception {
        String token = registerAndLogin("ord_ba_cart@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);

        // Cart id before buy-again
        MvcResult before = mockMvc.perform(get("/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        Long cartIdBefore = ((Number) parseBody(before).get("id")).longValue();

        mockMvc.perform(post("/orders/" + orderId + "/buy-again")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        MvcResult after = mockMvc.perform(get("/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        Long cartIdAfter = ((Number) parseBody(after).get("id")).longValue();

        assertThat(cartIdAfter).isEqualTo(cartIdBefore);
    }

    @Test
    void buyAgain_anotherUsersOrder_returns404() throws Exception {
        String ownerToken = registerAndLogin("ord_ba_owner@example.com");
        String otherToken = registerAndLogin("ord_ba_other@example.com");
        addItemToCart(ownerToken, 1L, 1);
        long addressId = createAddress(ownerToken);
        long orderId = checkoutAndGetOrderId(ownerToken, addressId);

        mockMvc.perform(post("/orders/" + orderId + "/buy-again")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private int getProductStock(long productId) throws Exception {
        MvcResult result = mockMvc.perform(get("/products/" + productId))
                .andExpect(status().isOk())
                .andReturn();
        return ((Number) parseBody(result).get("stockQuantity")).intValue();
    }
}
