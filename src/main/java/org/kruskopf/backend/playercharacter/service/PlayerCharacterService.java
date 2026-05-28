package org.kruskopf.backend.playercharacter.service;

import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.repository.CampaignUserRepository;
import org.kruskopf.backend.campaign.service.CampaignService;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.filestorage.FileStorageService;
import org.kruskopf.backend.playercharacter.PlayerCharacterMapper;
import org.kruskopf.backend.playercharacter.dto.PlayerCharacterInputDTO;
import org.kruskopf.backend.playercharacter.dto.PlayerCharacterOutputDTO;
import org.kruskopf.backend.playercharacter.entity.PlayerCharacter;
import org.kruskopf.backend.playercharacter.repository.PlayerCharacterRepository;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlayerCharacterService {
    private final PlayerCharacterRepository playerCharacterRepository;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final PlayerCharacterMapper playerCharacterMapper;
    private final CampaignUserRepository campaignUserRepository;
    private final FileStorageService fileStorageService;

    public PlayerCharacterService(
            PlayerCharacterRepository playerCharacterRepository,
            UserRepository userRepository,
            CampaignRepository campaignRepository,
            PlayerCharacterMapper playerCharacterMapper, CampaignUserRepository campaignUserRepository, FileStorageService fileStorageService) {
        this.playerCharacterRepository = playerCharacterRepository;
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.playerCharacterMapper = playerCharacterMapper;
        this.campaignUserRepository = campaignUserRepository;
        this.fileStorageService = fileStorageService;
    }

    public PlayerCharacterOutputDTO createCharacter(PlayerCharacterInputDTO inputDTO) {
        User owner = userRepository.findById(inputDTO.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + inputDTO.ownerId()));

        PlayerCharacter character = playerCharacterMapper.toEntity(inputDTO, owner, null);
        PlayerCharacter savedCharacter = playerCharacterRepository.save(character);

        return playerCharacterMapper.toOutputDTO(savedCharacter);
    }
    // Only for test/dummy data with startuprunner
    public PlayerCharacterOutputDTO createCharacterFromDto(PlayerCharacterInputDTO inputDTO) {
        User owner = userRepository.findById(inputDTO.ownerId())
                .orElseThrow(() ->  new ResourceNotFoundException("User not found with id: " + inputDTO.ownerId()));

        Campaign campaign = null;
        if (inputDTO.campaignId() != null) {
            campaign = campaignRepository.findById(inputDTO.campaignId())
                    .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + inputDTO.campaignId()));
        }

        PlayerCharacter character = playerCharacterMapper.toEntity(inputDTO, owner, campaign);
        PlayerCharacter savedCharacter = playerCharacterRepository.save(character);

        return playerCharacterMapper.toOutputDTO(savedCharacter);
    }



    public List<PlayerCharacterOutputDTO> getAllCharacters() {
        return playerCharacterRepository.findAll()
                .stream()
                .map(playerCharacterMapper::toOutputDTO)
                .toList();
    }

    public List<PlayerCharacterOutputDTO> getCharactersWithoutCampaign(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return playerCharacterRepository.findByOwner(user)
                .stream()
                .filter(character -> character.getCampaign() == null)
                .map(playerCharacterMapper::toOutputDTO)
                .collect(Collectors.toList());
    }

    public List<PlayerCharacterOutputDTO> getCharactersByUserId(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return playerCharacterRepository.findByOwner(user)
                .stream()
                .map(playerCharacterMapper::toOutputDTO)
                .collect(Collectors.toList());
    }

    public PlayerCharacterOutputDTO getCharacterById(long id, long userId) {
        PlayerCharacterOutputDTO character = playerCharacterRepository.findById(id)
                .map(playerCharacterMapper::toOutputDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found with id: " + id));

        if (character.campaignId() == null) {
            if (isNotOwner(character, userId)) {
                throw new UnauthorizedAccessException("User is not the owner of the character");
            }
        } else {
            if (isNotOwner(character, userId) && !isUserGameMaster(character.campaignId(), userId)) {
                throw new UnauthorizedAccessException("User is not the owner of the character, nor GM of the campaign");
            }
        }
        return character;
    }


    @Transactional
    public PlayerCharacterOutputDTO updateCharacter(long characterId, PlayerCharacterInputDTO inputDTO, long userId) {
        PlayerCharacter character = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found with id: " + characterId));

        if (!character.getOwner().getId().equals(userId)) {
            throw new UnauthorizedAccessException("User does not own this character");
        }

        // Use mapper to update fields
        playerCharacterMapper.patchEntity(character, inputDTO);

        PlayerCharacter updatedCharacter = playerCharacterRepository.save(character);
        return playerCharacterMapper.toOutputDTO(updatedCharacter);
    }

    public PlayerCharacterOutputDTO uploadCharacterImage(long characterId, MultipartFile file, long userId) {
        PlayerCharacter character = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));

        if (!character.getOwner().getId().equals(userId)) {
            throw new UnauthorizedAccessException("User does not own this character");
        }

        try {
            String fileName = fileStorageService.storeFile(file, "character_" + characterId);
            character.setImageUrl("/images/" + fileName);
            PlayerCharacter savedCharacter = playerCharacterRepository.save(character);
            return playerCharacterMapper.toOutputDTO(savedCharacter);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }


    @Transactional
    public PlayerCharacterOutputDTO addCharacterToCampaign(long characterId, long campaignId, long userId) {
        PlayerCharacter character = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found with id: " + characterId));

        if (!character.getOwner().getId().equals(userId)) {
            throw new UnauthorizedAccessException("User does not own this character");
        }

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + campaignId));

        // verify that the user has access to the campaign
        boolean isParticipant = campaignUserRepository.existsByCampaignIdAndUserId(campaignId, userId);

        if (!isParticipant) {
            throw new UnauthorizedAccessException("User is not a participant in this campaign");
        }

        character.setCampaign(campaign);
        PlayerCharacter savedCharacter = playerCharacterRepository.save(character);

        return playerCharacterMapper.toOutputDTO(savedCharacter);
    }

    @Transactional
    public PlayerCharacterOutputDTO removeCharacterFromCampaign(long characterId, long userId) {
        PlayerCharacter character = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found with id: " + characterId));

        // Verify that the user is the owner of the character
        if (!character.getOwner().getId().equals(userId)) {
            throw new UnauthorizedAccessException("User does not own this character");
        }

        // remove the character from the campaign
        character.setCampaign(null);
        PlayerCharacter savedCharacter = playerCharacterRepository.save(character);

        return playerCharacterMapper.toOutputDTO(savedCharacter);
    }

    //delete one character
    @Transactional
    public void deleteCharacter(long characterId, long userId) {

        PlayerCharacter character = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found with id: " + characterId));

        // Verify that the user is the owner of the character
        if (!character.getOwner().getId().equals(userId)) {
            throw new UnauthorizedAccessException("User does not own this character");
        }
        // delete the character
        playerCharacterRepository.delete(character);

    }

    /**
     * Removes all character-campaign associations for a specific user from a given campaign.
     *
     * <p>IMPORTANT: This method is intended to be called only from {@link CampaignService} where
     * proper authorization checks are already performed. It does not perform any permission
     * validation on its own and should not be exposed directly via controllers.</p>
     *
     * <p>This method is part of the participant removal process and ensures that when a user
     * is removed from a campaign, all their characters are properly disassociated from that campaign.</p>
     *
     * @param userId     The ID of the user whose characters should be disassociated from the campaign
     * @param campaignId The ID of the campaign from which to remove the character associations
     */
    @Transactional
    public void removeAllCharactersFromCampaign(long userId, long campaignId) {
        playerCharacterRepository.removeCampaignReferenceForUser(userId, campaignId);
    }

    public List<PlayerCharacterOutputDTO> getCharactersByCampaignId(long campaignId, long userId) {

        if (!campaignRepository.existsById(campaignId)) {
            throw new ResourceNotFoundException("Campaign not found with id: " + campaignId);
        }

        boolean isParticipant = campaignUserRepository.existsByCampaignIdAndUserId(campaignId, userId);
        if (!isParticipant) {
            throw new UnauthorizedAccessException("User is not a participant in this campaign");
        }
        return playerCharacterRepository.findByCampaignId(campaignId)
                .stream()
                .map(playerCharacterMapper::toOutputDTO)
                .collect(Collectors.toList());
    }

    // helper methods

    private boolean isUserGameMaster(long campaignId, long userId) {
        campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + campaignId));

        return campaignUserRepository.existsByCampaignIdAndUserIdAndRole(
                campaignId, userId, CampaignRole.GM
        );
    }


    private boolean isNotOwner(PlayerCharacterOutputDTO character, long userId) {
        return !character.ownerId().equals(userId);
    }


}
