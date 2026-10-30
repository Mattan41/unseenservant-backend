package org.kruskopf.backend.campaign;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.kruskopf.backend.campaign.dto.ParticipantResponseDTO;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The two fallbacks a participant row is built from: the role the request asks
 * for (PLAYER when it asks for nothing usable, never an error) and the nickname
 * (the user's display name when the request asks for none).
 */
@DisplayName("CampaignParticipantFactory Unit Tests")
class CampaignParticipantFactoryTest {

    private final CampaignParticipantFactory factory = new CampaignParticipantFactory();

    private static final long CAMPAIGN_ID = 10L;
    private static final long USER_ID = 3L;

    private User userWithId(long id) {
        User user = TestDataFactory.aUser("player" + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Campaign campaign() {
        Campaign campaign = TestDataFactory.aCampaign(userWithId(1L));
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        return campaign;
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Carries the campaign and the user onto the row")
        void carriesCampaignAndUser() {
            Campaign campaign = campaign();
            User user = userWithId(USER_ID);

            CampaignUser participant = factory.create(campaign, user, new ParticipantResponseDTO(USER_ID, "Nick", "PLAYER"));

            assertThat(participant.getCampaign()).isSameAs(campaign);
            assertThat(participant.getUser()).isSameAs(user);
            assertThat(participant.getId().campaignId()).isEqualTo(CAMPAIGN_ID);
            assertThat(participant.getId().userId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("Keeps the nickname and the role the request asks for")
        void keepsRequestedNicknameAndRole() {
            CampaignUser participant = factory.create(
                    campaign(), userWithId(USER_ID), new ParticipantResponseDTO(USER_ID, "The Wizard", "GM"));

            assertThat(participant.getNickname()).isEqualTo("The Wizard");
            assertThat(participant.getRole()).isEqualTo(CampaignRole.GM);
        }

        @Test
        @DisplayName("Falls back to the user's display name when no nickname is asked for")
        void fallsBackToDisplayName() {
            User user = userWithId(USER_ID);
            user.setDisplayName("Ada Lovelace");

            CampaignUser participant = factory.create(
                    campaign(), user, new ParticipantResponseDTO(USER_ID, null, "PLAYER"));

            assertThat(participant.getNickname()).isEqualTo("Ada Lovelace");
        }

        @Test
        @DisplayName("A blank nickname is treated as none")
        void blankNicknameFallsBack() {
            User user = userWithId(USER_ID);
            user.setDisplayName("Ada Lovelace");

            CampaignUser participant = factory.create(
                    campaign(), user, new ParticipantResponseDTO(USER_ID, "  ", "PLAYER"));

            assertThat(participant.getNickname()).isEqualTo("Ada Lovelace");
        }

        @Test
        @DisplayName("Falls back all the way to the username when the user has no name either")
        void fallsBackToUsername() {
            User user = userWithId(USER_ID);
            user.setDisplayName(null);
            user.setFullName(null);

            CampaignUser participant = factory.create(
                    campaign(), user, new ParticipantResponseDTO(USER_ID, null, "PLAYER"));

            assertThat(participant.getNickname()).isEqualTo(user.getUserName());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "KING"})
        @DisplayName("A role that is missing, blank or unknown falls back to PLAYER")
        void unusableRoleIsPlayer(String role) {
            CampaignUser participant = factory.create(
                    campaign(), userWithId(USER_ID), new ParticipantResponseDTO(USER_ID, "Nick", role));

            assertThat(participant.getRole()).isEqualTo(CampaignRole.PLAYER);
        }
    }
}
