package com.ebookstore.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for catalog endpoints (categories, brands, products).
 *
 * <p>All catalog endpoints are public — no auth token required.
 * Spins up a Testcontainers PostgreSQL and runs Flyway migrations.
 * Skipped automatically when Docker is unavailable.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @BeforeAll
    static void requireDocker() {
        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Skipping catalog integration tests: Docker is not available.");
    }

    // =========================================================================
    // GET /categories — no auth required
    // =========================================================================

    @Test
    void listCategories_noToken_returns200() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // =========================================================================
    // GET /brands — no auth required
    // =========================================================================

    @Test
    void listBrands_noToken_returns200() throws Exception {
        mockMvc.perform(get("/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // =========================================================================
    // GET /products — search/filter/pagination
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
    void searchProducts_withQParam_returns200() throws Exception {
        mockMvc.perform(get("/products").param("q", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void searchProducts_withAvailableOnlyFalse_returns200() throws Exception {
        mockMvc.perform(get("/products").param("availableOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void searchProducts_withPriceFilters_returns200() throws Exception {
        mockMvc.perform(get("/products")
                        .param("minPrice", "0")
                        .param("maxPrice", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void searchProducts_withCategoryId_returns200() throws Exception {
        mockMvc.perform(get("/products").param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void searchProducts_withBrandId_returns200() throws Exception {
        mockMvc.perform(get("/products").param("brandId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void searchProducts_paginationEnvelopeShape_correct() throws Exception {
        mockMvc.perform(get("/products")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.page").value(0))
                .andExpect(jsonPath("$.page.size").value(5));
    }

    @Test
    void searchProducts_defaultSortByTitle() throws Exception {
        // Just verify no exception and response is OK with default sort
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // GET /products/{id} — not found returns 404
    // =========================================================================

    @Test
    void getProduct_notFound_returns404WithErrorResponse() throws Exception {
        mockMvc.perform(get("/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
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
    // GET /products/{id}/related — not found returns 404
    // =========================================================================

    @Test
    void getRelatedProducts_notFound_returns404() throws Exception {
        mockMvc.perform(get("/products/999999/related"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
