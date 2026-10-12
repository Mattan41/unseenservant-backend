package org.kruskopf.backend.filestorage;

import org.junit.jupiter.api.Test;
import org.kruskopf.backend.exception.InvalidFileFormatException;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileStorageServiceTest {

    @Test
    void storeFileThrowsWhenOriginalFilenameIsNull() {
        // MockMultipartFile coerces a null filename to an empty string, so use a Mockito mock
        // to genuinely exercise the null-filename branch.
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn(null);

        FileStorageService fileStorageService = new FileStorageService();

        InvalidFileFormatException exception = assertThrows(InvalidFileFormatException.class,
                () -> fileStorageService.storeFile(file, "character", "IMAGE"));

        assertEquals("Missing file name", exception.getMessage());
    }
}
