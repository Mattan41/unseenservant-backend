package org.kruskopf.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an optimistic-concurrency check fails, i.e. the client tried to
 * save a resource that was modified by someone else in the meantime.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConcurrentUpdateException extends RuntimeException {

    public ConcurrentUpdateException(String message) {
        super(message);
    }
}
