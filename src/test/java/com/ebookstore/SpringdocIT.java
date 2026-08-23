package com.ebookstore;

import com.ebookstore.util.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Springdoc OpenAPI / Swagger UI endpoints.
 *
 * <p>Verifies that the API documentation is served correctly.
 * These endpoints are public (security: []) per {@link com.ebookstore.config.SecurityConfig}.
 */
class SpringdocIT extends AbstractIntegrationTest {

    /**
     * GET /api/v3/api-docs should return 200 with OpenAPI JSON.
     *
     * <p>Note: the context-path is /api (see application.yml), so the actual
     * path at the test level is /v3/api-docs (MockMvc strips the context-path prefix).
     */
    @Test
    void apiDocs_returns200() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    /**
     * GET /swagger-ui.html should redirect to the Swagger UI page (3xx) or return 200.
     *
     * <p>Springdoc typically redirects /swagger-ui.html → /swagger-ui/index.html.
     * We accept any 2xx or 3xx as success — the important thing is it's not 404/401.
     */
    @Test
    void swaggerUiHtml_returnsSuccessOrRedirect() throws Exception {
        // Accept redirect (3xx) or direct 200 — both indicate Springdoc is configured correctly
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status < 200 || status >= 400) {
                        throw new AssertionError(
                                "Expected 2xx or 3xx for /swagger-ui.html but got: " + status);
                    }
                });
    }

    /**
     * GET /swagger-ui/index.html should return 200.
     */
    @Test
    void swaggerUiIndex_returns200() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
