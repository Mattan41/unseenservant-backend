package org.kruskopf.backend.exception;

/**
 * Thrown when a client has spent its attempts for the current window on an
 * unauthenticated endpoint.
 * <p>
 * Answered as 429 with a {@code Retry-After} header, not as 403: the request is
 * not forbidden, it is early. The message deliberately says nothing about
 * whether the account exists, so the response cannot become a way to probe for
 * usernames.
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Too many attempts. Please try again in " + retryAfterSeconds + " seconds.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /** Seconds the client should wait before repeating the request. */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}