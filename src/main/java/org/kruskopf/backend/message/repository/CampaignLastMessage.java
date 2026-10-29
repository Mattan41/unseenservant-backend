package org.kruskopf.backend.message.repository;

import java.time.LocalDateTime;

/**
 * The newest message timestamp of one campaign — a projection of
 * {@link MessageRepository#findLastMessageAtByCampaignIdIn}, the batched query
 * the campaign activity read model is built from.
 */
public interface CampaignLastMessage {

    Long getCampaignId();

    LocalDateTime getLastMessageAt();
}
