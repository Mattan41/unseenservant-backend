package org.kruskopf.backend.journal.dto;

import jakarta.annotation.Nullable;

/**
 * Answer to rewriting a page.
 * <p>
 * {@code page} is the saved page, or {@code null} when the page was blank and
 * last, so writing it removed it (together with any further blank pages at the
 * end). {@code pageCount} is the number of stored pages after the change, so the
 * client can recompute its last page number without a second request.
 */
public record JournalPageSaveDTO(
        @Nullable JournalPageDTO page,
        int pageCount
) {
}
