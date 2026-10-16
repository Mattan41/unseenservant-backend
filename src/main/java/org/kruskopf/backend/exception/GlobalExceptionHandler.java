package org.kruskopf.backend.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<String> handleInvalidRequestException(InvalidRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    /**
     * A database constraint the service did not pre-check — a unique column hit
     * through a path nobody guarded, for instance. Answered as 409 like
     * {@link UniqueConstraintViolationException}, instead of leaking as a 500 and
     * hiding which side of the contract was broken.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("The change conflicts with an existing record.");
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

    /**
     * Bean-validation failure on a request body.
     * <p>
     * Without this the client receives Spring's default error body, which carries
     * no message — and the constraint messages on the request DTOs exist precisely
     * so the UI can show them ("A display name may be at most 50 characters").
     * The body shape matches {@code UniqueConstraintViolationException} above.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationFailure(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Validation failed");
        response.put("field", fieldError != null ? fieldError.getField() : null);
        response.put(
                "message",
                fieldError != null ? fieldError.getDefaultMessage() : "The request body is not valid.");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

}