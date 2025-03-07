package org.kruskopf.backend.playercharacter;

import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.playercharacter.dto.PlayerCharacterInputDTO;
import org.kruskopf.backend.playercharacter.dto.PlayerCharacterOutputDTO;
import org.kruskopf.backend.playercharacter.entity.PlayerCharacter;
import org.kruskopf.backend.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class PlayerCharacterMapper {

    public PlayerCharacterOutputDTO toOutputDTO(PlayerCharacter playerCharacter) {
        return new PlayerCharacterOutputDTO(
                playerCharacter.getId(),
                playerCharacter.getOwner().getId(),
                playerCharacter.getCampaign() != null ? playerCharacter.getCampaign().getId() : null,
                playerCharacter.getName(),
                playerCharacter.getLevel(),
                playerCharacter.getCharacterClass(),
                playerCharacter.getRace(),
                playerCharacter.getCharacterData(),
                playerCharacter.getCreatedAt(),
                playerCharacter.getUpdatedAt()
        );
    }

    public PlayerCharacter toEntity(PlayerCharacterInputDTO dto, User owner, Campaign campaign) {
        return new PlayerCharacter(
                owner,
                campaign,
                dto.name(),
                dto.level(),
                dto.characterClass(),
                dto.race(),
                dto.playerCharacterData()
        );
    }
}

