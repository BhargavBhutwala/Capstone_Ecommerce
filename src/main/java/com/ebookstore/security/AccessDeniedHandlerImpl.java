package com.ebookstore.security;

import com.ebookstore.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Returns a structured {@link ErrorResponse} with HTTP 403 when an authenticated
 * user lacks the required authority for a resource.
 *
 * <p>Never exposes stack traces, Spring Security internals, or role details.
 */
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(AccessDeniedHandlerImpl.class);

    private final ObjectMapper objectMapper;

    public AccessDeniedHandlerImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        log.warn("Access denied to {}: {}", request.getRequestURI(), accessDeniedException.getMessage());

        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "FORBIDDEN",
                "You do not have permission to access this resource.",
                request.getRequestURI()
        );

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
