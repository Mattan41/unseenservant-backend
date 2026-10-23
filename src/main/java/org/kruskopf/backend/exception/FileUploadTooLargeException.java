package org.kruskopf.backend.exception;

/**
 * An upload larger than what the application accepts
 * ({@code app.upload.max-file-size}).
 * <p>
 * Answered as 413 by {@link GlobalExceptionHandler} — the same status the servlet
 * container produces for an upload past the multipart limit, so the client sees
 * the same outcome whichever layer caught the file. Distinct from
 * {@link InvalidFileFormatException} (400), which is about a wrong or empty file.
 */
public class FileUploadTooLargeException extends RuntimeException {

    public FileUploadTooLargeException(String message) {
        super(message);
    }
}
