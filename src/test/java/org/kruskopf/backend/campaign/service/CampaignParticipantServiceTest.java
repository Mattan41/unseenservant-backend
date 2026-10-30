package org.kruskopf.backend.campaign.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kruskopf.backend.addon.service.CampaignAddonService;
import org.kruskopf.backend.campaign.CampaignMapper;
import org.kruskopf.backend.campaign.CampaignParticipantFactory;
import org.kruskopf.backend.campaign.dto.CampaignResponseDTO;
import org.kruskopf.backend.campaign.dto.ParticipantResponseDTO;
import org.kruskopf.backend.campaign.dto.UpdateParticipantsDTO;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.campaign.event.ParticipantRemovedEvent;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.exception.InvalidRequestException;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.service.UserService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The participant roster's administration axis: renaming, roles, ownership and the
 * add/remove of participants. Every write here is owner-only, except that a
 * participant may rename themselves — so the owner in these fixtures is a plain
 * PLAYER and the GM is a different, non-owning participant.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignParticipantService")
class CampaignParticipantServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private UserService userService;

    @Mock
    private CampaignPermissionService campaignPermissionService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private CampaignAddonService campaignAddonService;

    @Spy
    private CampaignMapper campaignMapper = new CampaignMapper();

    @Spy
    private CampaignParticipantFactory campaignParticipantFactory = new CampaignParticipantFactory();

    @InjectMocks
    private CampaignParticipantService campaignParticipantService;

    private static final long CAMPAIGN_ID = 10L;
    private static final long OWNER_ID = 1L;
    private static final long GM_ID = 2L;
    private static final long PLAYER_ID = 3L;
    private static final long ADDED_ID = 50L;
    private static final long OUTSIDER_ID = 99L;

    private User userWithId(long id) {
        User user = TestDataFactory.aUser("user" + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    /** Campaign whose owner is a plain PLAYER, with a separate non-owner GM. */
    private Campaign campaignWithNonGmOwner() {
        Campaign campaign = TestDataFactory.aCampaign(userWithId(OWNER_ID));
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        campaign.setWorldLore("Public lore");
        campaign.setPrivateDescription("GM secrets");
        campaign.getParticipants().add(new CampaignUser(campaign, userWithId(OWNER_ID), CampaignRole.PLAYER, "Owner"));
        campaign.getParticipants().add(new CampaignUser(campaign, userWithId(GM_ID), CampaignRole.GM, "Gm"));
        campaign.getParticipants().add(new CampaignUser(campaign, userWithId(PLAYER_ID), CampaignRole.PLAYER, "Player"));
        return campaign;
    }

    private Campaign storedCampaign() {
        Campaign campaign = campaignWithNonGmOwner();
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        return campaign;
    }

    @Nested
    @DisplayName("updateParticipantNickname()")
    class UpdateParticipantNickname {

        @Test
        @DisplayName("A participant may rename themselves without any ownership check")
        void allowsSelf() {
            Campaign campaign = storedCampaign();
            when(campaignRepository.save(campaign)).thenReturn(campaign);

            campaignParticipantService.updateParticipantNickname(CAMPAIGN_ID, PLAYER_ID, "Renamed", PLAYER_ID);

            assertThat(campaign.getParticipants())
                    .anySatisfy(p -> assertThat(p.getNickname()).isEqualTo("Renamed"));
            verifyNoInteractions(campaignPermissionService);
        }

        @Test
        @DisplayName("The campaign owner may rename another participant")
        void allowsOwnerForOthers() {
            Campaign campaign = storedCampaign();
            when(campaignPermissionService.isCampaignOwner(CAMPAIGN_ID, OWNER_ID)).thenReturn(true);
            when(campaignRepository.save(campaign)).thenReturn(campaign);

            campaignParticipantService.updateParticipantNickname(CAMPAIGN_ID, PLAYER_ID, "Renamed", OWNER_ID);

            assertThat(campaign.getParticipants())
                    .anySatisfy(p -> assertThat(p.getNickname()).isEqualTo("Renamed"));
        }

        @Test
        @DisplayName("A GM who is not the owner may not rename another participant")
        void rejectsNonOwnerForOthers() {
            storedCampaign();
            when(campaignPermissionService.isCampaignOwner(CAMPAIGN_ID, GM_ID)).thenReturn(false);

            assertThatThrownBy(() ->
                    campaignParticipantService.updateParticipantNickname(CAMPAIGN_ID, PLAYER_ID, "Renamed", GM_ID))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }

    @Nested
    @DisplayName("updateParticipantRole()")
    class UpdateParticipantRole {

        @Test
        @DisplayName("The owner may promote a participant to GM")
        void ownerPromotesParticipant() {
            Campaign campaign = storedCampaign();
            when(campaignRepository.save(campaign)).thenReturn(campaign);

            campaignParticipantService.updateParticipantRole(CAMPAIGN_ID, PLAYER_ID, "GM", OWNER_ID);

            assertThat(campaign.getParticipants())
                    .filteredOn(p -> p.getUser().getId().equals(PLAYER_ID))
                    .singleElement()
                    .satisfies(p -> assertThat(p.getRole()).isEqualTo(CampaignRole.GM));
        }

        @Test
        @DisplayName("A GM who is not the owner may not change a role")
        void rejectsNonOwner() {
            storedCampaign();

            assertThatThrownBy(() ->
                    campaignParticipantService.updateParticipantRole(CAMPAIGN_ID, PLAYER_ID, "GM", GM_ID))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        @DisplayName("An unknown role is a 400 and nothing is written")
        void rejectsUnknownRole() {
            storedCampaign();

            assertThatThrownBy(() ->
                    campaignParticipantService.updateParticipantRole(CAMPAIGN_ID, PLAYER_ID, "KING", OWNER_ID))
                    .isInstanceOf(InvalidRequestException.class);
            verify(campaignRepository, never()).save(any());
        }

        @Test
        @DisplayName("An id that is not a participant is a 404")
        void rejectsUnknownParticipant() {
            storedCampaign();

            assertThatThrownBy(() ->
                    campaignParticipantService.updateParticipantRole(CAMPAIGN_ID, OUTSIDER_ID, "GM", OWNER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("transferOwnership()")
    class TransferOwnership {

        @Test
        @DisplayName("The owner may hand the campaign to another participant")
        void ownerTransfersToParticipant() {
            Campaign campaign = storedCampaign();
            User newOwner = userWithId(PLAYER_ID);
            when(userService.findById(PLAYER_ID)).thenReturn(Optional.of(newOwner));
            when(campaignRepository.save(campaign)).thenReturn(campaign);

            CampaignResponseDTO result = campaignParticipantService.transferOwnership(CAMPAIGN_ID, PLAYER_ID, OWNER_ID);

            assertThat(campaign.getOwner()).isEqualTo(newOwner);
            assertThat(result.ownerId()).isEqualTo(PLAYER_ID);
        }

        @Test
        @DisplayName("A non-owner may not transfer ownership")
        void rejectsNonOwner() {
            storedCampaign();

            assertThatThrownBy(() ->
                    campaignParticipantService.transferOwnership(CAMPAIGN_ID, PLAYER_ID, GM_ID))
                    .isInstanceOf(UnauthorizedAccessException.class);
            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Transferring to the current owner is a 400")
        void rejectsSameOwner() {
            Campaign campaign = storedCampaign();
            when(userService.findById(OWNER_ID)).thenReturn(Optional.of(campaign.getOwner()));

            assertThatThrownBy(() ->
                    campaignParticipantService.transferOwnership(CAMPAIGN_ID, OWNER_ID, OWNER_ID))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("Transferring to someone outside the campaign is a 400")
        void rejectsNonParticipant() {
            storedCampaign();
            when(userService.findById(OUTSIDER_ID)).thenReturn(Optional.of(userWithId(OUTSIDER_ID)));

            assertThatThrownBy(() ->
                    campaignParticipantService.transferOwnership(CAMPAIGN_ID, OUTSIDER_ID, OWNER_ID))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("An unknown new owner is a 404")
        void rejectsUnknownUser() {
            storedCampaign();
            when(userService.findById(OUTSIDER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    campaignParticipantService.transferOwnership(CAMPAIGN_ID, OUTSIDER_ID, OWNER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateParticipants()")
    class UpdateParticipants {

        @Test
        @DisplayName("The owner adds a participant with the nickname and role the request asks for")
        void ownerAddsParticipant() {
            Campaign campaign = storedCampaign();
            when(userService.findById(OUTSIDER_ID)).thenReturn(Optional.of(userWithId(OUTSIDER_ID)));
            when(campaignRepository.save(campaign)).thenReturn(campaign);

            campaignParticipantService.updateParticipants(
                    CAMPAIGN_ID,
                    new UpdateParticipantsDTO(List.of(new ParticipantResponseDTO(OUTSIDER_ID, "The Wizard", "GM")), null),
                    OWNER_ID);

            assertThat(campaign.getParticipants())
                    .filteredOn(p -> p.getUser().getId().equals(OUTSIDER_ID))
                    .singleElement()
                    .satisfies(p -> {
                        assertThat(p.getNickname()).isEqualTo("The Wizard");
                        assertThat(p.getRole()).isEqualTo(CampaignRole.GM);
                    });
        }

        @Test
        @DisplayName("Adding somebody who is already a participant changes nothing")
        void skipsExistingParticipant() {
            Campaign campaign = storedCampaign();
            when(userService.findById(PLAYER_ID)).thenReturn(Optional.of(userWithId(PLAYER_ID)));
            when(campaignRepository.save(campaign)).thenReturn(campaign);

            campaignParticipantService.updateParticipants(
                    CAMPAIGN_ID,
                    new UpdateParticipantsDTO(List.of(new ParticipantResponseDTO(PLAYER_ID, "Ignored", "GM")), null),
                    OWNER_ID);

            assertThat(campaign.getParticipants())
                    .filteredOn(p -> p.getUser().getId().equals(PLAYER_ID))
                    .singleElement()
                    .satisfies(p -> assertThat(p.getNickname()).isEqualTo("Player"));
        }

        @Test
        @DisplayName("Removing a participant announces it and takes them off the roster")
        void removingParticipantAnnouncesIt() {
            Campaign campaign = storedCampaign();
            when(campaignRepository.save(campaign)).thenReturn(campaign);

            campaignParticipantService.updateParticipants(
                    CAMPAIGN_ID, new UpdateParticipantsDTO(null, List.of(PLAYER_ID)), OWNER_ID);

            verify(applicationEventPublisher).publishEvent(new ParticipantRemovedEvent(CAMPAIGN_ID, PLAYER_ID));
            assertThat(campaign.getParticipants())
                    .extracting(p -> p.getUser().getId())
                    .doesNotContain(PLAYER_ID);
        }

        @Test
        @DisplayName("An invalid removal id fails the request and saves nothing")
        void invalidRemovalSavesNothing() {
            storedCampaign();
            when(userService.findById(ADDED_ID)).thenReturn(Optional.of(userWithId(ADDED_ID)));
            UpdateParticipantsDTO request = new UpdateParticipantsDTO(
                    List.of(new ParticipantResponseDTO(ADDED_ID, "The Wizard", "PLAYER")),
                    List.of(OUTSIDER_ID));

            assertThatThrownBy(() -> campaignParticipantService.updateParticipants(CAMPAIGN_ID, request, OWNER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(campaignRepository, never()).save(any());
            verifyNoInteractions(applicationEventPublisher);
        }

        @Test
        @DisplayName("The owner cannot remove themselves")
        void ownerCannotBeRemoved() {
            storedCampaign();
            UpdateParticipantsDTO request = new UpdateParticipantsDTO(null, List.of(OWNER_ID));

            assertThatThrownBy(() -> campaignParticipantService.updateParticipants(CAMPAIGN_ID, request, OWNER_ID))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        @DisplayName("A non-owner may not change the roster")
        void rejectsNonOwner() {
            storedCampaign();
            UpdateParticipantsDTO request = new UpdateParticipantsDTO(null, List.of(PLAYER_ID));

            assertThatThrownBy(() -> campaignParticipantService.updateParticipants(CAMPAIGN_ID, request, GM_ID))
                    .isInstanceOf(UnauthorizedAccessException.class);
            verifyNoInteractions(applicationEventPublisher);
        }

        @Test
        @DisplayName("Removing somebody who is not a participant is a 404")
        void rejectsUnknownRemoval() {
            storedCampaign();
            UpdateParticipantsDTO request = new UpdateParticipantsDTO(null, List.of(OUTSIDER_ID));

            assertThatThrownBy(() -> campaignParticipantService.updateParticipants(CAMPAIGN_ID, request, OWNER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
            verifyNoInteractions(applicationEventPublisher);
        }

        @Test
        @DisplayName("A request that asks for nothing writes nothing")
        void emptyRequestChangesNothing() {
            storedCampaign();

            campaignParticipantService.updateParticipants(
                    CAMPAIGN_ID, new UpdateParticipantsDTO(null, null), OWNER_ID);

            verify(campaignRepository, never()).save(any());
            verifyNoInteractions(applicationEventPublisher);
        }
    }
}
