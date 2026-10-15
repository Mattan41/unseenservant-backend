package org.kruskopf.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<String> handleUnauthorizedAccessException(UnauthorizedAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidFileFormatException.class)
    public ResponseEntity<String> handleInvalidFileFormatException(InvalidFileFormatException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(ConcurrentUpdateException.class)
    public ResponseEntity<String> handleConcurrentUpdateException(ConcurrentUpdateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    /**
     * Thrown by Hibernate when a {@code @Version} row changed between the load and
     * the flush. The service-level checks run earlier, so this is the case they
     * cannot see: two writers that both passed the check and both flushed.
     * <p>
     * It surfaces from the transaction interceptor (at commit), which is why it
     * has to be handled here rather than inside the service.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<String> handleObjectOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("The resource was updated by someone else. Reload and try again.");
    }

    @ExceptionHandler(UniqueConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleUniqueConstraintViolation(UniqueConstraintViolationException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Unique constraint violation");
        response.put("message", ex.getMessage());
        response.put("field", ex.getField());
        response.put("value", ex.getValue());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

}