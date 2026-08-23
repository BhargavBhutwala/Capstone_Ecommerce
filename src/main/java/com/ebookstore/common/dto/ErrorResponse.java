package com.ebookstore.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Canonical error response body returned by {@code GlobalExceptionHandler} for
 * all error HTTP responses.
 *
 * <p>Shape:
 * <pre>
 * {
 *   "timestamp": "2024-01-15T10:30:00Z",
 *   "status":    404,
 *   "code":      "RESOURCE_NOT_FOUND",
 *   "message":   "Product not found with id: 42",
 *   "path":      "/api/products/42",
 *   "fieldErrors": { "title": "must not be blank" }   // omitted when null
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final OffsetDateTime timestamp;
    private final int status;
    private final String code;
    private final String message;
    private final String path;
    private final Map<String, String> fieldErrors;

    public ErrorResponse(OffsetDateTime timestamp,
                         int status,
                         String code,
                         String message,
                         String path,
                         Map<String, String> fieldErrors) {
        this.timestamp = timestamp;
        this.status = status;
        this.code = code;
        this.message = message;
        this.path = path;
        this.fieldErrors = fieldErrors;
    }

    /** Convenience constructor for responses without field-level errors. */
    public ErrorResponse(OffsetDateTime timestamp,
                         int status,
                         String code,
                         String message,
                         String path) {
        this(timestamp, status, code, message, path, null);
    }

    public OffsetDateTime getTimestamp() { return timestamp; }
    public int getStatus()               { return status; }
    public String getCode()              { return code; }
    public String getMessage()           { return message; }
    public String getPath()              { return path; }
    public Map<String, String> getFieldErrors() { return fieldErrors; }
}
