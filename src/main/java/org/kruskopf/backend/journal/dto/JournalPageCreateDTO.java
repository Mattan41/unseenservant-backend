package org.kruskopf.backend.journal.dto;

import jakarta.annotation.Nullable;

/**
 * Request to add a page.
 *
 * @param afterPageId the page the new one is inserted after; {@code null} appends
 *                    it at the end of the book. Appending a blank page is a
 *                    no-op, because the last page is always a blank virtual one.
 * @param content     markdown source; may be blank, which keeps an empty page in
 *                    the middle of the book
 */
public record JournalPageCreateDTO(
        @Nullable Long afterPageId,
        @Nullable String content
) {
}
