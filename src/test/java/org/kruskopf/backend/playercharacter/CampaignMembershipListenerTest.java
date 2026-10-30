package org.kruskopf.backend.playercharacter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kruskopf.backend.campaign.event.CampaignDeletedEvent;
import org.kruskopf.backend.campaign.event.ParticipantRemovedEvent;
import org.kruskopf.backend.playercharacter.service.GameCharacterService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The character domain's half of a roster change: it is told, and it unlinks. The
 * campaign side never calls the character service itself, so these two handlers are
 * the only place a character is detached from a campaign its owner has left.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignMembershipListener")
class CampaignMembershipListenerTest {

    @Mock
    private GameCharacterService gameCharacterService;

    @InjectMocks
    private CampaignMembershipListener listener;

    private static final long CAMPAIGN_ID = 10L;
    private static final long PLAYER_ID = 3L;
    private static final long OTHER_ID = 4L;

    @Nested
    @DisplayName("onParticipantRemoved()")
    class OnParticipantRemoved {

        @Test
        @DisplayName("Unlinks that user's characters from the campaign")
        void unlinksTheRemovedUser() {
            listener.onParticipantRemoved(new ParticipantRemovedEvent(CAMPAIGN_ID, PLAYER_ID));

            verify(gameCharacterService).removeAllCharactersFromCampaign(PLAYER_ID, CAMPAIGN_ID);
        }
    }

    @Nested
    @DisplayName("onCampaignDeleted()")
    class OnCampaignDeleted {

        @Test
        @DisplayName("Unlinks every participant's characters from the campaign")
        void unlinksEveryParticipant() {
            listener.onCampaignDeleted(new CampaignDeletedEvent(CAMPAIGN_ID, List.of(PLAYER_ID, OTHER_ID)));

            verify(gameCharacterService).removeAllCharactersFromCampaign(PLAYER_ID, CAMPAIGN_ID);
            verify(gameCharacterService).removeAllCharactersFromCampaign(OTHER_ID, CAMPAIGN_ID);
        }

        @Test
        @DisplayName("An empty roster touches nothing")
        void emptyRosterDoesNothing() {
            listener.onCampaignDeleted(new CampaignDeletedEvent(CAMPAIGN_ID, List.of()));

            verifyNoInteractions(gameCharacterService);
        }
    }
}
