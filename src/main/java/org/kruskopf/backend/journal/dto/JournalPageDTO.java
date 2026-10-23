package org.kruskopf.backend.journal.dto;

import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

/**
 * One page of a journal.
 *
 * @param pageNumber the page's place in the book (1-based) — a position, not an
 *                   identity; a client sends {@code id} back when it writes
 * @param version    the optimistic-locking token the client must return on save
 */
public record JournalPageDTO(
        Long id,
        int pageNumber,
        String content,
        long version,
        @Nullable LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt
) {
}
