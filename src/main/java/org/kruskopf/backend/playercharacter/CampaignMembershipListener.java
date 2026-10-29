package org.kruskopf.backend.playercharacter;

import org.kruskopf.backend.campaign.event.CampaignDeletedEvent;
import org.kruskopf.backend.campaign.event.ParticipantRemovedEvent;
import org.kruskopf.backend.playercharacter.service.GameCharacterService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The character domain's reaction to a roster change: a character must not stay in a
 * campaign its owner has left, and a campaign that is gone cannot have characters
 * pointing at it.
 * <p>
 * Both handlers are plain {@link EventListener}s, which Spring runs synchronously on
 * the publishing thread inside the publisher's transaction. That is what keeps the
 * unlink as atomic as it was when the campaign services called
 * {@code GameCharacterService} directly: a roster change that rolls back takes the
 * unlink with it. Listening after the commit instead
 * ({@code @TransactionalEventListener} + {@code REQUIRES_NEW}) would decouple the
 * transactions as well, at the price of a failed unlink leaving orphaned references
 * behind with nothing to retry them.
 */
@Component
public class CampaignMembershipListener {

    private final GameCharacterService gameCharacterService;

    public CampaignMembershipListener(GameCharacterService gameCharacterService) {
        this.gameCharacterService = gameCharacterService;
    }

    @EventListener
    public void onParticipantRemoved(ParticipantRemovedEvent event) {
        gameCharacterService.removeAllCharactersFromCampaign(event.userId(), event.campaignId());
    }

    @EventListener
    public void onCampaignDeleted(CampaignDeletedEvent event) {
        event.participantIds().forEach(userId ->
                gameCharacterService.removeAllCharactersFromCampaign(userId, event.campaignId()));
    }
}
