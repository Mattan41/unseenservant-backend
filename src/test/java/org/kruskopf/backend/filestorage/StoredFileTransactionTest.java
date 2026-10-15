package org.kruskopf.backend.filestorage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The contract that keeps the upload paths from holding a database connection
 * during the disk write: store the file first, run the database work in a short
 * transaction, and remove the file again if that transaction fails.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StoredFileTransaction")
class StoredFileTransactionTest {

    private static final String PREFIX = "ship_7";
    private static final String STORED_NAME = "ship_7_abc.png";

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private StoredFileTransaction storedFileTransaction;

    /** Makes the mocked template actually run the callback, as the real one does. */
    private void runCallbackInTransaction() {
        when(transactionTemplate.execute(ArgumentMatchers.<TransactionCallback<Object>>any()))
                .thenAnswer(invocation -> invocation.<TransactionCallback<Object>>getArgument(0)
                        .doInTransaction(mock(TransactionStatus.class)));
    }

    @Test
    @DisplayName("keeps the file when the transaction succeeds")
    void keepsFileOnSuccess() throws IOException {
        when(fileStorageService.storeFile(any(), eq(PREFIX), eq("IMAGE"))).thenReturn(STORED_NAME);
        runCallbackInTransaction();

        String url = storedFileTransaction.storeThenRun(
                mock(MultipartFile.class), PREFIX, "IMAGE", name -> "/images/" + name);

        assertThat(url).isEqualTo("/images/" + STORED_NAME);
        verify(fileStorageService, never()).deleteFile(anyString());
    }

    @Test
    @DisplayName("deletes the stored file when the transaction fails")
    void deletesFileWhenTransactionFails() throws IOException {
        when(fileStorageService.storeFile(any(), eq(PREFIX), eq("IMAGE"))).thenReturn(STORED_NAME);
        when(transactionTemplate.execute(ArgumentMatchers.<TransactionCallback<Object>>any()))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> storedFileTransaction.storeThenRun(
                mock(MultipartFile.class), PREFIX, "IMAGE", name -> "url"))
                .isInstanceOf(IllegalStateException.class);

        verify(fileStorageService).deleteFile(STORED_NAME);
    }

    @Test
    @DisplayName("does not touch the transaction or delete anything when storing itself fails")
    void doesNotCleanUpWhenStoringFails() throws IOException {
        when(fileStorageService.storeFile(any(), anyString(), anyString()))
                .thenThrow(new IOException("disk full"));

        assertThatThrownBy(() -> storedFileTransaction.storeThenRun(
                mock(MultipartFile.class), PREFIX, "IMAGE", name -> "url"))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(IOException.class);

        // Nothing was written, so there is nothing to clean up and no transaction
        // should have been started.
        verifyNoInteractions(transactionTemplate);
        verify(fileStorageService, never()).deleteFile(anyString());
    }

    @Test
    @DisplayName("a failing cleanup does not replace the original exception")
    void cleanupFailureDoesNotMaskTheOriginal() throws IOException {
        when(fileStorageService.storeFile(any(), anyString(), anyString())).thenReturn(STORED_NAME);
        doThrow(new IllegalArgumentException("refusing to delete outside the upload directory"))
                .when(fileStorageService).deleteFile(STORED_NAME);
        when(transactionTemplate.execute(ArgumentMatchers.<TransactionCallback<Object>>any()))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> storedFileTransaction.storeThenRun(
                mock(MultipartFile.class), PREFIX, "IMAGE", name -> "url"))
                .as("the caller must still see why the upload failed")
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
    }
}
