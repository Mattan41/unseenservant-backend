package org.kruskopf.backend.playercharacter.service;

import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.service.CampaignPermissionService;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.filestorage.FileStorageService;
import org.kruskopf.backend.playercharacter.GameCharacterMapper;
import org.kruskopf.backend.playercharacter.dto.CharacterInputDTO;
import org.kruskopf.backend.playercharacter.dto.CharacterOutputDTO;
import org.kruskopf.backend.playercharacter.entity.GameCharacter;
import org.kruskopf.backend.playercharacter.repository.GameCharacterRepository;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GameCharacterService {
    private final GameCharacterRepository gameCharacterRepository;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final GameCharacterMapper gameCharacterMapper;
    private final CampaignPermissionService campaignPermissionService;
    private final FileStorageService fileStorageService;

    public GameCharacterService(
            GameCharacterRepository gameCharacterRepository,
            UserRepository userRepository,
            CampaignRepository campaignRepository,
            GameCharacterMapper gameCharacterMapper,
            CampaignPermissionService campaignPermissionService,
            FileStorageService fileStorageService) {
        this.gameCharacterRepository = gameCharacterRepository;
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.gameCharacterMapper = gameCharacterMapper;
        this.campaignPermissionService = campaignPermissionService;
        this.fileStorageService = fileStorageService;
    }

    public CharacterOutputDTO createCharacter(CharacterInputDTO inputDTO) {
        User owner = userRepository.findById(inputDTO.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + inputDTO.ownerId()));

        GameCharacter character = gameCharacterMapper.toEntity(inputDTO, owner, null);
        GameCharacter savedCharacter = gameCharacterRepository.save(character);

        return gameCharacterMapper.toOutputDTO(savedCharacter);
    }

    public List<CharacterOutputDTO> getAllCharacters() {
        return gameCharacterRepository.findAll()
                .stream()
                .map(gameCharacterMapper::toOutputDTO)
                .toList();
    }

    public List<CharacterOutputDTO> getCharactersWithoutCampaign(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return gameCharacterRepository.findByOwner(user)
                .stream()
                .filter(character -> character.getCampaign() == null)
                .map(gameCharacterMapper::toOutputDTO)
                .collect(Collectors.toList());
    }

    public List<CharacterOutputDTO> getCharactersByUserId(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return gameCharacterRepository.findByOwner(user)
                .stream()
                .map(gameCharacterMapper::toOutputDTO)
                .collect(Collectors.toList());
    }

    public CharacterOutputDTO getCharacterById(long id, long userId) {
        CharacterOutputDTO character = gameCharacterRepository.findById(id)
                .map(gameCharacterMapper::toOutputDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found with id: " + id));

        if (character.campaignId() == null) {
            if (isNotOwner(character, userId)) {
                throw new UnauthorizedAccessException("User is not the owner of the character");
            }
        } else {
            if (isNotOwner(character, userId) && !campaignPermissionService.isGameMaster(character.campaignId(), userId)) {
                throw new UnauthorizedAccessException("User is not the owner of the character, nor GM of the campaign");
            }
        }
        return character;
    }

    @Transactional
    public CharacterOutputDTO updateCharacter(long characterId, CharacterInputDTO inputDTO, long userId) {
        GameCharacter character = gameCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found with id: " + characterId));

        if (!character.getOwner().getId().equals(userId)) {
            throw new UnauthorizedAccessException("User does not own this character");
        }

        // Use mapper to update fields
        gameCharacterMapper.patchEntity(character, inputDTO);

        GameCharacter updatedCharacter = gameCharacterRepository.save(character);
        return gameCharacterMapper.toOutputDTO(updatedCharacter);
    }

    public CharacterOutputDTO uploadCharacterImage(long characterId, MultipartFile file, long userId) {
        GameCharacter character = gameCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));

        if (!character.getOwner().getId().equals(userId)) {
            throw new UnauthorizedAccessException("User does not own this character");
        }

        try {
            String fileName = fileStorageService.storeFile(file, "character_" + characterId, "IMAGE");
            character.setImageUrl("/images/" + fileName);
            GameCharacter savedCharacter = gameCharacterRepository.save(character);
            return gameCharacterMapper.toOutputDTO(savedCharacter);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Transactional
    public CharacterOutputDTO addCharacterToCampaign(long characterId, long campaignId, long userId) {
        GameCharacter character = gameCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found with id: " + characterId));

        if (!character.getOwner().getId().equals(userId)) {
            throw new UnauthorizedAccessException("User does not own this character");
        }

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + campaignId));

        if (!campaignPermissionService.isParticipant(campaignId, userId)) {
            throw new UnauthorizedAccessException("User is not a participant in this campaign");
        }

        character.setCampaign(campaign);
        GameCharacter savedCharacter = gameCharacterRepository.save(character);

        return gameCharacterMapper.toOutputDTO(savedCharacter);
    }

    @Transactional
    public CharacterOutputDTO removeCharacterFromCampaign(long characterId, long userId) {
        GameCharacter character = gameCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found with id: " + characterId));

        // Verify that the user is the owner of the character
        if (!character.getOwner().getId().equals(userId)) {
            throw new UnauthorizedAccessException("User does not own this character");
        }

        // remove the character from the campaign
        character.setCampaign(null);
        GameCharacter savedCharacter = gameCharacterRepository.save(character);

        return gameCharacterMapper.toOutputDTO(savedCharacter);
    }

    //delete one character
    @Transactional
    public void deleteCharacter(long characterId, long userId) {

        GameCharacter character = gameCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found with id: " + characterId));

        // Verify that the user is the owner of the character
        if (!character.getOwner().getId().equals(userId)) {
            throw new UnauthorizedAccessException("User does not own this character");
        }
        // delete the character
        gameCharacterRepository.delete(character);

    }

    /**
     * Removes all character-campaign associations for a specific user from a given campaign.
     * See the original JavaDoc in the history for full context.
     */
    @Transactional
    public void removeAllCharactersFromCampaign(long userId, long campaignId) {
        gameCharacterRepository.removeCampaignReferenceForUser(userId, campaignId);
    }

    public List<CharacterOutputDTO> getCharactersByCampaignId(long campaignId, long userId) {

        if (!campaignRepository.existsById(campaignId)) {
            throw new ResourceNotFoundException("Campaign not found with id: " + campaignId);
        }

        if (!campaignPermissionService.isParticipant(campaignId, userId)) {
            throw new UnauthorizedAccessException("User is not a participant in this campaign");
        }
        return gameCharacterRepository.findByCampaignId(campaignId)
                .stream()
                .map(gameCharacterMapper::toOutputDTO)
                .collect(Collectors.toList());
    }

    // helper methods

    private boolean isNotOwner(CharacterOutputDTO character, long userId) {
        return !character.ownerId().equals(userId);
    }
}
