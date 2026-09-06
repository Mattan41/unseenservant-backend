package org.kruskopf.backend.campaign.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.repository.CampaignUserRepository;
import org.kruskopf.backend.playercharacter.entity.PlayerCharacter;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.ProviderType;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignPermissionService Unit Tests")
class CampaignPermissionServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignUserRepository campaignUserRepository;

    @InjectMocks
    private CampaignPermissionService permissionService;

    private User userWithId(long id) {
        User user = new User(
                "provider-" + id, ProviderType.GOOGLE,
                "user" + id + "@test.com", "User " + id,
                "user" + id, UserRole.USER, "password"
        );
        user.setId(id);
        return user;
    }

    private Campaign campaignOwnedBy(User owner, long campaignId) {
        Campaign campaign = new Campaign("Test Campaign", "Description");
        campaign.setOwner(owner);
        ReflectionTestUtils.setField(campaign, "id", campaignId);
        return campaign;
    }

    @Nested
    @DisplayName("isOwner()")
    class IsOwner {

        @Test
        @DisplayName("Returns true when user is the campaign owner")
        void returnsTrue_whenUserIsOwner() {
            // Arrange
            User owner = userWithId(1L);
            Campaign campaign = campaignOwnedBy(owner, 10L);
            when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));

            // Act & Assert
            assertThat(permissionService.isOwner(10L, 1L)).isTrue();
        }

        @Test
        @DisplayName("Returns false when user is not the campaign owner")
        void returnsFalse_whenUserIsNotOwner() {
            // Arrange
            User owner = userWithId(2L);
            Campaign campaign = campaignOwnedBy(owner, 10L);
            when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));

            // Act & Assert
            assertThat(permissionService.isOwner(10L, 1L)).isFalse();
        }

        @Test
        @DisplayName("Returns false when campaign does not exist")
        void returnsFalse_whenCampaignNotFound() {
            // Arrange
            when(campaignRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThat(permissionService.isOwner(99L, 1L)).isFalse();
        }
    }

    @Nested
    @DisplayName("isParticipant()")
    class IsParticipant {

        @Test
        @DisplayName("Returns true when user is a participant")
        void returnsTrue_whenUserIsParticipant() {
            // Arrange
            when(campaignUserRepository.existsByCampaignIdAndUserId(10L, 1L)).thenReturn(true);

            // Act & Assert
            assertThat(permissionService.isParticipant(10L, 1L)).isTrue();
        }

        @Test
        @DisplayName("Returns false when user is not a participant")
        void returnsFalse_whenUserIsNotParticipant() {
            // Arrange
            when(campaignUserRepository.existsByCampaignIdAndUserId(10L, 1L)).thenReturn(false);

            // Act & Assert
            assertThat(permissionService.isParticipant(10L, 1L)).isFalse();
        }
    }

    @Nested
    @DisplayName("hasRole()")
    class HasRole {

        @Test
        @DisplayName("Returns true when user has GM role")
        void returnsTrue_whenUserHasGmRole() {
            // Arrange
            when(campaignUserRepository.existsByCampaignIdAndUserIdAndRole(10L, 1L, CampaignRole.GM)).thenReturn(true);

            // Act & Assert
            assertThat(permissionService.hasRole(10L, 1L, CampaignRole.GM)).isTrue();
        }

        @Test
        @DisplayName("Returns false when user does not have GM role")
        void returnsFalse_whenUserDoesNotHaveGmRole() {
            // Arrange
            when(campaignUserRepository.existsByCampaignIdAndUserIdAndRole(10L, 1L, CampaignRole.GM)).thenReturn(false);

            // Act & Assert
            assertThat(permissionService.hasRole(10L, 1L, CampaignRole.GM)).isFalse();
        }

        @Test
        @DisplayName("Returns true when user has PLAYER role")
        void returnsTrue_whenUserHasPlayerRole() {
            // Arrange
            when(campaignUserRepository.existsByCampaignIdAndUserIdAndRole(10L, 1L, CampaignRole.PLAYER)).thenReturn(true);

            // Act & Assert
            assertThat(permissionService.hasRole(10L, 1L, CampaignRole.PLAYER)).isTrue();
        }
    }

    @Nested
    @DisplayName("isGameMaster()")
    class IsGameMaster {

        @Test
        @DisplayName("Returns true when user is a game master")
        void returnsTrue_whenUserIsGm() {
            // Arrange
            when(campaignUserRepository.existsByCampaignIdAndUserIdAndRole(10L, 1L, CampaignRole.GM)).thenReturn(true);

            // Act & Assert
            assertThat(permissionService.isGameMaster(10L, 1L)).isTrue();
        }

        @Test
        @DisplayName("Returns false when user is not a game master")
        void returnsFalse_whenUserIsNotGm() {
            // Arrange
            when(campaignUserRepository.existsByCampaignIdAndUserIdAndRole(10L, 1L, CampaignRole.GM)).thenReturn(false);

            // Act & Assert
            assertThat(permissionService.isGameMaster(10L, 1L)).isFalse();
        }

        @Test
        @DisplayName("Delegates to hasRole with GM role, never checks PLAYER role")
        void delegatesToHasRole_withGmRole() {
            // Arrange
            when(campaignUserRepository.existsByCampaignIdAndUserIdAndRole(10L, 1L, CampaignRole.GM)).thenReturn(true);

            // Act
            permissionService.isGameMaster(10L, 1L);

            // Assert
            verify(campaignUserRepository).existsByCampaignIdAndUserIdAndRole(10L, 1L, CampaignRole.GM);
            verify(campaignUserRepository, never()).existsByCampaignIdAndUserIdAndRole(any(), any(), eq(CampaignRole.PLAYER));
        }
    }

    @Nested
    @DisplayName("canViewCharacter()")
    class CanViewCharacter {

        @Test
        @DisplayName("Returns true when viewer is the character owner")
        void returnsTrue_forOwner() {
            // Arrange
            User owner = userWithId(1L);
            PlayerCharacter character = TestDataFactory.aCharacter(owner);

            // Act & Assert
            assertThat(permissionService.canViewCharacter(character, 1L)).isTrue();
            verifyNoInteractions(campaignUserRepository);
        }

        @Test
        @DisplayName("Returns true when viewer is the GM of the character's campaign")
        void returnsTrue_forGm_whenCharacterInCampaign() {
            // Arrange
            User owner = userWithId(1L);
            Campaign campaign = campaignOwnedBy(owner, 10L);

            PlayerCharacter character = TestDataFactory.aCharacter(owner, campaign);

            when(campaignUserRepository.existsByCampaignIdAndUserIdAndRole(10L, 2L, CampaignRole.GM)).thenReturn(true);

            // Act & Assert
            assertThat(permissionService.canViewCharacter(character, 2L)).isTrue();
        }

        @Test
        @DisplayName("Returns false when viewer is neither the owner nor a GM")
        void returnsFalse_forNonOwnerNonGm_whenCharacterInCampaign() {
            // Arrange
            User owner = userWithId(1L);
            Campaign campaign = campaignOwnedBy(owner, 10L);

            PlayerCharacter character = TestDataFactory.aCharacter(owner, campaign);

            when(campaignUserRepository.existsByCampaignIdAndUserIdAndRole(10L, 2L, CampaignRole.GM)).thenReturn(false);

            // Act & Assert
            assertThat(permissionService.canViewCharacter(character, 2L)).isFalse();
        }

        @Test
        @DisplayName("Returns false when viewer is not the owner and character has no campaign")
        void returnsFalse_whenCharacterHasNoCampaign() {
            // Arrange
            User owner = userWithId(1L);
            PlayerCharacter character = TestDataFactory.aCharacter(owner);
            character.setCampaign(null);

            // Act & Assert
            assertThat(permissionService.canViewCharacter(character, 2L)).isFalse();
            verifyNoInteractions(campaignUserRepository);
        }
    }

    @Nested
    @DisplayName("canEditCharacter()")
    class CanEditCharacter {

        @Test
        @DisplayName("Returns true when viewer is the character owner")
        void returnsTrue_forOwner() {
            // Arrange
            User owner = userWithId(1L);
            PlayerCharacter character = TestDataFactory.aCharacter(owner);

            // Act & Assert
            assertThat(permissionService.canEditCharacter(character, 1L)).isTrue();
        }

        @Test
        @DisplayName("Returns false when viewer is not the character owner")
        void returnsFalse_forNonOwner() {
            // Arrange
            User owner = userWithId(1L);
            PlayerCharacter character = TestDataFactory.aCharacter(owner);

            // Act & Assert
            assertThat(permissionService.canEditCharacter(character, 2L)).isFalse();
        }
    }
}
