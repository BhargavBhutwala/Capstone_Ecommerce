package com.ebookstore.payment;

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
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for payment endpoints.
 *
 * <p>Spins up a real PostgreSQL container via Testcontainers.
 * Each test registers a unique user so tests are independent.
 * Skipped automatically when Docker is unavailable.
 *
 * <p>Covers:
 * <ol>
 *   <li>Register user, create address, add product, POST /orders → PENDING_PAYMENT</li>
 *   <li>POST /payments → 201, Payment.status=SUCCESS, Payment.amount=Order.totalAmount, paidAt set</li>
 *   <li>Order.status=PAID after payment</li>
 *   <li>Second POST /payments → 409</li>
 *   <li>Payment for another user's order → 404</li>
 *   <li>GET /payments/{id} → correct PaymentResponse</li>
 *   <li>GET another user's payment → 404</li>
 * </ol>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @BeforeAll
    static void requireDocker() {
        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Skipping integration tests: Docker is not available on this machine.");
    }

    // =========================================================================
    // Full payment flow
    // =========================================================================

    @Test
    void payment_fullFlow_orderStatusBecomesPaid() throws Exception {
        String token = registerAndLogin("pay_flow@example.com");
        long productId = getAnyActiveProductId();
        addItemToCart(token, productId, 1);
        long addressId = createAddress(token);

        // Checkout → order in PENDING_PAYMENT
        long orderId = checkoutAndGetOrderId(token, addressId);

        // Verify order is PENDING_PAYMENT before payment
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

        // Verify payment.amount equals order.totalAmount
        Map<?, ?> orderBody = getOrderBody(token, orderId);
        Map<?, ?> paymentBody = parseBody(paymentResult);
        double orderTotal = ((Number) orderBody.get("totalAmount")).doubleValue();
        double paymentAmount = ((Number) paymentBody.get("amount")).doubleValue();
        assertThat(paymentAmount).isEqualTo(orderTotal);

        // Verify order.status = PAID
        mockMvc.perform(get("/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void payment_duplicatePayment_returns409() throws Exception {
        String token = registerAndLogin("pay_dup@example.com");
        long productId = getAnyActiveProductId();
        addItemToCart(token, productId, 1);
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

    @Test
    void payment_anotherUsersOrder_returns404() throws Exception {
        // Owner creates order
        String ownerToken = registerAndLogin("pay_owner@example.com");
        long productId = getAnyActiveProductId();
        addItemToCart(ownerToken, productId, 1);
        long addressId = createAddress(ownerToken);
        long orderId = checkoutAndGetOrderId(ownerToken, addressId);

        // Attacker tries to pay for owner's order → must get 404
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
        // Order must be in PENDING_PAYMENT; cancelling makes it CANCELLED which should give 409
        String token = registerAndLogin("pay_notpending@example.com");
        long productId = getAnyActiveProductId();
        addItemToCart(token, productId, 1);
        long addressId = createAddress(token);
        long orderId = checkoutAndGetOrderId(token, addressId);

        // Cancel the order
        mockMvc.perform(post("/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Attempt payment on cancelled order → 409
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
        long productId = getAnyActiveProductId();
        addItemToCart(token, productId, 1);
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
        // Owner pays for their order
        String ownerToken = registerAndLogin("pay_get_owner@example.com");
        long productId = getAnyActiveProductId();
        addItemToCart(ownerToken, productId, 1);
        long addressId = createAddress(ownerToken);
        long orderId = checkoutAndGetOrderId(ownerToken, addressId);

        MvcResult payResult = mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"paymentMethod\":\"CREDIT_CARD\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        long paymentId = ((Number) parseBody(payResult).get("id")).longValue();

        // Attacker tries to read the payment → 404
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
        assertThat(content).as("Seed data must include at least one active product").isNotEmpty();
        return ((Number) content.get(0).get("id")).longValue();
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

    private long checkoutAndGetOrderId(String token, long addressId) throws Exception {
        MvcResult result = mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) parseBody(result).get("id")).longValue();
    }

    private Map<?, ?> getOrderBody(String token, long orderId) throws Exception {
        MvcResult result = mockMvc.perform(get("/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return parseBody(result);
    }

    private Map<?, ?> parseBody(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
    }
}
