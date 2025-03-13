package org.kruskopf.backend.playercharacter.service;

import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.playercharacter.PlayerCharacterMapper;
import org.kruskopf.backend.playercharacter.dto.PlayerCharacterInputDTO;
import org.kruskopf.backend.playercharacter.dto.PlayerCharacterOutputDTO;
import org.kruskopf.backend.playercharacter.entity.PlayerCharacter;
import org.kruskopf.backend.playercharacter.repository.PlayerCharacterRepository;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayerCharacterService {
    private final PlayerCharacterRepository playerCharacterRepository;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final PlayerCharacterMapper playerCharacterMapper;

    public PlayerCharacterService(
            PlayerCharacterRepository playerCharacterRepository,
            UserRepository userRepository,
            CampaignRepository campaignRepository,
            PlayerCharacterMapper playerCharacterMapper) {
        this.playerCharacterRepository = playerCharacterRepository;
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.playerCharacterMapper = playerCharacterMapper;
    }

    public List<PlayerCharacterOutputDTO> getAllCharacters() {
        return playerCharacterRepository.findAll()
                .stream()
                .map(playerCharacterMapper::toOutputDTO)
                .toList();
    }

    public PlayerCharacterOutputDTO getCharacterById(Long id) {
        return playerCharacterRepository.findById(id)
                .map(playerCharacterMapper::toOutputDTO)
                .orElseThrow(() -> new RuntimeException("Character not found!"));
    }

    public PlayerCharacterOutputDTO createCharacter(PlayerCharacterInputDTO inputDTO) {
        User owner = userRepository.findById(inputDTO.ownerId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Campaign campaign = null;
        if (inputDTO.campaignId() != null) {
            campaign = campaignRepository.findById(inputDTO.campaignId())
                    .orElseThrow(() -> new RuntimeException("Campaign not found"));
        }
        PlayerCharacter playerCharacter = playerCharacterMapper.toEntity(inputDTO, owner, campaign);
        playerCharacterRepository.save(playerCharacter);
        return playerCharacterMapper.toOutputDTO(playerCharacter);
    }
}
