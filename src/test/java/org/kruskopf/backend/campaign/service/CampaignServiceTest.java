package org.kruskopf.backend.campaign.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kruskopf.backend.campaign.dto.CampaignResponseDTO;
import org.kruskopf.backend.campaign.dto.CampaignUpdateDTO;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.filestorage.StoredFileTransaction;
import org.kruskopf.backend.playercharacter.service.GameCharacterService;
import org.kruskopf.backend.ship.service.ShipService;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.service.UserService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Permission-enforcement tests for the two axes: ownership → administration,
 * table role (GM) → world-building. The owner in these fixtures is a PLAYER, so
 * a non-GM owner must not be treated as a GM.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignService permission enforcement")
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private UserService userService;

    @Mock
    private CampaignPermissionService campaignPermissionService;

    @Mock
    private GameCharacterService gameCharacterService;

    @Mock
    private StoredFileTransaction storedFileTransaction;

    @Mock
    private ShipService shipService;

    @InjectMocks
    private CampaignService campaignService;

    private static final long CAMPAIGN_ID = 10L;
    private static final long OWNER_ID = 1L;
    private static final long GM_ID = 2L;
    private static final long PLAYER_ID = 3L;

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
    @DisplayName("updateCampaign()")
    class UpdateCampaign {

        @Test
        @DisplayName("Allows a campaign GM to edit the campaign details")
        void allowsGm() {
            Campaign campaign = storedCampaign();
            when(campaignPermissionService.isGameMaster(CAMPAIGN_ID, GM_ID)).thenReturn(true);
            when(campaignRepository.save(campaign)).thenReturn(campaign);

            CampaignUpdateDTO dto = new CampaignUpdateDTO("New name", "desc", "lore", "notes", null);
            CampaignResponseDTO result = campaignService.updateCampaign(CAMPAIGN_ID, dto, GM_ID);

            assertThat(result.name()).isEqualTo("New name");
            assertThat(result.privateDescription()).isEqualTo("notes");
        }

        @Test
        @DisplayName("Rejects a non-GM owner with 403")
        void rejectsNonGmOwner() {
            storedCampaign();
            when(campaignPermissionService.isGameMaster(CAMPAIGN_ID, OWNER_ID)).thenReturn(false);

            CampaignUpdateDTO dto = new CampaignUpdateDTO("New name", "desc", "lore", "notes", null);

            assertThatThrownBy(() -> campaignService.updateCampaign(CAMPAIGN_ID, dto, OWNER_ID))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }

    @Nested
    @DisplayName("uploadCampaignImage()")
    class UploadCampaignImage {

        @Test
        @DisplayName("Rejects a non-GM and never touches file storage")
        void rejectsNonGm() {
            storedCampaign();
            when(campaignPermissionService.isGameMaster(CAMPAIGN_ID, PLAYER_ID)).thenReturn(false);

            assertThatThrownBy(() -> campaignService.uploadCampaignImage(CAMPAIGN_ID, null, PLAYER_ID))
                    .isInstanceOf(UnauthorizedAccessException.class);
            verifyNoInteractions(storedFileTransaction);
        }
    }

    @Nested
    @DisplayName("updateParticipantNickname()")
    class UpdateParticipantNickname {

        @Test
        @DisplayName("A participant may rename themselves without any ownership check")
        void allowsSelf() {
            Campaign campaign = storedCampaign();
            when(campaignRepository.save(campaign)).thenReturn(campaign);

            campaignService.updateParticipantNickname(CAMPAIGN_ID, PLAYER_ID, "Renamed", PLAYER_ID);

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

            campaignService.updateParticipantNickname(CAMPAIGN_ID, PLAYER_ID, "Renamed", OWNER_ID);

            assertThat(campaign.getParticipants())
                    .anySatisfy(p -> assertThat(p.getNickname()).isEqualTo("Renamed"));
        }

        @Test
        @DisplayName("A GM who is not the owner may not rename another participant")
        void rejectsNonOwnerForOthers() {
            storedCampaign();
            when(campaignPermissionService.isCampaignOwner(CAMPAIGN_ID, GM_ID)).thenReturn(false);

            assertThatThrownBy(() ->
                    campaignService.updateParticipantNickname(CAMPAIGN_ID, PLAYER_ID, "Renamed", GM_ID))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }

    @Nested
    @DisplayName("GM notes visibility")
    class GmNotesVisibility {

        @Test
        @DisplayName("A GM receives the private description")
        void gmSeesPrivateDescription() {
            storedCampaign();
            when(campaignPermissionService.isParticipant(CAMPAIGN_ID, GM_ID)).thenReturn(true);

            CampaignResponseDTO result = campaignService.getCampaignByIdIfAuthorized(CAMPAIGN_ID, GM_ID);

            assertThat(result.privateDescription()).isEqualTo("GM secrets");
        }

        @Test
        @DisplayName("A non-GM owner does not receive the private description but still gets world lore")
        void nonGmOwnerDoesNotSeePrivateDescription() {
            storedCampaign();
            when(campaignPermissionService.isParticipant(CAMPAIGN_ID, OWNER_ID)).thenReturn(true);

            CampaignResponseDTO result = campaignService.getCampaignByIdIfAuthorized(CAMPAIGN_ID, OWNER_ID);

            assertThat(result.privateDescription()).isNull();
            assertThat(result.worldLore()).isEqualTo("Public lore");
        }
    }
}
