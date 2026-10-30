package org.kruskopf.backend.message.repository;

import java.time.LocalDateTime;

/**
 * The newest message timestamp of one campaign — a projection of
 * {@link MessageRepository#findLastMessageAtByCampaignIdIn}, the batched query
 * the campaign activity read model is built from.
 * <p>
 * The accessors are <strong>{@code get}-style on purpose, and must stay that
 * way</strong>: this is a Map-backed interface projection, so Spring Data
 * resolves each accessor by stripping the prefix and matching the remainder
 * against the JPQL alias — {@code getCampaignId()} against {@code AS campaignId}.
 * A record-style {@code campaignId()} is rejected outright ("is no accessor
 * method") because it is not an accessor to the interceptor that reads the map,
 * even though the alias itself is {@code campaignId}.
 */
public interface CampaignLastMessage {

    Long getCampaignId();

    LocalDateTime getLastMessageAt();
}
