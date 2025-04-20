package org.kruskopf.backend.playercharacter.dto;

import org.kruskopf.backend.playercharacter.PlayerCharacterStats;

public record PlayerCharacterInputDTO(

        Long ownerId,
        Long campaignId,
        String name,
        int level,
        String characterClass,
        String race,
        PlayerCharacterStats playerCharacterData) {
}
