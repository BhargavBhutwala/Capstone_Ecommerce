package com.ebookstore.catalog;

import com.ebookstore.util.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for catalog endpoints (categories, brands, products).
 *
 * <p>All catalog endpoints are public — no auth token required.
 * Uses the shared Testcontainers PostgreSQL from {@link AbstractIntegrationTest}.
 * Seed data (2 categories, 2 brands, 3 products) is inserted before each test.
 */
class CatalogControllerIT extends AbstractIntegrationTest {

    // =========================================================================
    // GET /categories — no auth required
    // =========================================================================

    @Test
    void listCategories_noToken_returns200WithArray() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id", notNullValue()))
                .andExpect(jsonPath("$[0].name", notNullValue()));
    }

    // =========================================================================
    // GET /brands — no auth required
    // =========================================================================

    @Test
    void listBrands_noToken_returns200WithArray() throws Exception {
        mockMvc.perform(get("/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id", notNullValue()))
                .andExpect(jsonPath("$[0].name", notNullValue()));
    }

    // =========================================================================
    // GET /products — default pagination, search, filters
    // =========================================================================

    @Test
    void searchProducts_noToken_returns200WithPageEnvelope() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.page.page").value(0))
                .andExpect(jsonPath("$.page.size").isNumber())
                .andExpect(jsonPath("$.page.totalElements").isNumber())
                .andExpect(jsonPath("$.page.totalPages").isNumber());
    }

    @Test
    void searchProducts_paginationEnvelopeShape_correct() throws Exception {
        mockMvc.perform(get("/products")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.page").value(0))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalElements").isNumber())
                .andExpect(jsonPath("$.page.totalPages").isNumber());
    }

    @Test
    void searchProducts_withQParam_returnsMatchingResults() throws Exception {
        // "Java" should match "Java Fundamentals"
        MvcResult result = mockMvc.perform(get("/products").param("q", "Java"))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = parseBody(result);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> content = (List<Map<?, ?>>) body.get("content");
        assertThat(content).isNotEmpty();
        assertThat(content.stream().anyMatch(p -> p.get("title").toString().contains("Java"))).isTrue();
    }

    @Test
    void searchProducts_withCategoryId_returnsMatchingResults() throws Exception {
        // Category 1 = Programming (products 1 and 2)
        MvcResult result = mockMvc.perform(get("/products").param("categoryId", "1"))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = parseBody(result);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> content = (List<Map<?, ?>>) body.get("content");
        assertThat(content).isNotEmpty();
    }

    @Test
    void searchProducts_withBrandId_returnsMatchingResults() throws Exception {
        // Brand 1 = TechPress (products 1 and 2)
        MvcResult result = mockMvc.perform(get("/products").param("brandId", "1"))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = parseBody(result);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> content = (List<Map<?, ?>>) body.get("content");
        assertThat(content).isNotEmpty();
    }

    @Test
    void searchProducts_withMinPrice_returnsProductsAbovePrice() throws Exception {
        MvcResult result = mockMvc.perform(get("/products").param("minPrice", "40"))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = parseBody(result);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> content = (List<Map<?, ?>>) body.get("content");
        // Only "Spring Boot in Action" at 49.99 is >= 40
        assertThat(content).isNotEmpty();
        content.forEach(p -> {
            double price = ((Number) p.get("price")).doubleValue();
            assertThat(price).isGreaterThanOrEqualTo(40.0);
        });
    }

    @Test
    void searchProducts_withMaxPrice_returnsProductsBelowPrice() throws Exception {
        MvcResult result = mockMvc.perform(get("/products").param("maxPrice", "35"))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = parseBody(result);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> content = (List<Map<?, ?>>) body.get("content");
        // Only "Cosmos Explained" at 29.99 is <= 35
        assertThat(content).isNotEmpty();
        content.forEach(p -> {
            double price = ((Number) p.get("price")).doubleValue();
            assertThat(price).isLessThanOrEqualTo(35.0);
        });
    }

    @Test
    void searchProducts_withAvailableOnlyTrue_returnsOnlyInStock() throws Exception {
        MvcResult result = mockMvc.perform(get("/products").param("availableOnly", "true"))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = parseBody(result);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> content = (List<Map<?, ?>>) body.get("content");
        // All seed products are active and in-stock
        assertThat(content).isNotEmpty();
    }

    @Test
    void searchProducts_withAvailableOnlyFalse_returns200() throws Exception {
        mockMvc.perform(get("/products").param("availableOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void searchProducts_defaultSortWorks() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void searchProducts_withSortParam_returns200() throws Exception {
        mockMvc.perform(get("/products")
                        .param("sort", "price,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // =========================================================================
    // GET /products/{id} — detail and not found
    // =========================================================================

    @Test
    void getProduct_found_returns200WithProductDetail() throws Exception {
        long productId = getAnyActiveProductId();

        mockMvc.perform(get("/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.title", notNullValue()))
                .andExpect(jsonPath("$.price", notNullValue()))
                .andExpect(jsonPath("$.stockQuantity", notNullValue()))
                // OpenAPI ProductSummary exposes `available` (derived: active && stock>0), not `active`
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void getProduct_notFound_returns404WithErrorResponse() throws Exception {
        mockMvc.perform(get("/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    // =========================================================================
    // GET /products/{id}/related — same-category products
    // =========================================================================

    @Test
    void getRelatedProducts_found_returns200WithList() throws Exception {
        // Product 1 (Java Fundamentals, cat 1) → related = product 2 (Spring Boot, cat 1)
        mockMvc.perform(get("/products/1/related"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getRelatedProducts_notFound_returns404() throws Exception {
        mockMvc.perform(get("/products/999999/related"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // =========================================================================
    // GET /categories/{id}/products — paged
    // =========================================================================

    @Test
    void getProductsByCategory_returns200WithPageEnvelope() throws Exception {
        mockMvc.perform(get("/categories/1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists());
    }

    // =========================================================================
    // GET /brands/{id}/products — paged
    // =========================================================================

    @Test
    void getProductsByBrand_returns200WithPageEnvelope() throws Exception {
        mockMvc.perform(get("/brands/1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists());
    }

    // =========================================================================
    // Security: public endpoints accessible without token
    // =========================================================================

    @Test
    void catalog_publicEndpoints_accessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/categories")).andExpect(status().isOk());
        mockMvc.perform(get("/brands")).andExpect(status().isOk());
        mockMvc.perform(get("/products")).andExpect(status().isOk());
        mockMvc.perform(get("/products/1")).andExpect(status().isOk());
    }
}
