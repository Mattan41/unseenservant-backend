package org.kruskopf.backend.playercharacter.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.service.CampaignPermissionService;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.filestorage.FileStorageService;
import org.kruskopf.backend.playercharacter.GameCharacterMapper;
import org.kruskopf.backend.playercharacter.entity.GameCharacter;
import org.kruskopf.backend.playercharacter.repository.GameCharacterRepository;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The "remove from campaign" detach is allowed for the character's owner, the
 * campaign GM, or the campaign owner — and no one else.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameCharacterService.removeCharacterFromCampaign()")
class GameCharacterServiceTest {

    @Mock
    private GameCharacterRepository gameCharacterRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private GameCharacterMapper gameCharacterMapper;

    @Mock
    private CampaignPermissionService campaignPermissionService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private GameCharacterService gameCharacterService;

    private static final long CAMPAIGN_ID = 10L;
    private static final long CHARACTER_ID = 100L;
    private static final long CHARACTER_OWNER_ID = 1L;
    private static final long GM_ID = 2L;
    private static final long CAMPAIGN_OWNER_ID = 3L;
    private static final long STRANGER_ID = 4L;

    private User userWithId(long id) {
        User user = TestDataFactory.aUser("user" + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private GameCharacter storedCharacter() {
        Campaign campaign = TestDataFactory.aCampaign(userWithId(CAMPAIGN_OWNER_ID));
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        GameCharacter character = TestDataFactory.aCharacter(userWithId(CHARACTER_OWNER_ID), campaign);
        ReflectionTestUtils.setField(character, "id", CHARACTER_ID);
        when(gameCharacterRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(character));
        return character;
    }

    @Test
    @DisplayName("The character's owner may detach it without any campaign check")
    void allowsCharacterOwner() {
        GameCharacter character = storedCharacter();
        when(gameCharacterRepository.save(character)).thenReturn(character);

        gameCharacterService.removeCharacterFromCampaign(CHARACTER_ID, CHARACTER_OWNER_ID);

        assertThat(character.getCampaign()).isNull();
        verify(gameCharacterRepository).save(character);
        verifyNoInteractions(campaignPermissionService);
    }

    @Test
    @DisplayName("A campaign GM may detach another player's character")
    void allowsCampaignGm() {
        GameCharacter character = storedCharacter();
        when(campaignPermissionService.isGameMaster(CAMPAIGN_ID, GM_ID)).thenReturn(true);
        when(campaignPermissionService.isCampaignOwner(CAMPAIGN_ID, GM_ID)).thenReturn(false);
        when(gameCharacterRepository.save(character)).thenReturn(character);

        gameCharacterService.removeCharacterFromCampaign(CHARACTER_ID, GM_ID);

        assertThat(character.getCampaign()).isNull();
    }

    @Test
    @DisplayName("The campaign owner may detach another player's character")
    void allowsCampaignOwner() {
        GameCharacter character = storedCharacter();
        when(campaignPermissionService.isGameMaster(CAMPAIGN_ID, CAMPAIGN_OWNER_ID)).thenReturn(false);
        when(campaignPermissionService.isCampaignOwner(CAMPAIGN_ID, CAMPAIGN_OWNER_ID)).thenReturn(true);
        when(gameCharacterRepository.save(character)).thenReturn(character);

        gameCharacterService.removeCharacterFromCampaign(CHARACTER_ID, CAMPAIGN_OWNER_ID);

        assertThat(character.getCampaign()).isNull();
    }

    @Test
    @DisplayName("An unrelated participant may not detach the character")
    void rejectsUnrelatedParticipant() {
        GameCharacter character = storedCharacter();
        when(campaignPermissionService.isGameMaster(CAMPAIGN_ID, STRANGER_ID)).thenReturn(false);
        when(campaignPermissionService.isCampaignOwner(CAMPAIGN_ID, STRANGER_ID)).thenReturn(false);

        assertThatThrownBy(() -> gameCharacterService.removeCharacterFromCampaign(CHARACTER_ID, STRANGER_ID))
                .isInstanceOf(UnauthorizedAccessException.class);
        assertThat(character.getCampaign()).isNotNull();
    }
}