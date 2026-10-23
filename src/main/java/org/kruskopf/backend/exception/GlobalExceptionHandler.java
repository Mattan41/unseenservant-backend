package org.kruskopf.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

/**
 * The one place where a failed request becomes a response body.
 * <p>
 * Every error body is an RFC 9457 {@code ProblemDetail}: {@code type},
 * {@code title}, {@code status}, {@code detail} and {@code instance}, plus
 * {@code field} and {@code value} where they say something the caller can act
 * on. A client therefore reads {@code detail} for the sentence to show and
 * {@code field} for the input to mark; docs/INTEGRATION_CONTRACT.md lists the
 * bodies per status.
 * <p>
 * Extending {@link ResponseEntityExceptionHandler} is what keeps that shape
 * single. Spring Boot auto-configures a bare subclass of it to answer the
 * framework's own exceptions — a malformed body, a parameter of the wrong type,
 * an unsupported media type, an unknown path, an upload past the multipart limit
 * — but only when the application defines none, so extending it here takes those
 * over too and they answer with the same body.
 * <p>
 * The security filter chain runs outside the dispatcher and cannot be reached
 * from here; {@code SecurityConfig} writes the same body for its 401 and 403.
 * Nothing else in the application should build an error response.
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * RFC 9457 gives an absent type the fixed meaning "about:blank"; setting it
     * keeps the member present instead of null in the JSON.
     */
    private static final URI DEFAULT_TYPE = URI.create("about:blank");

    /** Sent for a 500: what went wrong is ours to log, not the caller's to read. */
    private static final String UNEXPECTED_DETAIL = "Something went wrong on the server.";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ProblemDetail> handleUnauthorizedAccessException(UnauthorizedAccessException ex) {
        return response(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InvalidFileFormatException.class)
    public ResponseEntity<ProblemDetail> handleInvalidFileFormatException(InvalidFileFormatException ex) {
        return response(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Too large is 413, like the container's own answer past the multipart limit,
     * rather than a 400 that reads as "wrong kind of file".
     */
    @ExceptionHandler(FileUploadTooLargeException.class)
    public ResponseEntity<ProblemDetail> handleFileUploadTooLarge(FileUploadTooLargeException ex) {
        // CONTENT_TOO_LARGE, not the deprecated PAYLOAD_TOO_LARGE: the same 413,
        // under the name RFC 9110 gives it. The framework's own upload handler
        // uses it too, so both layers report the same title.
        return response(HttpStatus.CONTENT_TOO_LARGE, ex.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRequestException(InvalidRequestException ex) {
        return response(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException ex) {
        return response(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * Something that needed an identity and found none. The filter chain refuses
     * an unauthenticated request before a controller is reached, so this is the
     * backstop for the same case arriving through method security.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(AuthenticationException ex) {
        return response(HttpStatus.UNAUTHORIZED, "Authentication is required.");
    }

    /**
     * A {@code @PreAuthorize} denial. It is thrown inside the controller method,
     * so it resolves here and never reaches the filter chain's access-denied
     * handling; without this mapping the catch-all below would call it a 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return response(HttpStatus.FORBIDDEN, "Access denied.");
    }

    /**
     * A client that has spent its attempts on an unauthenticated endpoint.
     * <p>
     * 429 rather than 403 - the request is not refused, it is early - and
     * {@code Retry-After} is what tells the client when to come back. The body
     * is the same whatever was attempted, so it cannot be read as a signal
     * about whether an account exists.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceeded(RateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(problem(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage()));
    }

    /**
     * A database constraint the service did not pre-check — a unique column hit
     * through a path nobody guarded, for instance. Answered as 409 like
     * {@link UniqueConstraintViolationException}, instead of leaking as a 500 and
     * hiding which side of the contract was broken.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return response(HttpStatus.CONFLICT, "The change conflicts with an existing record.");
    }

    @ExceptionHandler(ConcurrentUpdateException.class)
    public ResponseEntity<ProblemDetail> handleConcurrentUpdateException(ConcurrentUpdateException ex) {
        return response(HttpStatus.CONFLICT, ex.getMessage());
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
    public ResponseEntity<ProblemDetail> handleObjectOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException ex) {
        return response(HttpStatus.CONFLICT, "The resource was updated by someone else. Reload and try again.");
    }

    /**
     * A value the caller offered that is already taken. {@code field} names the
     * input that has to change, {@code value} what was offered — the client may or
     * may not want to show the latter.
     */
    @ExceptionHandler(UniqueConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleUniqueConstraintViolation(UniqueConstraintViolationException ex) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("field", ex.getField());
        if (ex.getValue() != null) {
            problem.setProperty("value", ex.getValue());
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    /**
     * Bean-validation failure on a request body.
     * <p>
     * The constraint messages on the request DTOs exist precisely so the UI can
     * show them ("A display name may be at most 50 characters"), so the first
     * failure's message becomes {@code detail} and its field a {@code field}
     * property the form can mark. Only the first is reported: the UI shows one
     * sentence and the DTOs are small, so a second failure is rare.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        FieldError fieldError = ex.getBindingResult().getFieldError();
        String detail = (fieldError != null && fieldError.getDefaultMessage() != null)
                ? fieldError.getDefaultMessage()
                : "The request body is not valid.";

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, detail);
        if (fieldError != null) {
            problem.setProperty("field", fieldError.getField());
        }

        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Anything not claimed above — a bug on our side, most likely.
     * <p>
     * Deliberately without the exception's message: the caller gets a sentence it
     * can show, the log gets the stack trace. The framework's own exceptions are
     * matched by the inherited handlers before this one is reached; the
     * {@link ErrorResponse} branch keeps the status of anything that arrives here
     * already knowing its own.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            return ResponseEntity.status(errorResponse.getStatusCode()).body(errorResponse.getBody());
        }

        log.error("Unhandled exception", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_DETAIL);
    }

    private static ProblemDetail problem(HttpStatusCode status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(DEFAULT_TYPE);
        return problem;
    }

    private static ResponseEntity<ProblemDetail> response(HttpStatusCode status, String detail) {
        return ResponseEntity.status(status).body(problem(status, detail));
    }

}