package org.kruskopf.backend.campaign.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.AbstractIntegrationTest;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.repository.CampaignUserRepository;
import org.kruskopf.backend.playercharacter.entity.PlayerCharacter;
import org.kruskopf.backend.playercharacter.repository.PlayerCharacterRepository;
import org.kruskopf.backend.user.entity.ProviderType;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CampaignPermissionService.
 * <p>
 * Verifies permission logic end-to-end against a real MySQL Testcontainer,
 * covering the scenarios that the unit test covers with mocks.
 */
@Transactional
@DisplayName("CampaignPermissionService Integration Tests")
class CampaignPermissionServiceIT extends AbstractIntegrationTest {

    private final CampaignPermissionService permissionService;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignUserRepository campaignUserRepository;
    private final PlayerCharacterRepository playerCharacterRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    CampaignPermissionServiceIT(
            CampaignPermissionService permissionService,
            UserRepository userRepository,
            CampaignRepository campaignRepository,
            CampaignUserRepository campaignUserRepository,
            PlayerCharacterRepository playerCharacterRepository) {
        this.permissionService = permissionService;
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.campaignUserRepository = campaignUserRepository;
        this.playerCharacterRepository = playerCharacterRepository;
    }

    private User owner;
    private User gm;
    private User player;
    private User outsider;
    private Campaign campaign;
    private PlayerCharacter character;

    @BeforeEach
    void setUp() {
        // Clean up in FK-safe order
        playerCharacterRepository.deleteAll();
        campaignUserRepository.deleteAll();
        campaignRepository.deleteAll();
        userRepository.deleteAll();

        // Create users
        owner = userRepository.save(new User(
                "google-owner", ProviderType.GOOGLE, "owner@it-test.com",
                "Owner", "owner-it", UserRole.USER, "password"));
        gm = userRepository.save(new User(
                "google-gm", ProviderType.GOOGLE, "gm@it-test.com",
                "Game Master", "gm-it", UserRole.USER, "password"));
        player = userRepository.save(new User(
                "google-player", ProviderType.GOOGLE, "player@it-test.com",
                "Player", "player-it", UserRole.USER, "password"));
        outsider = userRepository.save(new User(
                "google-outsider", ProviderType.GOOGLE, "outsider@it-test.com",
                "Outsider", "outsider-it", UserRole.USER, "password"));

        // Create campaign with owner
        campaign = new Campaign("IT Campaign", "Integration test campaign");
        campaign.setOwner(owner);
        campaign = campaignRepository.save(campaign);

        // Add participants (owner, gm, player — outsider is deliberately excluded)
        campaignUserRepository.save(new CampaignUser(campaign, owner, CampaignRole.PLAYER, "The Owner"));
        campaignUserRepository.save(new CampaignUser(campaign, gm, CampaignRole.GM, "The GM"));
        campaignUserRepository.save(new CampaignUser(campaign, player, CampaignRole.PLAYER, "The Player"));

        // Create a character owned by player, assigned to the campaign
        character = new PlayerCharacter();
        character.setOwner(player);
        character.setCampaign(campaign);
        character.setName("Test Hero");
        character.setCharacterClass("Fighter");
        character.setRace("Human");
        character = playerCharacterRepository.save(character);
    }

    @Nested
    @DisplayName("isOwner()")
    class IsOwner {

        @Test
        @DisplayName("Returns true for the campaign owner")
        void returnsTrue_forOwner() {
            assertThat(permissionService.isOwner(campaign.getId(), owner.getId())).isTrue();
        }

        @Test
        @DisplayName("Returns false for a participant who is not the owner")
        void returnsFalse_forParticipantNonOwner() {
            assertThat(permissionService.isOwner(campaign.getId(), player.getId())).isFalse();
        }

        @Test
        @DisplayName("Returns false for a non-existent campaign")
        void returnsFalse_forNonExistentCampaign() {
            assertThat(permissionService.isOwner(99999L, owner.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("isParticipant()")
    class IsParticipant {

        @Test
        @DisplayName("Returns true for the campaign owner")
        void returnsTrue_forOwner() {
            assertThat(permissionService.isParticipant(campaign.getId(), owner.getId())).isTrue();
        }

        @Test
        @DisplayName("Returns true for a GM participant")
        void returnsTrue_forGm() {
            assertThat(permissionService.isParticipant(campaign.getId(), gm.getId())).isTrue();
        }

        @Test
        @DisplayName("Returns true for a PLAYER participant")
        void returnsTrue_forPlayer() {
            assertThat(permissionService.isParticipant(campaign.getId(), player.getId())).isTrue();
        }

        @Test
        @DisplayName("Returns false for a user not in the campaign")
        void returnsFalse_forOutsider() {
            assertThat(permissionService.isParticipant(campaign.getId(), outsider.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("hasRole()")
    class HasRole {

        @Test
        @DisplayName("Returns true for GM role when user is a GM")
        void returnsTrue_gmRole_forGm() {
            assertThat(permissionService.hasRole(campaign.getId(), gm.getId(), CampaignRole.GM)).isTrue();
        }

        @Test
        @DisplayName("Returns false for GM role when user is a PLAYER")
        void returnsFalse_gmRole_forPlayer() {
            assertThat(permissionService.hasRole(campaign.getId(), player.getId(), CampaignRole.GM)).isFalse();
        }

        @Test
        @DisplayName("Returns true for PLAYER role when user is a PLAYER")
        void returnsTrue_playerRole_forPlayer() {
            assertThat(permissionService.hasRole(campaign.getId(), player.getId(), CampaignRole.PLAYER)).isTrue();
        }

        @Test
        @DisplayName("Returns false for PLAYER role when user is a GM")
        void returnsFalse_playerRole_forGm() {
            assertThat(permissionService.hasRole(campaign.getId(), gm.getId(), CampaignRole.PLAYER)).isFalse();
        }
    }

    @Nested
    @DisplayName("isGameMaster()")
    class IsGameMaster {

        @Test
        @DisplayName("Returns true for a GM participant")
        void returnsTrue_forGm() {
            assertThat(permissionService.isGameMaster(campaign.getId(), gm.getId())).isTrue();
        }

        @Test
        @DisplayName("Returns false for a PLAYER participant")
        void returnsFalse_forPlayer() {
            assertThat(permissionService.isGameMaster(campaign.getId(), player.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("canViewCharacter()")
    class CanViewCharacter {

        @Test
        @DisplayName("Returns true for the character owner")
        void returnsTrue_forCharacterOwner() {
            assertThat(permissionService.canViewCharacter(character, player.getId())).isTrue();
        }

        @Test
        @DisplayName("Returns true for the GM of the character's campaign")
        void returnsTrue_forGmOfCampaign() {
            assertThat(permissionService.canViewCharacter(character, gm.getId())).isTrue();
        }

        @Test
        @DisplayName("Returns false for a user outside the campaign")
        void returnsFalse_forOutsider() {
            assertThat(permissionService.canViewCharacter(character, outsider.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("canEditCharacter()")
    class CanEditCharacter {

        @Test
        @DisplayName("Returns true for the character owner")
        void returnsTrue_forCharacterOwner() {
            assertThat(permissionService.canEditCharacter(character, player.getId())).isTrue();
        }

        @Test
        @DisplayName("Returns false for the GM (who is not the owner)")
        void returnsFalse_forGm() {
            assertThat(permissionService.canEditCharacter(character, gm.getId())).isFalse();
        }
    }
}
