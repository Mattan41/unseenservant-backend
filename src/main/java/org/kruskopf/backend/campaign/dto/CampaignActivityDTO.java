package org.kruskopf.backend.campaign.dto;

import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

/**
 * What has changed in one campaign, as the client needs it to badge its
 * navigation: a change timestamp per area, never the data itself.
 * <p>
 * Every timestamp is nullable, and a {@code null} means "there is nothing there
 * yet" — no ship, no messages — rather than "unreadable". Nothing here is
 * per-user: the record is the same for every participant, and the read state
 * lives on the client, which compares these stamps against the "last opened"
 * markers it keeps per user. That is also why there is no "unread count" field:
 * a count would need either per-user server state or the client's markers sent
 * along with the request, and the client already has the messages of the
 * campaign it is looking at.
 *
 * @param campaignUpdatedAt the campaign row's {@code updatedAt} — the signal
 *                          behind the <em>World Lore &amp; Background</em> badge.
 *                          Any campaign write moves it (a rename, a new image, a
 *                          participant or add-on change), not only a change to
 *                          the lore text.
 * @param lastMessageAt     when the campaign's newest message was written, or
 *                          {@code null} when it has none
 * @param shipUpdatedAt     the ship row's {@code updatedAt}, or {@code null} when
 *                          the campaign has no ship
 */
public record CampaignActivityDTO(
        Long campaignId,
        @Nullable LocalDateTime campaignUpdatedAt,
        @Nullable LocalDateTime lastMessageAt,
        @Nullable LocalDateTime shipUpdatedAt
) {
}
