package org.kruskopf.backend.campaign.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kruskopf.backend.campaign.dto.CampaignActivityDTO;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.message.repository.CampaignLastMessage;
import org.kruskopf.backend.message.repository.MessageRepository;
import org.kruskopf.backend.ship.entity.Ship;
import org.kruskopf.backend.ship.repository.ShipRepository;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The campaign activity read model: what a badge may sit on, per campaign the
 * caller is in. The side lookups are batched on purpose, so the tests assert
 * both the values and that a page of campaigns costs one query per source.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignActivityService")
class CampaignActivityServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ShipRepository shipRepository;

    @InjectMocks
    private CampaignActivityService campaignActivityService;

    private static final long USER_ID = 7L;

    private static Campaign campaignWithId(long id, LocalDateTime updatedAt) {
        Campaign campaign = TestDataFactory.aCampaign(TestDataFactory.aUser("owner"));
        ReflectionTestUtils.setField(campaign, "id", id);
        campaign.setUpdatedAt(updatedAt);
        return campaign;
    }

    private static Ship shipOf(long campaignId, LocalDateTime updatedAt) {
        Ship ship = new Ship();
        ship.setCampaign(campaignWithId(campaignId, null));
        ship.setUpdatedAt(updatedAt);
        return ship;
    }

    /**
     * One row of the batched "newest message per campaign" projection. The
     * {@code get}-style overrides are what the interface asks for — see the note on
     * {@link CampaignLastMessage}.
     */
    private record LastMessage(Long campaignId, LocalDateTime lastMessageAt) implements CampaignLastMessage {

        @Override
        public Long getCampaignId() {
            return campaignId;
        }

        @Override
        public LocalDateTime getLastMessageAt() {
            return lastMessageAt;
        }
    }

    @Nested
    @DisplayName("getActivityForCurrentUser()")
    class GetActivityForCurrentUser {

        @Test
        @DisplayName("reports the three change stamps of each of the caller's campaigns")
        void reportsTheChangeStamps() {
            LocalDateTime campaignChanged = LocalDateTime.of(2026, 10, 1, 9, 0);
            LocalDateTime lastMessage = LocalDateTime.of(2026, 10, 2, 20, 30);
            LocalDateTime shipChanged = LocalDateTime.of(2026, 10, 3, 7, 15);
            Campaign campaign = campaignWithId(10L, campaignChanged);

            when(campaignRepository.findAllByParticipantsUserId(USER_ID)).thenReturn(List.of(campaign));
            when(messageRepository.findLastMessageAtByCampaignIdIn(List.of(10L)))
                    .thenReturn(List.<CampaignLastMessage>of(new LastMessage(10L, lastMessage)));
            when(shipRepository.findByCampaignIdIn(List.of(10L)))
                    .thenReturn(List.of(shipOf(10L, shipChanged)));

            List<CampaignActivityDTO> activity = campaignActivityService.getActivityForCurrentUser(USER_ID);

            assertThat(activity)
                    .containsExactly(new CampaignActivityDTO(10L, campaignChanged, lastMessage, shipChanged));
        }

        @Test
        @DisplayName("leaves a stamp null when the campaign has no messages and no ship")
        void leavesMissingStampsNull() {
            Campaign campaign = campaignWithId(10L, LocalDateTime.of(2026, 10, 1, 9, 0));

            when(campaignRepository.findAllByParticipantsUserId(USER_ID)).thenReturn(List.of(campaign));
            when(messageRepository.findLastMessageAtByCampaignIdIn(List.of(10L))).thenReturn(List.of());
            when(shipRepository.findByCampaignIdIn(List.of(10L))).thenReturn(List.of());

            List<CampaignActivityDTO> activity = campaignActivityService.getActivityForCurrentUser(USER_ID);

            assertThat(activity).hasSize(1);
            assertThat(activity.getFirst().lastMessageAt()).isNull();
            assertThat(activity.getFirst().shipUpdatedAt()).isNull();
            assertThat(activity.getFirst().campaignUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("keeps a campaign that has only one of the two side records")
        void keepsOneSidedActivity() {
            LocalDateTime lastMessage = LocalDateTime.of(2026, 10, 2, 20, 30);
            Campaign campaign = campaignWithId(10L, LocalDateTime.of(2026, 10, 1, 9, 0));

            when(campaignRepository.findAllByParticipantsUserId(USER_ID)).thenReturn(List.of(campaign));
            when(messageRepository.findLastMessageAtByCampaignIdIn(List.of(10L)))
                    .thenReturn(List.<CampaignLastMessage>of(new LastMessage(10L, lastMessage)));
            when(shipRepository.findByCampaignIdIn(List.of(10L))).thenReturn(List.of());

            List<CampaignActivityDTO> activity = campaignActivityService.getActivityForCurrentUser(USER_ID);

            assertThat(activity.get(0).lastMessageAt()).isEqualTo(lastMessage);
            assertThat(activity.get(0).shipUpdatedAt()).isNull();
        }

        @Test
        @DisplayName("looks the messages and the ships up once for the whole list")
        void batchesTheSideLookups() {
            Campaign first = campaignWithId(10L, LocalDateTime.of(2026, 10, 1, 9, 0));
            Campaign second = campaignWithId(11L, LocalDateTime.of(2026, 10, 1, 9, 0));

            when(campaignRepository.findAllByParticipantsUserId(USER_ID)).thenReturn(List.of(first, second));
            when(messageRepository.findLastMessageAtByCampaignIdIn(List.of(10L, 11L))).thenReturn(List.of());
            when(shipRepository.findByCampaignIdIn(List.of(10L, 11L))).thenReturn(List.of());

            campaignActivityService.getActivityForCurrentUser(USER_ID);

            verify(messageRepository).findLastMessageAtByCampaignIdIn(List.of(10L, 11L));
            verify(shipRepository).findByCampaignIdIn(List.of(10L, 11L));
        }

        @Test
        @DisplayName("answers empty for a caller in no campaign, without reading messages or ships")
        void answersEmptyForACallerWithNoCampaigns() {
            when(campaignRepository.findAllByParticipantsUserId(USER_ID)).thenReturn(List.of());

            assertThat(campaignActivityService.getActivityForCurrentUser(USER_ID)).isEmpty();

            verifyNoInteractions(messageRepository, shipRepository);
        }

        @Test
        @DisplayName("derives the set from the caller's participation, never from every campaign")
        void scopesToTheCaller() {
            when(campaignRepository.findAllByParticipantsUserId(USER_ID)).thenReturn(List.of());

            campaignActivityService.getActivityForCurrentUser(USER_ID);

            verify(campaignRepository).findAllByParticipantsUserId(USER_ID);
            verify(campaignRepository, never()).findAll();
        }
    }
}
