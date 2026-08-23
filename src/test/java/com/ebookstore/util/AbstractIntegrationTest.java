package com.ebookstore.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.MediaType;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared base class for all integration tests.
 *
 * <p>Starts a single shared PostgreSQL Testcontainers container for the entire
 * test suite, runs Flyway migrations against it, and wires Spring via
 * {@link DynamicPropertySource}. Tests must NOT skip when Docker is unavailable
 * — if the container fails to start, the test run fails immediately with the
 * exact error.
 *
 * <p>Database cleanup is performed before each test via {@link JdbcTemplate},
 * respecting foreign-key dependency order. This replaces {@code @Transactional}
 * rollback which is ineffective with {@code RANDOM_PORT} tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("ebookstore_test")
                    .withUsername("test")
                    .withPassword("test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /**
     * Truncates all data tables before each test, respecting FK dependency order.
     * Seed data (categories, brands, products) is re-inserted after truncation
     * so catalog-dependent tests always have a consistent baseline.
     */
    @BeforeEach
    void cleanDatabase() {
        // Truncate in dependency order (children first)
        jdbcTemplate.execute("TRUNCATE TABLE payments RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE order_items RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE orders RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE cart_items RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE carts RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE addresses RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE products RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE brands RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE categories RESTART IDENTITY CASCADE");

        // Insert minimal seed data required by all catalog/cart/order tests
        insertSeedData();
    }

    private void insertSeedData() {
        // Categories
        jdbcTemplate.execute("""
                INSERT INTO categories (id, name, description, active, created_at, updated_at)
                VALUES
                  (1, 'Programming', 'Programming books', true, now(), now()),
                  (2, 'Science',     'Science books',     true, now(), now())
                """);

        // Brands
        jdbcTemplate.execute("""
                INSERT INTO brands (id, name, description, active, created_at, updated_at)
                VALUES
                  (1, 'TechPress',   'Technical publisher',   true, now(), now()),
                  (2, 'ScienceHouse','Scientific publisher',  true, now(), now())
                """);

        // Products — at least 2 active products with known stock > 0
        jdbcTemplate.execute("""
                INSERT INTO products (id, title, isbn, description, price, stock_quantity,
                                      category_id, brand_id, delivery_days_min, delivery_days_max,
                                      active, created_at, updated_at)
                VALUES
                  (1, 'Java Fundamentals', '978-1-00001', 'Learn Java', 39.99, 50,
                   1, 1, 3, 7, true, now(), now()),
                  (2, 'Spring Boot in Action', '978-1-00002', 'Spring Boot guide', 49.99, 30,
                   1, 1, 2, 5, true, now(), now()),
                  (3, 'Cosmos Explained', '978-2-00001', 'Astronomy guide', 29.99, 20,
                   2, 2, 4, 8, true, now(), now())
                """);

        // Reset sequences to avoid conflicts with test-inserted data
        jdbcTemplate.execute("SELECT setval('categories_id_seq', 10, true)");
        jdbcTemplate.execute("SELECT setval('brands_id_seq',     10, true)");
        jdbcTemplate.execute("SELECT setval('products_id_seq',   10, true)");
    }

    // =========================================================================
    // Common HTTP helpers available to all subclasses
    // =========================================================================

    protected String registerAndLogin(String email) throws Exception {
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

    protected long getAnyActiveProductId() throws Exception {
        MvcResult result = mockMvc.perform(get("/products?size=1"))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> body = parseBody(result);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> content = (List<Map<?, ?>>) body.get("content");
        assertThat(content).as("Seed data must include at least one product").isNotEmpty();
        return ((Number) content.get(0).get("id")).longValue();
    }

    protected void addItemToCart(String token, long productId, int quantity) throws Exception {
        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":" + quantity + "}"))
                .andExpect(status().isCreated());
    }

    protected long createAddress(String token) throws Exception {
        String body = """
                {"label":"Home","addressLine1":"123 Main St","addressLine2":null,
                "city":"Springfield","state":"IL","postalCode":"62701","country":"US","isDefault":false}
                """;
        MvcResult result = mockMvc.perform(post("/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) parseBody(result).get("id")).longValue();
    }

    protected long checkoutAndGetOrderId(String token, long addressId) throws Exception {
        MvcResult result = mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) parseBody(result).get("id")).longValue();
    }

    protected Map<?, ?> parseBody(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
    }
}
