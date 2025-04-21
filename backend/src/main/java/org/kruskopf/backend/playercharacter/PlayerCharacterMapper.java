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
                playerCharacter.getImageUrl(),
                playerCharacter.getCharacterData(),
                playerCharacter.getCreatedAt(),
                playerCharacter.getUpdatedAt()
        );
    }

    public PlayerCharacter toEntity(PlayerCharacterInputDTO dto, User owner, Campaign campaign) {
        PlayerCharacter character = new PlayerCharacter(
                owner,
                campaign,
                dto.name(),
                dto.level(),
                dto.characterClass(),
                dto.race(),
                dto.playerCharacterData()
        );

        if (dto.imageUrl() != null) {
            character.setImageUrl(dto.imageUrl());
        }

        return character;
    }

    public void patchEntity(PlayerCharacter entity, PlayerCharacterInputDTO dto) {
        if (dto.name() != null) entity.setName(dto.name());
        if (dto.characterClass() != null) entity.setCharacterClass(dto.characterClass());
        if (dto.level() != null) {
            entity.setLevel(dto.level());
        }
        if (dto.race() != null) entity.setRace(dto.race());
        if (dto.playerCharacterData() != null) entity.setCharacterData(dto.playerCharacterData());

        // Lägg till denna rad för att kunna uppdatera imageUrl
        if (dto.imageUrl() != null) entity.setImageUrl(dto.imageUrl());
    }

}

