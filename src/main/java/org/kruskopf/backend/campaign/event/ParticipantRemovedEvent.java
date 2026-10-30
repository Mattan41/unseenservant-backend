package org.kruskopf.backend.campaign.event;

/**
 * A user left a campaign's roster.
 * <p>
 * Published by the campaign domain so the character domain can unlink that user's
 * characters without the campaign having to know characters exist. The listener is
 * {@code CampaignMembershipListener}.
 * <p>
 * Ids only, deliberately: the roster row is gone by the time this is handled in a
 * removal, and an event that carried entities would invite the listener to read
 * state that is no longer there.
 */
public record ParticipantRemovedEvent(long campaignId, long userId) {
}
