package org.kruskopf.backend.filestorage;

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
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageService {
    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * Saves a file to the server after validating it.
     *
     * @param file File that is being uploaded
     * @param filePrefix Prefix for the file name
     * @return The generated file name
     * @throws IOException If an error occurs while saving the file
     * @throws IllegalArgumentException If the file is invalid or exceeds the size limit
     */
    public String storeFile(MultipartFile file, String filePrefix) throws IOException {

        validateImageFile(file);

        // Create a unique file name to avoid overwriting existing files
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String fileName = filePrefix + "_" + UUID.randomUUID() + fileExtension;

        // Create the upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Copy the file to the target location
        Path targetLocation = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("File stored at: " + targetLocation);
        System.out.println("File name: " + fileName);
        return fileName;
    }

    /**
     * Validate that the uploaded file is a valid image file and meets the size requirements.
     *
     * @param file The file that is being uploaded
     * @throws IllegalArgumentException if the file is empty, exceeds the size limit, or is not a valid image type
     */
    private void validateImageFile(MultipartFile file) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }

        // File size validation maximum 5MB
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
        }

        // Validate MIME type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        // Only allow specific image types
        List<String> allowedTypes = Arrays.asList(
                "image/jpeg", "image/png", "image/gif", "image/webp"
        );

        if (!allowedTypes.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG, GIF and WEBP images are allowed");
        }
    }

    /**
     * Extracts the file extension from the given filename.
     *
     * @param fileName File name
     * @return File extension including the dot (e.g., ".jpg", ".png")
     */
    private String getFileExtension(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf(".");
        if (lastIndexOfDot > 0) {
            return fileName.substring(lastIndexOfDot);
        }
        return ""; // No extension found
    }
}
