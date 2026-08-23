package com.ebookstore.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc OpenAPI configuration for the E-Bookstore API.
 *
 * <p>Sets up:
 * <ul>
 *   <li>API title and version matching the source OpenAPI contract.</li>
 *   <li>A {@code bearerAuth} HTTP Bearer JWT security scheme.</li>
 *   <li>A global security requirement applying {@code bearerAuth} to all
 *       operations by default. Public endpoints (catalog, register, login)
 *       override this via {@code @SecurityRequirements({})} on their
 *       controller methods.</li>
 * </ul>
 *
 * <p>The Springdoc paths are configured in {@code application.yml}:
 * <pre>
 *   springdoc.api-docs.path  = /v3/api-docs   → served at /api/v3/api-docs
 *   springdoc.swagger-ui.path = /swagger-ui.html → served at /api/swagger-ui.html
 * </pre>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-Bookstore API")
                        .version("1.0.0")
                        .description("REST API contract for the AI Specialist Capstone E-Bookstore platform."))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
