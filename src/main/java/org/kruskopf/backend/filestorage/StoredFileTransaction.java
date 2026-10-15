package org.kruskopf.backend.filestorage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.function.Function;

/**
 * Runs "store a file, then update the database" without holding a database
 * connection while the file is written.
 * <p>
 * The file is stored <em>before</em> the transaction opens, so the connection is
 * held only for the (short) database work instead of for the whole disk write.
 * If that work fails — including at commit — the stored file is deleted again, so
 * a rejected request leaves no orphan behind.
 * <p>
 * Callers <strong>must</strong> validate and authorize before calling this: the
 * file is written unconditionally, so a request that should have been refused
 * would otherwise touch the disk first.
 */
@Component
public class StoredFileTransaction {

    private static final Logger log = LoggerFactory.getLogger(StoredFileTransaction.class);

    private final FileStorageService fileStorageService;
    private final TransactionTemplate transactionTemplate;

    public StoredFileTransaction(FileStorageService fileStorageService,
                                 TransactionTemplate transactionTemplate) {
        this.fileStorageService = fileStorageService;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Stores {@code file} and then runs {@code work} in its own transaction,
     * passing the generated file name so the callback can build the URL it stores.
     *
     * @param file     The upload, validated by {@link FileStorageService#storeFile}
     * @param prefix   Filename prefix, e.g. {@code "ship_7"}
     * @param category {@code "IMAGE"} or {@code "DOCUMENT"}
     * @param work     The database work; runs inside a transaction
     * @return Whatever {@code work} returned
     * @throws org.kruskopf.backend.exception.InvalidFileFormatException If the file is rejected
     */
    public <T> T storeThenRun(MultipartFile file, String prefix, String category,
                              Function<String, T> work) {
        String storedName;
        try {
            storedName = fileStorageService.storeFile(file, prefix, category);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store uploaded file", e);
        }

        try {
            return transactionTemplate.execute(status -> work.apply(storedName));
        } catch (RuntimeException e) {
            deleteQuietly(storedName);
            throw e;
        }
    }

    /**
     * Never lets a cleanup failure replace the exception the caller is already
     * handling.
     */
    private void deleteQuietly(String storedName) {
        try {
            fileStorageService.deleteFile(storedName);
        } catch (RuntimeException cleanupFailure) {
            log.warn("Could not clean up stored file '{}' after a failed transaction",
                    storedName, cleanupFailure);
        }
    }
}
