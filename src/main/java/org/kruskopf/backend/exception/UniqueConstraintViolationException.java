package org.kruskopf.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)  // 409 Conflict is adequate for duplicate values
public class UniqueConstraintViolationException extends RuntimeException {

    private final String field;
    private final String value;

    public UniqueConstraintViolationException(String field, String value) {
        super("Value '" + value + "' is already in use for " + field);
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}
