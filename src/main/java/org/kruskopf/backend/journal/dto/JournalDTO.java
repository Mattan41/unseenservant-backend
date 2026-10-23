package org.kruskopf.backend.journal.dto;

import java.util.List;

/**
 * A participant's whole journal. {@code pages} holds the stored pages in reading
 * order and {@code pageCount} is their number — the trailing blank page a client
 * always shows is virtual and is never part of this payload, so the client's last
 * page number is {@code pageCount + 1}.
 * <p>
 * {@code id} is null and the lists are empty while nothing has been written: a
 * read never creates the journal row.
 */
public record JournalDTO(
        Long id,
        Long campaignId,
        Long userId,
        int pageCount,
        List<JournalPageDTO> pages
) {
}
