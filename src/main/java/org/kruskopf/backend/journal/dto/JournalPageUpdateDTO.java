package org.kruskopf.backend.journal.dto;

import jakarta.annotation.Nullable;

/**
 * Request to rewrite one page.
 * <p>
 * Clearing the last stored page deletes it, which makes it the virtual trailing
 * blank page again; {@link #version} is the token from the page as it was loaded
 * and a stale one is rejected with 409.
 */
public record JournalPageUpdateDTO(
        @Nullable String content,
        @Nullable Long version
) {
}
