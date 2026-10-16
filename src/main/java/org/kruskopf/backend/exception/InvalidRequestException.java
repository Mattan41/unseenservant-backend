package org.kruskopf.backend.exception;

/**
 * A request that is well-formed but not allowed by the domain rules, e.g. an
 * administrator trying to demote themselves or to remove the last remaining
 * administrator.
 * <p>
 * Mapped to 400 by {@link GlobalExceptionHandler}.
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}