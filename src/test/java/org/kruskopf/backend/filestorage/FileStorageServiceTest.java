package org.kruskopf.backend.filestorage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kruskopf.backend.exception.InvalidFileFormatException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileStorageServiceTest {

    private static final long MAX_BYTES = DataSize.ofMegabytes(5).toBytes();

    @TempDir
    Path tempDir;

    /**
     * Builds a FileStorageService with the fields normally injected by Spring, so the
     * size threshold is deterministic without booting an application context.
     */
    private FileStorageService newService() {
        FileStorageService fileStorageService = new FileStorageService();
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(fileStorageService, "maxFileSize", DataSize.ofMegabytes(5));
        return fileStorageService;
    }

    @Test
    void storeFileThrowsWhenOriginalFilenameIsNull() {
        // MockMultipartFile coerces a null filename to an empty string, so use a Mockito mock
        // to genuinely exercise the null-filename branch.
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn(null);

        FileStorageService fileStorageService = newService();

        InvalidFileFormatException exception = assertThrows(InvalidFileFormatException.class,
                () -> fileStorageService.storeFile(file, "character", "IMAGE"));

        assertEquals("Missing file name", exception.getMessage());
    }

    @Test
    void storeFileAcceptsFileJustUnderLimit() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(MAX_BYTES - 1);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn("image.png");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        FileStorageService fileStorageService = newService();

        String storedFileName = fileStorageService.storeFile(file, "character", "IMAGE");

        assertNotNull(storedFileName);
        assertTrue(storedFileName.startsWith("character_"), "Expected a generated character filename");
    }

    @Test
    void storeFileRejectsFileJustOverLimit() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(MAX_BYTES + 1);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn("image.png");

        FileStorageService fileStorageService = newService();

        InvalidFileFormatException exception = assertThrows(InvalidFileFormatException.class,
                () -> fileStorageService.storeFile(file, "character", "IMAGE"));

        assertEquals("File size exceeds maximum limit of 5MB", exception.getMessage());
    }
}
