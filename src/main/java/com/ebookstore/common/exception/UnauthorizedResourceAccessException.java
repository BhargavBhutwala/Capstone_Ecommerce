package com.ebookstore.common.exception;

/**
 * Thrown when an authenticated user attempts to access a resource they do not
 * own or are not permitted to access.
 * Maps to HTTP 403 Forbidden.
 *
 * <p>Note: in practice, ownership violations are often surfaced as
 * {@link ResourceNotFoundException} to avoid revealing resource existence.
 * Use this exception only when the resource is known to exist but the caller
 * is explicitly denied access.
 */
public class UnauthorizedResourceAccessException extends RuntimeException {

    public UnauthorizedResourceAccessException(String message) {
        super(message);
    }
}
