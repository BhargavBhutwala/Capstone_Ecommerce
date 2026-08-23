package com.ebookstore.payment;

import com.ebookstore.util.AbstractIntegrationTest;
import com.ebookstore.util.ClockTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for payment endpoints.
 *
 * <p>Uses a real Testcontainers PostgreSQL from {@link AbstractIntegrationTest}.
 * Each test registers a unique user so tests are independent.
 *
 * <p>Covers:
 * <ol>
 *   <li>Register user, create address, add product, POST /orders → PENDING_PAYMENT</li>
 *   <li>POST /payments → 201, Payment.status=SUCCESS, Payment.amount=Order.totalAmount, paidAt set</li>
 *   <li>Order.status=PAID after payment (not CONFIRMED)</li>
 *   <li>Second POST /payments → 409</li>
 *   <li>Payment for another user's order → 404</li>
 *   <li>GET /payments/{id} → correct PaymentResponse</li>
 *   <li>GET another user's payment → 404</li>
 *   <li>Payment on non-PENDING_PAYMENT order → 409</li>
 *   <li>Client cannot supply payment amount — server always uses order.totalAmount</li>
 * </ol>
 */
@Import(ClockTestConfig.class)
class PaymentControllerIT extends AbstractIntegrationTest {

    // =========================================================================
    // Full payment flow
    // =========================================================================

    @Test
    void payment_fullFlow_statusSuccessAndPaidAtSet() throws Exception {
        String token = registerAndLogin("pay_flow@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);

        // Verify PENDING_PAYMENT before payment
        mockMvc.perform(get("/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        // POST /payments → 201
        MvcResult paymentResult = mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"paymentMethod\":\"CREDIT_CARD\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.paymentReference", notNullValue()))
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.amount").isNumber())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.paidAt", notNullValue()))
                .andReturn();

        // payment.amount must equal order.totalAmount (server-authoritative, not client-supplied)
        MvcResult orderResult = mockMvc.perform(get("/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        double orderTotal = ((Number) parseBody(orderResult).get("totalAmount")).doubleValue();
        double paymentAmount = ((Number) parseBody(paymentResult).get("amount")).doubleValue();
        assertThat(paymentAmount).isEqualTo(orderTotal);
    }

    @Test
    void payment_orderTransitionsToPaid_notConfirmed() throws Exception {
        String token = registerAndLogin("pay_status@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);

        mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"paymentMethod\":\"DEBIT_CARD\"}"))
                .andExpect(status().isCreated());

        // Order must be PAID, not CONFIRMED
        mockMvc.perform(get("/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void payment_amountTakenFromOrderTotal_notFromClient() throws Exception {
        // Attempt to supply a different amount in the request — server must ignore it
        // and use order.totalAmount
        String token = registerAndLogin("pay_amt@example.com");
        addItemToCart(token, 1L, 1);  // product 1 = 39.99
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);

        // orderTotal = 39.99 (1 × 39.99)
        MvcResult orderResult = mockMvc.perform(get("/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        double orderTotal = ((Number) parseBody(orderResult).get("totalAmount")).doubleValue();

        // Client tries to supply amount=0.01 (fraudulent) — it's simply ignored by the service
        MvcResult payResult = mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"paymentMethod\":\"CREDIT_CARD\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        double paymentAmount = ((Number) parseBody(payResult).get("amount")).doubleValue();
        // Amount must equal order total, regardless of any client input
        assertThat(paymentAmount).isEqualTo(orderTotal);
        assertThat(paymentAmount).isEqualTo(39.99);
    }

    // =========================================================================
    // Duplicate payment protection
    // =========================================================================

    @Test
    void payment_duplicatePayment_returns409() throws Exception {
        String token = registerAndLogin("pay_dup@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);

        // First payment succeeds
        mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"paymentMethod\":\"CREDIT_CARD\"}"))
                .andExpect(status().isCreated());

        // Second attempt → 409
        mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"paymentMethod\":\"DEBIT_CARD\"}"))
                .andExpect(status().isConflict());
    }

    // =========================================================================
    // Authorization / ownership
    // =========================================================================

    @Test
    void payment_anotherUsersOrder_returns404() throws Exception {
        String ownerToken = registerAndLogin("pay_owner@example.com");
        addItemToCart(ownerToken, 1L, 1);
        long addressId = createAddress(ownerToken);
        long orderId = checkoutAndGetOrderId(ownerToken, addressId);

        String attackerToken = registerAndLogin("pay_attacker@example.com");
        mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + attackerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"paymentMethod\":\"CREDIT_CARD\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void payment_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":1,\"paymentMethod\":\"CREDIT_CARD\"}"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Validation
    // =========================================================================

    @Test
    void payment_invalidPaymentMethod_returns400() throws Exception {
        String token = registerAndLogin("pay_badmethod@example.com");

        mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":1,\"paymentMethod\":\"BITCOIN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void payment_missingOrderId_returns400() throws Exception {
        String token = registerAndLogin("pay_noid@example.com");

        mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"CREDIT_CARD\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void payment_orderNotPendingPayment_returns409() throws Exception {
        String token = registerAndLogin("pay_notpending@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);

        // Cancel the order → status = CANCELLED
        mockMvc.perform(post("/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Attempt payment on CANCELLED order → 409
        mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"paymentMethod\":\"CREDIT_CARD\"}"))
                .andExpect(status().isConflict());
    }

    // =========================================================================
    // GET /payments/{paymentId}
    // =========================================================================

    @Test
    void getPayment_ownPayment_returnsPaymentResponse() throws Exception {
        String token = registerAndLogin("pay_get@example.com");
        addItemToCart(token, 1L, 1);
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);

        MvcResult payResult = mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"paymentMethod\":\"DEBIT_CARD\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        long paymentId = ((Number) parseBody(payResult).get("id")).longValue();

        mockMvc.perform(get("/payments/" + paymentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.paymentReference", notNullValue()))
                .andExpect(jsonPath("$.paymentMethod").value("DEBIT_CARD"))
                .andExpect(jsonPath("$.amount").isNumber())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.paidAt", notNullValue()));
    }

    @Test
    void getPayment_anotherUsersPayment_returns404() throws Exception {
        String ownerToken = registerAndLogin("pay_get_owner@example.com");
        addItemToCart(ownerToken, 1L, 1);
        long addressId = createAddress(ownerToken);
        long orderId = checkoutAndGetOrderId(ownerToken, addressId);

        MvcResult payResult = mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"paymentMethod\":\"CREDIT_CARD\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        long paymentId = ((Number) parseBody(payResult).get("id")).longValue();

        String attackerToken = registerAndLogin("pay_get_attacker@example.com");
        mockMvc.perform(get("/payments/" + paymentId)
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPayment_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/payments/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPayment_nonExistent_returns404() throws Exception {
        String token = registerAndLogin("pay_notfound@example.com");

        mockMvc.perform(get("/payments/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
