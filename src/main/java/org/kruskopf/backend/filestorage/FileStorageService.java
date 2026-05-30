package org.kruskopf.backend.filestorage;

import org.kruskopf.backend.exception.InvalidFileFormatException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for handling file uploads, validation, and storage on the server.
 */
@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    // Define allowed MIME types centrally
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final List<String> ALLOWED_DOC_TYPES = Arrays.asList(
            "application/pdf", "text/plain", "application/json"
    );

    /**
     * Saves a file to the server after validating it against a specific category.
     *
     * @param file             The multipart file being uploaded
     * @param filePrefix       Prefix for the generated unique filename (e.g., "character", "campaign")
     * @param expectedCategory The expected file category ("IMAGE" or "DOCUMENT")
     * @return The unique generated filename
     * @throws IOException                If an error occurs while writing the file to disk
     * @throws InvalidFileFormatException If the file is empty, too large, or has an invalid format
     * @throws IllegalArgumentException  If an unrecognized category is provided
     */
    public String storeFile(MultipartFile file, String filePrefix, String expectedCategory) throws IOException {

        // 1. Generic validation (size and presence)
        validateGenericFile(file);

        // 2. Category-specific MIME type validation
        if ("IMAGE".equalsIgnoreCase(expectedCategory)) {
            validateImageMimeType(file);
        } else if ("DOCUMENT".equalsIgnoreCase(expectedCategory)) {
            validateDocumentMimeType(file);
        } else {
            throw new IllegalArgumentException("Unknown file category: " + expectedCategory);
        }

        // 3. Generate a unique filename to prevent overwriting existing assets
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String fileName = filePrefix + "_" + UUID.randomUUID() + fileExtension;

        // Ensure target directory exists
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Copy file to target destination
        Path targetLocation = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    /**
     * Performs generic validations applicable to all uploaded files.
     *
     * @param file The multipart file to validate
     * @throws InvalidFileFormatException If the file is empty or exceeds the 5MB size limit
     */
    private void validateGenericFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileFormatException("Cannot upload empty file");
        }

        // Enforce a maximum file size limit of 5MB
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new InvalidFileFormatException("File size exceeds maximum limit of 5MB");
        }
    }

    /**
     * Validates that the file has a permitted image MIME type.
     *
     * @param file The multipart file to validate
     * @throws InvalidFileFormatException If the MIME type is missing or not allowed for images
     */
    private void validateImageMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new InvalidFileFormatException("Only JPEG, PNG, GIF and WEBP images are allowed");
        }
    }

    /**
     * Validates that the file has a permitted document MIME type.
     *
     * @param file The multipart file to validate
     * @throws InvalidFileFormatException If the MIME type is missing or not allowed for documents
     */
    private void validateDocumentMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_DOC_TYPES.contains(contentType)) {
            throw new InvalidFileFormatException("Only PDF, TXT and JSON documents are allowed");
        }
    }

    /**
     * Extracts the file extension including the leading dot from a filename.
     *
     * @param fileName The clean path filename
     * @return The file extension (e.g., ".png", ".pdf") or an empty string if none is found
     */
    private String getFileExtension(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf(".");
        if (lastIndexOfDot > 0) {
            return fileName.substring(lastIndexOfDot);
        }
        return "";
    }
}