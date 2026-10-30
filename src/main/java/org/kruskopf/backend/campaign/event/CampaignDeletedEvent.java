package org.kruskopf.backend.campaign.event;

import java.util.List;

/**
 * A campaign was deleted.
 * <p>
 * {@code participantIds} are the user ids that were on the roster at the moment of
 * deletion — the rows are gone by the time this is handled, so the event has to carry
 * them. Published by the campaign domain so the character domain can unlink every
 * character that pointed at the campaign.
 */
public record CampaignDeletedEvent(long campaignId, List<Long> participantIds) {
}
