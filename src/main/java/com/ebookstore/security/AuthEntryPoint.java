package com.ebookstore.security;

import com.ebookstore.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Returns a structured {@link ErrorResponse} with HTTP 401 when an unauthenticated
 * request reaches a protected endpoint.
 *
 * <p>Never exposes stack traces, Spring Security internals, or token details.
 */
@Component
public class AuthEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(AuthEntryPoint.class);

    private final ObjectMapper objectMapper;

    public AuthEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("Unauthorized request to {}: {}", request.getRequestURI(), authException.getMessage());

        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                "Authentication required. Please provide a valid Bearer token.",
                request.getRequestURI()
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
